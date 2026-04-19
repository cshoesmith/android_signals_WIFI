import re
with open("app/src/main/java/com/example/androidsignalswifi/MainActivity.kt", "r") as f:
    content = f.read()

s1_old = """                items(filteredCells.toList()) { cell ->
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
                }"""
s1_new = """                items(filteredCells.toList()) { cell ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Cell Identity: ${cell.cellIdentity}", fontWeight = FontWeight.Bold)
                            Text(text = "Last RSSI: ${cell.lastRssi}")
                        }
                    }
                }"""
content = content.replace(s1_old, s1_new)

with open("app/src/main/java/com/example/androidsignalswifi/MainActivity.kt", "w") as f:
    f.write(content)
