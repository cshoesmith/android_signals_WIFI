import re
with open("app/src/main/java/com/example/androidsignalswifi/MainActivity.kt", "r", encoding="utf-8") as f:
    content = f.read()

s_old = """                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Discovered APs (${filteredAps.size})", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showList = false }) { Text("Close") }
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(filteredAps.sortedByDescending { it.totalWeight }) { ap ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text("SSID: ${if(ap.ssid.isNotEmpty()) ap.ssid else "[Hidden]"}", fontWeight = FontWeight.Bold)
                            Text("BSSID: ${ap.bssid}  ->  Vendor: ${VendorLookup.getVendor(ap.bssid)}")
                            Text("Freq: ${ap.frequency} MHz | Standard: ${ap.wifiStandard}")
                            Text("RSSI: ${ap.rssi} dBm | Weight: ${ap.totalWeight.toInt()}")

                            val secText = if (ap.securityType.isNotEmpty()) ap.securityType else if (ap.isSecured) "Secured" else "Open"
                            Text("Security: $secText")
                            Text("Caps: ${ap.capabilities}", fontSize = 12.sp, color = Color.LightGray)

                            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }"""

s_new = """                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Discovered Devices (${filteredAps.size + filteredBles.size + filteredCells.size})", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showList = false }) { Text("Close") }
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(filteredAps.sortedByDescending { it.totalWeight }) { ap ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text("SSID: ${if(ap.ssid.isNotEmpty()) ap.ssid else "[Hidden]"}", fontWeight = FontWeight.Bold)
                            Text("BSSID: ${ap.bssid}  ->  Vendor: ${VendorLookup.getVendor(ap.bssid)}")
                            Text("Freq: ${ap.frequency} MHz | Standard: ${ap.wifiStandard}")
                            Text("RSSI: ${ap.rssi} dBm | Weight: ${ap.totalWeight.toInt()}")

                            val secText = if (ap.securityType.isNotEmpty()) ap.securityType else if (ap.isSecured) "Secured" else "Open"
                            Text("Security: $secText")
                            Text("Caps: ${ap.capabilities}", fontSize = 12.sp, color = Color.LightGray)

                            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                    items(filteredBles.sortedByDescending { it.lastRssi }) { ble ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text("BLE Name: ${if(ble.name.isNotEmpty()) ble.name else "[Unknown]"}", fontWeight = FontWeight.Bold)
                            Text("MAC: ${ble.macAddress}")
                            Text("RSSI: ${ble.lastRssi} dBm")
                            Text("Last Seen: ${ble.lastSeen}")
                            
                            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                    items(filteredCells.sortedByDescending { it.lastRssi }) { cell ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text("Cell Identity: ${cell.cellIdentity}", fontWeight = FontWeight.Bold)
                            Text("Operator: ${cell.operatorName}")
                            Text("RSSI: ${cell.lastRssi} dBm")
                            Text("Last Seen: ${cell.lastSeen}")
                            
                            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }"""

content = content.replace(s_old, s_new)

with open("app/src/main/java/com/example/androidsignalswifi/MainActivity.kt", "w", encoding="utf-8") as f:
    f.write(content)
