import sys

with open('app/src/main/java/com/example/androidsignalswifi/MainActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

imp_base = '''import androidx.core.content.ContextCompat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.platform.LocalContext
import android.content.pm.PackageInfo'''
text = text.replace('import androidx.core.content.ContextCompat', imp_base)

text = text.replace('Text("Android Signals WIFI", fontSize = 24.sp, fontWeight = FontWeight.Bold)', 
'''Text("Android Signals WIFI", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("by craftbeers.app", fontSize = 14.sp)''')

text = text.replace('var showList by remember { mutableStateOf(false) }', 
'''var showList by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }''')

old_map_header = '''        // Top Badges mapping the exact states we're in
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            InputChip(
                selected = true,
                onClick = { showSecurityMenu = true },
                label = { Text("Security: ") },
                modifier = Modifier.padding(end = 8.dp)
            )
            InputChip(
                selected = true,
                onClick = { showTriangulationMenu = true },
                label = { Text("Triangulation: ") }
            )
        }'''
        
new_map_header = '''        // Top Badges mapping the exact states we're in
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { showInfoDialog = true },
                modifier = Modifier.background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f), androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(Icons.Filled.Info, contentDescription = "Info")
            }
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                InputChip(
                    selected = true,
                    onClick = { showSecurityMenu = true },
                    label = { Text("Security: ") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                InputChip(
                    selected = true,
                    onClick = { showTriangulationMenu = true },
                    label = { Text("Triangulation: ") }
                )
            }
        }'''
        
text = text.replace(old_map_header, new_map_header)

old_table_code = '''                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,   
                            verticalAlignment = Alignment.CenterVertically      
                        ) {
                            Text("Discovered APs ()", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { showList = false }) { Text("Close") }
                        }
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            items(filteredAps.sortedByDescending { it.totalWeight }) { ap ->
                                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text("SSID: ", fontWeight = FontWeight.Bold)
                                    Text("BSSID:   ->  Vendor: ")
                                    Text("RSSI:  dBm | Weight: ")

                                    val secText = if (ap.securityType.isNotEmpty()) ap.securityType else if (ap.isSecured) "Secured" else "Open"
                                    Text("Security: ")

                                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                                }
                            }
                        }'''
new_table_code = '''                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.SpaceBetween,   
                            verticalAlignment = Alignment.CenterVertically      
                        ) {
                            val numAps = filteredAps.size
                            val pctTri = if (numAps > 0) String.format(java.util.Locale.US, "%.1f", (filteredAps.count { it.triangulationResult != null }.toFloat() / numAps) * 100) else "0.0"
                            val pctOpen = if (numAps > 0) String.format(java.util.Locale.US, "%.1f", (filteredAps.count { !it.isSecured }.toFloat() / numAps) * 100) else "0.0"
                            Column(modifier = Modifier.padding(end=16.dp)) {
                                Text("Discovered APs ()", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("Triangulated: " + pctTri + "% | Open: " + pctOpen + "%", fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { showInfoDialog = true }) { Icon(Icons.Filled.Info, contentDescription = "Info") }
                                TextButton(onClick = { showList = false }) { Text("Close") }
                            }
                        }
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            items(filteredAps.sortedByDescending { it.totalWeight }) { ap ->
                                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text("SSID: ", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    TooltipTextRow("Basic Service Set Identifier (MAC address of AP)", "BSSID:", ap.bssid)
                                    TooltipTextRow("Manufacturer of wireless hardware", "Vendor:", VendorLookup.getVendor(ap.bssid))
                                    TooltipTextRow("Received Signal Strength Indicator", "RSSI:", " dBm")
                                    TooltipTextRow("Calculated accuracy score based on frequency/rssi/duration", "Weight:", "")

                                    val secText = if (ap.securityType.isNotEmpty()) ap.securityType else if (ap.isSecured) "Secured" else "Open"
                                    TooltipTextRow("Authentication type", "Security:", secText)

                                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                                }
                            }
                        }'''

text = text.replace(old_table_code, new_table_code)


dialog_code = '''    if (showInfoDialog) {
        val context = LocalContext.current
        var versionName = "Unknown"
        try {
            val pInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            versionName = pInfo.versionName ?: "Unknown"
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("About") },
            text = {
                Text("Version: " + versionName + "\\nCreated by craftbeers.app\\n\\nLicense: MIT License")
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("Close") }
            }
        )
    }

    if (showList) {'''

text = text.replace('    if (showList) {', dialog_code)

tooltip_def = '''
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipTextRow(tooltipText: String, labelText: String, valueText: String) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(tooltipText)
            }
        },
        state = rememberTooltipState()
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text(labelText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 4.dp))
            Text(valueText, fontSize = 12.sp)
        }
    }
}
'''

text += tooltip_def

with open('app/src/main/java/com/example/androidsignalswifi/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)
