import re
with open("app/src/main/java/com/example/androidsignalswifi/MainActivity.kt", "r") as f:
    content = f.read()

s1_old = """                items(filteredAps.toList()) { ap ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {"""
s1_new = """                items(filteredAps.toList()) { ap ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "SSID: ${ap.ssid}", fontWeight = FontWeight.Bold)
                            Text(text = "BSSID: ${ap.bssid}")
                            Text(text = "Best RSSI: ${ap.bestRssi}")
                            Text(text = "Avg RSSI: ${ap.averageRssi}")
                            Text(text = "Locations: ${ap.readings.size}")
                        }
                    }
                }
                items(filteredBles.toList()) { ble ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Name: ${ble.name}", fontWeight = FontWeight.Bold)
                            Text(text = "MAC: ${ble.macAddress}")
                            Text(text = "Last RSSI: ${ble.lastRssi}")
                        }
                    }
                }
                items(filteredCells.toList()) { cell ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {"""
content = content.replace(s1_old, s1_new)

with open("app/src/main/java/com/example/androidsignalswifi/MainActivity.kt", "w") as f:
    f.write(content)
