package com.example.androidsignalswifi

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

object VendorLookup {
    private val vendorMap = mutableMapOf<String, String>()
    var isLoaded = false

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (char in line) {
            if (char == '"') {
                inQuotes = !inQuotes
            } else if (char == ',' && !inQuotes) {
                result.add(current.toString().trim())
                current.clear()
            } else {
                current.append(char)
            }
        }
        result.add(current.toString().trim())
        return result
    }

    fun init(context: Context) {
        if (isLoaded) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                context.assets.open("oui.csv").use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        // Skip header
                        reader.readLine()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val parts = parseCsvLine(line!!)
                            if (parts.size >= 3) {
                                val prefix = parts[1].trim().uppercase()
                                val name = parts[2].trim()
                                if (prefix.length == 6 && name.isNotEmpty()) {
                                    vendorMap[prefix] = name
                                }
                            }
                        }
                    }
                }
                isLoaded = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getVendor(bssid: String): String {
        val cleanPrefix = bssid.replace(":", "").take(6).uppercase()
        return vendorMap[cleanPrefix] ?: "Unknown"
    }
}
