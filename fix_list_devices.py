import re
with open("app/src/main/java/com/example/androidsignalswifi/MainActivity.kt", "r", encoding="utf-8") as f:
    content = f.read()

s_old = """                            Text("Discovered APs (${filteredAps.size})", fontSize = 20.sp, fontWeight = FontWeight.Bold)"""
s_new = """                            Text("Discovered Devices (${filteredAps.size + filteredBles.size + filteredCells.size})", fontSize = 20.sp, fontWeight = FontWeight.Bold)"""
content = content.replace(s_old, s_new)

s2_old = """                                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                                }
                            }
                        }
                    }"""
s2_new = """                                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
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
                        }
                    }"""
content = content.replace(s2_old, s2_new)

with open("app/src/main/java/com/example/androidsignalswifi/MainActivity.kt", "w", encoding="utf-8") as f:
    f.write(content)
