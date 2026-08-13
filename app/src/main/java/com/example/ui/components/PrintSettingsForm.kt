package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintSettingsForm(
    settings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var paperTypeExpanded by remember { mutableStateOf(false) }
    var presetExpanded by remember { mutableStateOf(false) }

    val paperTypes = listOf("A4", "A5", "Letter")
    val presets = listOf(
        "30x20" to "30 × 20 مم",
        "40x25" to "40 × 25 مم",
        "50x30" to "50 × 30 مم",
        "60x40" to "60 × 40 مم",
        "70x50" to "70 × 50 مم",
        "100x50" to "100 × 50 مم",
        "CUSTOM" to "مقاس مخصص"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "إعدادات الورقة والتخطيط",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Paper Type & Label Preset Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                        label = { Text("نوع الورقة", fontSize = 10.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paperTypeExpanded) },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth().testTag("print_dialog_paper_type")
                    )
                    ExposedDropdownMenu(
                        expanded = paperTypeExpanded,
                        onDismissRequest = { paperTypeExpanded = false }
                    ) {
                        paperTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type, fontSize = 12.sp) },
                                onClick = {
                                    onSettingsChanged(settings.copy(paperType = type))
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
                        label = { Text("مقاس الملصق", fontSize = 10.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetExpanded) },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth().testTag("print_dialog_preset_size")
                    )
                    ExposedDropdownMenu(
                        expanded = presetExpanded,
                        onDismissRequest = { presetExpanded = false }
                    ) {
                        presets.forEach { (key, labelText) ->
                            DropdownMenuItem(
                                text = { Text(labelText, fontSize = 12.sp) },
                                onClick = {
                                    onSettingsChanged(settings.copy(presetSize = key))
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
                Text("عدد الأعمدة:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (settings.columnsCount > 1) {
                                onSettingsChanged(settings.copy(columnsCount = settings.columnsCount - 1))
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Minus", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "${settings.columnsCount}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    IconButton(
                        onClick = {
                            if (settings.columnsCount < 8) {
                                onSettingsChanged(settings.copy(columnsCount = settings.columnsCount + 1))
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Plus", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Margins & Gap Sliders
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("الهوامش: ${settings.marginTopMm.toInt()} مم", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        Slider(
                            value = settings.marginTopMm,
                            onValueChange = {
                                onSettingsChanged(
                                    settings.copy(
                                        marginTopMm = it,
                                        marginBottomMm = it,
                                        marginLeftMm = it,
                                        marginRightMm = it
                                    )
                                )
                            },
                            valueRange = 0f..30f
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("المسافة الفاصلة: ${settings.horizontalGapMm.toInt()} مم", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        Slider(
                            value = settings.horizontalGapMm,
                            onValueChange = {
                                onSettingsChanged(
                                    settings.copy(
                                        horizontalGapMm = it,
                                        verticalGapMm = it
                                    )
                                )
                            },
                            valueRange = 0f..15f
                        )
                    }
                }
            }
        }
    }
}
