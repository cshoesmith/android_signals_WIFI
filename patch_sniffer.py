import re

with open('app/src/main/java/com/example/androidsignalswifi/CellSniffer.kt', 'r', encoding='utf-8') as f:
    text = f.read()

# Normalize line endings
text = text.replace('\r\n', '\n')

replacement = \"\"\"
                var cellId = ""
                var networkType = ""
                var mccMnc = ""
                var lac = ""
                var owner = ""
                var pci = ""
                var band = ""
                var rssi = 0

                when (cell) {
                    is CellInfoLte -> {
                        val identity = cell.cellIdentity
                        cellId = ""
                        networkType = "LTE"
                        mccMnc = ""
                        lac = ""
                        pci = ""
                        band = "EARFCN "
                        rssi = cell.cellSignalStrength.dbm
                        owner = resolveOwner(mccMnc, identity.operatorAlphaLong?.toString())
                    }
                    is CellInfoGsm -> {
                        val identity = cell.cellIdentity
                        cellId = ""
                        networkType = "GSM"
                        mccMnc = ""
                        lac = ""
                        pci = "BSIC "
                        band = "ARFCN "
                        rssi = cell.cellSignalStrength.dbm
                        owner = resolveOwner(mccMnc, identity.operatorAlphaLong?.toString())
                    }
                    is CellInfoWcdma -> {
                        val identity = cell.cellIdentity
                        cellId = ""
                        networkType = "WCDMA"
                        mccMnc = ""
                        lac = ""
                        pci = "PSC "
                        band = "UARFCN "
                        rssi = cell.cellSignalStrength.dbm
                        owner = resolveOwner(mccMnc, identity.operatorAlphaLong?.toString())
                    }
                    is CellInfoNr -> {
                        val identity = cell.cellIdentity as CellIdentityNr
                        cellId = ""
                        networkType = "NR (5G)"
                        mccMnc = ""
                        lac = ""
                        pci = ""
                        band = "NRARFCN "
                        rssi = cell.cellSignalStrength.dbm
                        owner = resolveOwner(mccMnc, identity.operatorAlphaLong?.toString())
                    }
                }
\"\"\"
match = re.search(r'                var cellId = "".*?                }', text, re.DOTALL)
if match:
    text = text[:match.start()] + replacement.strip() + text[match.end():]
else:
    print("Failed to replace when block")

db_save_replacement = \"\"\"                val values = ContentValues().apply {
                    put(DatabaseHelper.COL_CELL_ID, cellId)
                    put(DatabaseHelper.COL_CELL_NETWORK, networkType)
                    put(DatabaseHelper.COL_CELL_MCC_MNC, mccMnc)
                    put(DatabaseHelper.COL_CELL_LAC, lac)
                    put(DatabaseHelper.COL_CELL_OWNER, owner)
                    put(DatabaseHelper.COL_CELL_PCI, pci)
                    put(DatabaseHelper.COL_CELL_BAND, band)
                    put(DatabaseHelper.COL_EST_LAT, newEstLat)
                    put(DatabaseHelper.COL_EST_LON, newEstLon)
                    put(DatabaseHelper.COL_TOTAL_WEIGHT, newTotalWeight)
                    put(DatabaseHelper.COL_LAST_RSSI, rssi)
                    put(DatabaseHelper.COL_LAST_SEEN, System.currentTimeMillis())
                }\"\"\"
match_db = re.search(r'                val values = ContentValues\(\)\.apply \{.*?\}', text, re.DOTALL)
if match_db:
    text = text[:match_db.start()] + db_save_replacement + text[match_db.end():]
else:
    print("Failed to replace DB insert block")

owner_function = \"\"\"
    private fun resolveOwner(mccMnc: String?, operatorAlpha: String?): String {
        if (!operatorAlpha.isNullOrEmpty()) return operatorAlpha
        return when (mccMnc) {
            "50501" -> "Telstra"
            "50502" -> "Optus"
            "50503" -> "Vodafone"
            "50506" -> "Vodafone"
            "50511" -> "TPG"
            "50571" -> "Telstra"
            "50572" -> "Telstra"
            "50590" -> "Optus"
            "" -> "Unknown"
            else -> mccMnc ?: "Unknown"
        }
    }
}
\"\"\"
if "resolveOwner" not in text:
    text = text[:text.rfind('}')] + owner_function.strip() + "\n"

old_cursor = \"\"\"                            DatabaseHelper.COL_CELL_NETWORK, DatabaseHelper.COL_CELL_MCC_MNC,
                            DatabaseHelper.COL_EST_LAT, DatabaseHelper.COL_EST_LON, DatabaseHelper.COL_TOTAL_WEIGHT,
                            DatabaseHelper.COL_LAST_RSSI, DatabaseHelper.COL_CELL_LAC
                        )\"\"\"
new_cursor = \"\"\"                            DatabaseHelper.COL_CELL_NETWORK, DatabaseHelper.COL_CELL_MCC_MNC,
                            DatabaseHelper.COL_EST_LAT, DatabaseHelper.COL_EST_LON, DatabaseHelper.COL_TOTAL_WEIGHT,
                            DatabaseHelper.COL_LAST_RSSI, DatabaseHelper.COL_CELL_LAC,
                            DatabaseHelper.COL_CELL_OWNER, DatabaseHelper.COL_CELL_PCI, DatabaseHelper.COL_CELL_BAND
                        )\"\"\"
if old_cursor in text:
    text = text.replace(old_cursor, new_cursor)
else:
    print("Failed to replace cursor query")

old_scan = \"\"\"                            ScannedCell(
                                cellId = cursor.getString(0),
                                networkType = cursor.getString(1),
                                mccMnc = cursor.getString(2),
                                estLat = cursor.getDouble(3),
                                estLon = cursor.getDouble(4),
                                totalWeight = cursor.getDouble(5),
                                rssi = cursor.getInt(6),
                                lac = cursor.getString(7)
                            )\"\"\"
new_scan = \"\"\"                            ScannedCell(
                                cellId = cursor.getString(0),
                                networkType = cursor.getString(1),
                                mccMnc = cursor.getString(2),
                                estLat = cursor.getDouble(3),
                                estLon = cursor.getDouble(4),
                                totalWeight = cursor.getDouble(5),
                                rssi = cursor.getInt(6),
                                lac = cursor.getString(7),
                                owner = cursor.getString(8),
                                pci = cursor.getString(9),
                                band = cursor.getString(10)
                            )\"\"\"
if old_scan in text:
    text = text.replace(old_scan, new_scan)
else:
    print("Failed to replace ScannedCell load")


with open('app/src/main/java/com/example/androidsignalswifi/CellSniffer.kt', 'w', encoding='utf-8') as f:
    f.write(text)
