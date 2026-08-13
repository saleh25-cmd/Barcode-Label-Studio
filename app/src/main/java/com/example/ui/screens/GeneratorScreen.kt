package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.IconButton
import com.example.ui.components.BarcodeScannerDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppSettings
import com.example.data.LabelItem
import com.example.ui.MainViewModel
import com.example.ui.components.LabelCard

private fun generateRandom10DigitCode(): String {
    return (1000000000L..9999999999L).random().toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    viewModel: MainViewModel,
    settings: AppSettings,
    isGenerating: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Manual, 1 = Auto Sequential

    // Manual mode fields
    var manualCode by remember { mutableStateOf(generateRandom10DigitCode()) }
    var manualShopName by remember(settings.shopName) { mutableStateOf(settings.shopName) }
    var manualPrice by remember(settings.defaultPrice) { mutableStateOf(settings.defaultPrice) }
    var manualCopies by remember { mutableStateOf("1") }

    var showScannerDialog by remember { mutableStateOf(false) }

    if (showScannerDialog) {
        BarcodeScannerDialog(
            onDismissRequest = { showScannerDialog = false },
            onBarcodeScanned = { scanned ->
                manualCode = scanned
            }
        )
    }

    // Auto Sequential mode fields
    var startCode by remember { mutableStateOf(settings.lastUsedStartCode.toString()) }
    var count by remember { mutableStateOf("10") }
    var prefix by remember { mutableStateOf("") }
    var autoShopName by remember(settings.shopName) { mutableStateOf(settings.shopName) }
    var autoPrice by remember(settings.defaultPrice) { mutableStateOf(settings.defaultPrice) }
    var autoCopies by remember { mutableStateOf("1") }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab Row selector
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth().testTag("tab_row_generator")
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("إنشاء فردي / عشوائي", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("توليد تلقائي متسلسل", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) }
            )
        }

        // Live Preview Card
        val previewItem = remember(selectedTab, manualCode, manualShopName, manualPrice, startCode, prefix, autoShopName, autoPrice) {
            val codeToUse = if (selectedTab == 0) {
                if (manualCode.isBlank()) "1000000000" else manualCode
            } else {
                val num = startCode.ifBlank { "1000" }
                if (prefix.isNotBlank()) "$prefix$num" else num
            }
            val shopToUse = if (selectedTab == 0) manualShopName else autoShopName
            val priceToUse = if (selectedTab == 0) manualPrice else autoPrice
            LabelItem(
                code = codeToUse,
                shopName = if (shopToUse.isNotBlank()) shopToUse else settings.shopName,
                price = if (priceToUse.isNotBlank()) priceToUse else settings.defaultPrice,
                logoUri = settings.logoUri
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "معاينة الملصقة المباشرة",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LabelCard(
                    item = previewItem,
                    settings = settings,
                    showActions = false
                )
            }
        }

        // Progress bar for batch generation
        AnimatedVisibility(visible = isGenerating) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("جاري توليد الباركودات في الخلفية...", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${(progress * 100).toInt()}%", fontSize = 12.sp)
                }
            }
        }

        // Form fields
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedTab == 0) {
                    // MANUAL / RANDOM 10-DIGIT CREATION FORM
                    Text("بيانات الملصقة والتوليد العشوائي", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    // Product / Shop Name Field (above barcode)
                    OutlinedTextField(
                        value = manualShopName,
                        onValueChange = { manualShopName = it },
                        label = { Text("اسم المنتج (فوق الباركود)") },
                        placeholder = { Text("مثلاً: زيت زيتون 1لتر / اسم المحل") },
                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("manual_shop_name_input"),
                        singleLine = true
                    )

                    // Full width barcode text input field
                    OutlinedTextField(
                        value = manualCode,
                        onValueChange = { manualCode = it },
                        label = { Text("رقم الباركود (10 أرقام)") },
                        placeholder = { Text("مثلاً: 1000000000") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (manualCode.isNotEmpty()) {
                                IconButton(onClick = { manualCode = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "مسح"
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_code_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // Helper Action Buttons for Barcode Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { manualCode = generateRandom10DigitCode() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("generate_random_10digit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("توليد عشوائي", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = { showScannerDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("scan_barcode_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مسح بالكاميرا", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // Price & Copies row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualPrice,
                            onValueChange = { manualPrice = it },
                            label = { Text("السعر") },
                            placeholder = { Text("1200 DA") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("manual_price_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = manualCopies,
                            onValueChange = { manualCopies = it },
                            label = { Text("عدد النسخ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.width(100.dp).testTag("manual_copies_input"),
                            singleLine = true
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.addSingleLabel(
                                code = manualCode,
                                price = manualPrice,
                                copies = manualCopies.toIntOrNull() ?: 1,
                                shopName = manualShopName
                            )
                            // Auto generate next random 10 digit code for quick consecutive entry
                            manualCode = generateRandom10DigitCode()
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_manual_button"),
                        enabled = manualCode.isNotBlank() && !isGenerating
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إضافة الملصقة", fontWeight = FontWeight.Bold)
                    }

                } else {
                    // AUTO SEQUENTIAL BATCH FORM
                    Text("إعدادات التوليد التلقائي لآلاف الملصقات", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    OutlinedTextField(
                        value = autoShopName,
                        onValueChange = { autoShopName = it },
                        label = { Text("اسم المنتج / المحل (فوق الباركود)") },
                        placeholder = { Text("اسم المنتج أو المحل للملصقات") },
                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("auto_shop_name_input"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startCode,
                            onValueChange = { startCode = it },
                            label = { Text("أول رقم متسلسل") },
                            placeholder = { Text("1000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("auto_start_code_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = count,
                            onValueChange = { count = it },
                            label = { Text("العدد الإجمالي") },
                            placeholder = { Text("250") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("auto_count_input"),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = prefix,
                            onValueChange = { prefix = it },
                            label = { Text("بادئة الكود (اختياري)") },
                            placeholder = { Text("PR أو ITEM-") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("auto_prefix_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = autoCopies,
                            onValueChange = { autoCopies = it },
                            label = { Text("النسخ/ملصقة") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.width(100.dp).testTag("auto_copies_input"),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = autoPrice,
                        onValueChange = { autoPrice = it },
                        label = { Text("السعر لجميع الملصقات") },
                        placeholder = { Text("1200 DA") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("auto_price_input"),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            viewModel.generateBatchSequential(
                                startCodeStr = startCode,
                                countStr = count,
                                prefix = prefix,
                                priceStr = autoPrice,
                                copiesPerCode = autoCopies.toIntOrNull() ?: 1,
                                shopNameStr = autoShopName
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("generate_batch_button"),
                        enabled = !isGenerating && startCode.isNotBlank() && count.isNotBlank()
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("توليد الملصقات الآن", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
