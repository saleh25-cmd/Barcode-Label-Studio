package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.AppSettings
import com.example.data.LabelItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

object DataExporter {

    fun exportToCsv(labels: List<LabelItem>): String {
        val sb = StringBuilder()
        sb.append("Code,ShopName,Price,Copies\n")
        for (item in labels) {
            val code = escapeCsv(item.code)
            val shop = escapeCsv(item.shopName)
            val price = escapeCsv(item.price)
            val copies = item.copies
            sb.append("$code,$shop,$price,$copies\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(str: String): String {
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            return "\"" + str.replace("\"", "\"\"") + "\""
        }
        return str
    }

    fun parseCsv(csvText: String): List<LabelItem> {
        val list = mutableListOf<LabelItem>()
        val lines = csvText.lines()
        if (lines.size <= 1) return list

        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            val tokens = parseCsvLine(line)
            if (tokens.isNotEmpty()) {
                val code = tokens.getOrElse(0) { "" }
                val shop = tokens.getOrElse(1) { "" }
                val price = tokens.getOrElse(2) { "" }
                val copies = tokens.getOrElse(3) { "1" }.toIntOrNull() ?: 1
                if (code.isNotBlank()) {
                    list.add(LabelItem(code = code, shopName = shop, price = price, copies = copies))
                }
            }
        }
        return list
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (c in line) {
            if (c == '"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(c)
            }
        }
        result.add(sb.toString().trim())
        return result
    }

    fun exportToJson(labels: List<LabelItem>, settings: AppSettings): String {
        val root = JSONObject()
        val labelsArr = JSONArray()
        for (item in labels) {
            val obj = JSONObject().apply {
                put("code", item.code)
                put("shopName", item.shopName)
                put("price", item.price)
                put("copies", item.copies)
            }
            labelsArr.put(obj)
        }
        root.put("labels", labelsArr)

        val settingsObj = JSONObject().apply {
            put("shopName", settings.shopName)
            put("presetSize", settings.presetSize)
            put("customWidthMm", settings.customWidthMm.toDouble())
            put("customHeightMm", settings.customHeightMm.toDouble())
            put("paperType", settings.paperType)
            put("columnsCount", settings.columnsCount)
            put("defaultPrice", settings.defaultPrice)
        }
        root.put("settings", settingsObj)

        return root.toString(2)
    }

    fun parseJsonLabels(jsonText: String): List<LabelItem> {
        val list = mutableListOf<LabelItem>()
        try {
            val root = JSONObject(jsonText)
            val labelsArr = root.optJSONArray("labels") ?: return list
            for (i in 0 until labelsArr.length()) {
                val obj = labelsArr.getJSONObject(i)
                val code = obj.optString("code", "")
                val shop = obj.optString("shopName", "")
                val price = obj.optString("price", "")
                val copies = obj.optInt("copies", 1)
                if (code.isNotBlank()) {
                    list.add(LabelItem(code = code, shopName = shop, price = price, copies = copies))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun readTextFromUri(context: Context, uri: Uri): String {
        val sb = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    sb.append(line).append("\n")
                    line = reader.readLine()
                }
            }
        }
        return sb.toString()
    }
}
