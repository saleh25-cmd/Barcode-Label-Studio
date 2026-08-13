package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppSettings
import com.example.data.LabelItem
import com.example.data.LabelRepository
import com.example.util.DataExporter
import com.example.util.PdfExportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LabelRepository

    val searchQuery = MutableStateFlow("")

    val settings: StateFlow<AppSettings>

    val labels: StateFlow<List<LabelItem>>

    val isGeneratingBatch = MutableStateFlow(false)
    val batchProgress = MutableStateFlow(0f)

    val messageEvent = MutableStateFlow<String?>(null)

    init {
        val db = AppDatabase.getDatabase(application)
        repository = LabelRepository(db.labelDao(), db.settingsDao())

        settings = repository.settingsFlow
            .flatMapLatest { settings ->
                if (settings == null) {
                    val defaultSettings = AppSettings()
                    viewModelScope.launch { repository.saveSettings(defaultSettings) }
                    MutableStateFlow(defaultSettings)
                } else {
                    MutableStateFlow(settings)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = AppSettings()
            )

        labels = combine(repository.allLabels, searchQuery) { list, query ->
            if (query.isBlank()) {
                list
            } else {
                list.filter {
                    it.code.contains(query, ignoreCase = true) ||
                    it.price.contains(query, ignoreCase = true) ||
                    it.shopName.contains(query, ignoreCase = true)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    /**
     * Add single label manually.
     */
    fun addSingleLabel(code: String, price: String, copies: Int = 1, shopName: String = "") {
        if (code.isBlank()) return
        viewModelScope.launch {
            val currentSettings = repository.getSettings()
            val finalShopName = if (shopName.isNotBlank()) shopName.trim() else currentSettings.shopName
            val label = LabelItem(
                code = code.trim(),
                shopName = finalShopName,
                price = if (price.isNotBlank()) price.trim() else currentSettings.defaultPrice,
                logoUri = currentSettings.logoUri,
                copies = copies.coerceAtLeast(1)
            )
            repository.insertLabel(label)
            messageEvent.value = "تمت إضافة الملصق بنجاح"
        }
    }

    /**
     * Sequential Auto Batch Generation.
     * Supports generating up to thousands of barcodes efficiently in background coroutines.
     */
    fun generateBatchSequential(
        startCodeStr: String,
        countStr: String,
        prefix: String = "",
        priceStr: String = "",
        copiesPerCode: Int = 1,
        shopNameStr: String = ""
    ) {
        val startVal = startCodeStr.toLongOrNull() ?: 1000L
        val countVal = countStr.toIntOrNull()?.coerceIn(1, 50000) ?: 10

        viewModelScope.launch {
            isGeneratingBatch.value = true
            batchProgress.value = 0f

            withContext(Dispatchers.IO) {
                val currentSettings = repository.getSettings()
                val price = if (priceStr.isNotBlank()) priceStr.trim() else currentSettings.defaultPrice
                val shopName = if (shopNameStr.isNotBlank()) shopNameStr.trim() else currentSettings.shopName
                val logoUri = currentSettings.logoUri

                val batchList = ArrayList<LabelItem>(countVal)
                val chunkSize = 1000

                for (i in 0 until countVal) {
                    val currentNum = startVal + i
                    val codeText = if (prefix.isNotBlank()) "$prefix$currentNum" else "$currentNum"

                    batchList.add(
                        LabelItem(
                            code = codeText,
                            shopName = shopName,
                            price = price,
                            logoUri = logoUri,
                            copies = copiesPerCode.coerceAtLeast(1)
                        )
                    )

                    if (batchList.size >= chunkSize || i == countVal - 1) {
                        repository.insertBatchLabels(batchList)
                        batchList.clear()
                        batchProgress.value = (i + 1).toFloat() / countVal.toFloat()
                    }
                }

                // Update last used code in settings
                repository.saveSettings(currentSettings.copy(lastUsedStartCode = startVal + countVal))
            }

            isGeneratingBatch.value = false
            messageEvent.value = "تم توليد $countVal ملصق بنجاح"
        }
    }

    fun updateLabel(label: LabelItem) {
        viewModelScope.launch {
            repository.updateLabel(label)
            messageEvent.value = "تم تحديث الملصق"
        }
    }

    fun deleteLabel(label: LabelItem) {
        viewModelScope.launch {
            repository.deleteLabel(label)
            messageEvent.value = "تم حذف الملصق"
        }
    }

    fun clearAllLabels() {
        viewModelScope.launch {
            repository.deleteAllLabels()
            messageEvent.value = "تم مسح جميع الملصقات"
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            repository.saveSettings(newSettings)
            messageEvent.value = "تم حفظ الإعدادات"
        }
    }

    fun setLogoUri(uri: Uri?) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(logoUri = uri?.toString()))
            messageEvent.value = if (uri != null) "تم حفظ اللوجو بنجاح" else "تم إزالة اللوجو"
        }
    }

    fun importCsv(context: android.content.Context, uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val text = DataExporter.readTextFromUri(context, uri)
                    val items = DataExporter.parseCsv(text)
                    if (items.isNotEmpty()) {
                        val currentSettings = repository.getSettings()
                        val filledItems = items.map {
                            it.copy(
                                shopName = if (it.shopName.isBlank()) currentSettings.shopName else it.shopName,
                                price = if (it.price.isBlank()) currentSettings.defaultPrice else it.price,
                                logoUri = currentSettings.logoUri
                            )
                        }
                        repository.insertBatchLabels(filledItems)
                        withContext(Dispatchers.Main) {
                            messageEvent.value = "تم استيراد ${items.size} ملصق من ملف CSV"
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun importJson(context: android.content.Context, uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val text = DataExporter.readTextFromUri(context, uri)
                    val items = DataExporter.parseJsonLabels(text)
                    if (items.isNotEmpty()) {
                        repository.insertBatchLabels(items)
                        withContext(Dispatchers.Main) {
                            messageEvent.value = "تم استيراد ${items.size} ملصق من ملف JSON"
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun exportPdfAndPrint(
        context: android.content.Context,
        targetLabels: List<LabelItem>? = null,
        directPrint: Boolean = false,
        onFileExported: ((File) -> Unit)? = null
    ) {
        val currentLabels = targetLabels ?: labels.value
        if (currentLabels.isEmpty()) {
            messageEvent.value = "لا توجد ملصقات للتصدير"
            return
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val currentSettings = repository.getSettings()
                val pdfFile = File(context.cacheDir, "Barcode_Labels_${System.currentTimeMillis()}.pdf")
                val success = PdfExportManager.generatePdfFile(context, currentLabels, currentSettings, pdfFile)

                withContext(Dispatchers.Main) {
                    if (success) {
                        if (directPrint) {
                            PdfExportManager.printDocument(context, pdfFile)
                        } else {
                            onFileExported?.invoke(pdfFile)
                            messageEvent.value = "تم تصدير ملف PDF بنجاح"
                        }
                    } else {
                        messageEvent.value = "فشل إنشاء ملف PDF"
                    }
                }
            }
        }
    }

    fun clearMessage() {
        messageEvent.value = null
    }
}
