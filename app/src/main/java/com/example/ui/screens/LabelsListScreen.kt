package com.example.ui.screens

import android.content.Intent
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.LabelSheetPreview
import com.example.ui.components.PrintSettingsForm
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppSettings
import com.example.data.LabelItem
import com.example.ui.MainViewModel
import com.example.ui.components.LabelCard

@Composable
fun LabelsListScreen(
    viewModel: MainViewModel,
    labels: List<LabelItem>,
    settings: AppSettings,
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var editingLabel by remember { mutableStateOf<LabelItem?>(null) }
    var singlePrintLabel by remember { mutableStateOf<LabelItem?>(null) }
    var showBatchPrintDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showScannerDialog by remember { mutableStateOf(false) }

    if (showScannerDialog) {
        BarcodeScannerDialog(
            onDismissRequest = { showScannerDialog = false },
            onBarcodeScanned = { scanned ->
                viewModel.searchQuery.value = scanned
                val exists = labels.any { it.code == scanned }
                if (!exists) {
                    viewModel.addSingleLabel(scanned, settings.defaultPrice, 1)
                }
            }
        )
    }

    // File pickers for CSV and JSON
    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importCsv(context, uri)
        }
    }

    val jsonPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importJson(context, uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search bar with camera scan
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_input"),
                placeholder = { Text("بحث عن باركود...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            IconButton(
                onClick = { showScannerDialog = true },
                modifier = Modifier
                    .testTag("scan_list_camera_button")
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "مسح بالكميرا",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Summary bar & Import / Clear buttons
        val totalCopies = remember(labels) { labels.sumOf { it.copies } }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "عدد الأنواع: ${labels.size}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "إجمالي الملصقات للطباعة: $totalCopies",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (labels.isNotEmpty()) {
                        Button(
                            onClick = { showBatchPrintDialog = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("print_all_labels_button")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("طباعة PDF للكل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(
                        onClick = { csvPickerLauncher.launch("*/*") },
                        modifier = Modifier.testTag("import_csv_button")
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = "استيراد CSV", tint = MaterialTheme.colorScheme.primary)
                    }

                    if (labels.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.testTag("clear_all_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف الكل", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Labels Grid
        if (labels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "لم يتم العثور على نتائج للبحث" else "لا توجد ملصقات مضافة بعد\nاستخدم التوليد لإضافة ملصقات جديدة",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 180.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(labels, key = { it.id }) { label ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LabelCard(
                            item = label,
                            settings = settings,
                            cardWidth = 180.dp,
                            cardHeight = 120.dp,
                            onEdit = { editingLabel = label },
                            onDelete = { viewModel.deleteLabel(label) }
                        )

                        // Copies adjustment & Single Label Print row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, start = 2.dp, end = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (label.copies > 1) {
                                            viewModel.updateLabel(label.copy(copies = label.copies - 1))
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Minus", tint = MaterialTheme.colorScheme.primary)
                                }

                                Text(
                                    text = "${label.copies}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )

                                IconButton(
                                    onClick = {
                                        viewModel.updateLabel(label.copy(copies = label.copies + 1))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Plus", tint = MaterialTheme.colorScheme.primary)
                                }
                            }

                            // Single label print PDF button
                            OutlinedButton(
                                onClick = { singlePrintLabel = label },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("print_single_label_${label.id}")
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("طبع PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Single Label Print PDF Dialog
    singlePrintLabel?.let { target ->
        var singleCopies by remember { mutableStateOf(target.copies.toString()) }
        val dialogScrollState = rememberScrollState()

        AlertDialog(
            onDismissRequest = { singlePrintLabel = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إعدادات ومعاينة طباعة الملصق", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(dialogScrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "الباركود: ${target.code}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (target.shopName.isNotBlank()) {
                                Text("اسم المنتج / المحل: ${target.shopName}", fontSize = 11.sp)
                            }
                            if (target.price.isNotBlank()) {
                                Text("السعر: ${target.price}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = singleCopies,
                        onValueChange = { singleCopies = it },
                        label = { Text("عدد النسخ للملصق", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("single_print_copies_input")
                    )

                    // Compact Print Settings Form
                    PrintSettingsForm(
                        settings = settings,
                        onSettingsChanged = { updated ->
                            viewModel.updateSettings(updated)
                        }
                    )

                    // Live Page Sheet Preview
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "معاينة صفحة الطباعة قبل التصدير:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val previewCopies = singleCopies.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        LabelSheetPreview(
                            labels = listOf(target.copy(copies = previewCopies)),
                            settings = settings,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = {
                            val copiesCount = singleCopies.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            val customItem = target.copy(copies = copiesCount)
                            viewModel.exportPdfAndPrint(context, targetLabels = listOf(customItem), directPrint = false) { pdfFile ->
                                try {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة / فتح ملف PDF للملصق"))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            singlePrintLabel = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("single_export_pdf_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تصدير PDF وحفظ/مشاركة", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val copiesCount = singleCopies.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            val customItem = target.copy(copies = copiesCount)
                            viewModel.exportPdfAndPrint(context, targetLabels = listOf(customItem), directPrint = true)
                            singlePrintLabel = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("single_direct_print_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("طباعة مباشرة للملصق", fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { singlePrintLabel = null }) {
                    Text("إلغاء", fontSize = 12.sp)
                }
            }
        )
    }

    // Batch Print PDF Dialog
    if (showBatchPrintDialog) {
        val dialogScrollState = rememberScrollState()

        AlertDialog(
            onDismissRequest = { showBatchPrintDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("طباعة ومعاينة جميع الملصقات (PDF)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(dialogScrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "عدد الأنواع: ${labels.size} | إجمالي عدد الملصقات: ${labels.sumOf { it.copies }}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    // Compact Print Settings Form
                    PrintSettingsForm(
                        settings = settings,
                        onSettingsChanged = { updated ->
                            viewModel.updateSettings(updated)
                        }
                    )

                    // Live Page Sheet Preview for Batch Labels
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "معاينة صفحة الطباعة لجميع الملصقات:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LabelSheetPreview(
                            labels = labels,
                            settings = settings,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = {
                            viewModel.exportPdfAndPrint(context, directPrint = false) { pdfFile ->
                                try {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة / فتح ملف PDF للملصقات"))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            showBatchPrintDialog = false
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("batch_export_pdf_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تصدير PDF لجميع الملصقات", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.exportPdfAndPrint(context, directPrint = true)
                            showBatchPrintDialog = false
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("batch_direct_print_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("طباعة مباشرة للكل", fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchPrintDialog = false }) {
                    Text("إلغاء", fontSize = 12.sp)
                }
            }
        )
    }

    // Edit Label Dialog
    editingLabel?.let { target ->
        var editCode by remember { mutableStateOf(target.code) }
        var editShopName by remember { mutableStateOf(target.shopName) }
        var editPrice by remember { mutableStateOf(target.price) }
        var editCopies by remember { mutableStateOf(target.copies.toString()) }

        AlertDialog(
            onDismissRequest = { editingLabel = null },
            title = { Text("تعديل بيانات الملصقة", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editCode,
                        onValueChange = { editCode = it },
                        label = { Text("كود الباركود") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editShopName,
                        onValueChange = { editShopName = it },
                        label = { Text("اسم المنتج / المحل (فوق الباركود)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editPrice,
                        onValueChange = { editPrice = it },
                        label = { Text("السعر") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editCopies,
                        onValueChange = { editCopies = it },
                        label = { Text("عدد النسخ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateLabel(
                            target.copy(
                                code = editCode,
                                shopName = editShopName,
                                price = editPrice,
                                copies = editCopies.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            )
                        )
                        editingLabel = null
                    }
                ) {
                    Text("حفظ التغييرات")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingLabel = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Confirm Clear All Dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("مسح جميع الملصقات") },
            text = { Text("هل أنت تأكد من مسح كافة الملصقات المفعلة؟ لا يمكن التراجع عن هذه العملية.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllLabels()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، مسح الكل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
