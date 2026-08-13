package com.example.ui.screens

import android.content.Intent
import androidx.core.content.FileProvider
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppSettings
import com.example.data.LabelItem
import com.example.ui.MainViewModel
import com.example.ui.components.LabelSheetPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintPreviewScreen(
    viewModel: MainViewModel,
    labels: List<LabelItem>,
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var paperTypeExpanded by remember { mutableStateOf(false) }
    var presetExpanded by remember { mutableStateOf(false) }

    val paperTypes = listOf("A4", "A5", "Letter")
    val presets = listOf(
        "30x20" to "30 × 20 mm",
        "40x25" to "40 × 25 mm",
        "50x30" to "50 × 30 mm",
        "60x40" to "60 × 40 mm",
        "70x50" to "70 × 50 mm",
        "100x50" to "100 × 50 mm",
        "CUSTOM" to "مقاس مخصص"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Page Sheet Layout & Preset Controls Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "إعدادات ورقة الطباعة وتخطيط الصفوف والأعمدة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Paper Type Selector
                    ExposedDropdownMenuBox(
                        expanded = paperTypeExpanded,
                        onExpandedChange = { paperTypeExpanded = !paperTypeExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = settings.paperType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("نوع الورق") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paperTypeExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth().testTag("paper_type_selector")
                        )
                        ExposedDropdownMenu(
                            expanded = paperTypeExpanded,
                            onDismissRequest = { paperTypeExpanded = false }
                        ) {
                            paperTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        viewModel.updateSettings(settings.copy(paperType = type))
                                        paperTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Label Dimensions Preset Selector
                    ExposedDropdownMenuBox(
                        expanded = presetExpanded,
                        onExpandedChange = { presetExpanded = !presetExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        val currentPresetLabel = presets.find { it.first == settings.presetSize }?.second ?: settings.presetSize
                        OutlinedTextField(
                            value = currentPresetLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("مقاس الملصق") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth().testTag("preset_size_selector")
                        )
                        ExposedDropdownMenu(
                            expanded = presetExpanded,
                            onDismissRequest = { presetExpanded = false }
                        ) {
                            presets.forEach { (key, labelText) ->
                                DropdownMenuItem(
                                    text = { Text(labelText) },
                                    onClick = {
                                        viewModel.updateSettings(settings.copy(presetSize = key))
                                        presetExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Columns Count Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("عدد الأعمدة في الصفحة:", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (settings.columnsCount > 1) {
                                    viewModel.updateSettings(settings.copy(columnsCount = settings.columnsCount - 1))
                                }
                            }
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease columns")
                        }
                        Text(
                            text = "${settings.columnsCount}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = {
                                if (settings.columnsCount < 8) {
                                    viewModel.updateSettings(settings.copy(columnsCount = settings.columnsCount + 1))
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase columns")
                        }
                    }
                }

                // Margins & Gap Sliders
                Text("هوامش الصفحة والمسافة بين الملصقات (mm):", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("الهامش العلوي/السفلي: ${settings.marginTopMm.toInt()} mm", fontSize = 11.sp)
                        Slider(
                            value = settings.marginTopMm,
                            onValueChange = { viewModel.updateSettings(settings.copy(marginTopMm = it, marginBottomMm = it)) },
                            valueRange = 0f..30f
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("المسافة بين الملصقات: ${settings.horizontalGapMm.toInt()} mm", fontSize = 11.sp)
                        Slider(
                            value = settings.horizontalGapMm,
                            onValueChange = { viewModel.updateSettings(settings.copy(horizontalGapMm = it, verticalGapMm = it)) },
                            valueRange = 0f..15f
                        )
                    }
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة / فتح ملف PDF"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).testTag("export_pdf_button")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("تصدير PDF", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    viewModel.exportPdfAndPrint(context, directPrint = true)
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).testTag("direct_print_button")
            ) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("طباعة مباشرة", fontWeight = FontWeight.Bold)
            }
        }

        // Live Printable Sheet Preview Container
        Text("معاينة شكل ورقة الطباعة (1:1):", fontWeight = FontWeight.Bold, fontSize = 14.sp)

        LabelSheetPreview(
            labels = labels,
            settings = settings,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
