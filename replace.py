import sys

with open('app/src/main/java/com/example/androidsignalswifi/MainActivity.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('Text("Discovered APs ()", fontSize = 20.sp, fontWeight = FontWeight.Bold)', 
'''val numAps = filteredAps.size
                            val pctTri = if (numAps > 0) String.format(java.util.Locale.US, "%.1f", (filteredAps.count { it.triangulationResult != null }.toFloat() / numAps) * 100) else "0.0"
                            val pctOpen = if (numAps > 0) String.format(java.util.Locale.US, "%.1f", (filteredAps.count { !it.isSecured }.toFloat() / numAps) * 100) else "0.0"
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Discovered APs ()", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("Triangulated: % | Open: %", fontSize = 12.sp)
                            }''')

content = content.replace('TextButton(onClick = { showList = false }) { Text("Close") }', 
'''Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { showInfoDialog = true }) { Icon(Icons.Filled.Info, contentDescription = "Info") }
                                TextButton(onClick = { showList = false }) { Text("Close") }
                            }''')

content = content.replace('Text("SSID: ", fontWeight = FontWeight.Bold)', 
'Text("SSID: ", fontSize = 14.sp, fontWeight = FontWeight.Bold)')

content = content.replace('Text("BSSID:   ->  Vendor: ")', 
'''TooltipTextRow("Basic Service Set Identifier (MAC address of AP)", "BSSID:", ap.bssid)
                                    TooltipTextRow("Manufacturer of wireless hardware", "Vendor:", VendorLookup.getVendor(ap.bssid))''')

content = content.replace('Text("RSSI:  dBm | Weight: ")', 
'''TooltipTextRow("Received Signal Strength Indicator", "RSSI:", " dBm")
                                    TooltipTextRow("Calculated accuracy score based on frequency/rssi/duration", "Weight:", "")''')

content = content.replace('Text("Security: ")', 
'TooltipTextRow("Authentication type", "Security:", secText)')

with open('app/src/main/java/com/example/androidsignalswifi/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated.")
