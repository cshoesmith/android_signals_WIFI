package com.example.androidsignalswifi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.painterResource












import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.platform.LocalContext
import android.content.pm.PackageInfo
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var wifiSniffer: WifiSniffer

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            wifiSniffer.startScanning()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        VendorLookup.init(this) // Load MAC vendors async

        wifiSniffer = WifiSniffer(this)

        checkPermissionsAndStart()

        setContent {
            MaterialTheme {
                var showSplash by rememberSaveable { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(2000)
                    showSplash = false
                }

                if (showSplash) {
                    SplashScreen()
                } else {
                    MainScreen(wifiSniffer)
                }
            }
        }
    }

    private fun checkPermissionsAndStart() {
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (fineLocationPermission == PackageManager.PERMISSION_GRANTED) {
            wifiSniffer.startScanning()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "App Logo",
                modifier = Modifier.size(150.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Android Signals WIFI", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("by craftbeers.app", fontSize = 14.sp)
        }
    }
}

enum class SecurityFilter { ALL, OPEN, SECURED }
enum class TriangulationFilter { ALL, LEARNING, KNOWN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(wifiSniffer: WifiSniffer) {
    val aps by wifiSniffer.scannedAps.collectAsState()
    val isScanning by wifiSniffer.isScanning.collectAsState()
    val lastScan by wifiSniffer.lastScanTime.collectAsState()
    
    var secFilter by remember { mutableStateOf(SecurityFilter.ALL) }
    var triFilter by remember { mutableStateOf(TriangulationFilter.ALL) }
    
    var showSecurityMenu by remember { mutableStateOf(false) }
    var showTriangulationMenu by remember { mutableStateOf(false) }
    var centerTrigger by remember { mutableIntStateOf(0) }

    var showList by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val filteredAps = aps.filter { ap ->
        val passSec = when (secFilter) {
            SecurityFilter.ALL -> true
            SecurityFilter.OPEN -> !ap.isSecured
            SecurityFilter.SECURED -> ap.isSecured
        }
        val passTri = when (triFilter) {
            TriangulationFilter.ALL -> true
            TriangulationFilter.LEARNING -> ap.totalWeight <= 200.0
            TriangulationFilter.KNOWN -> ap.totalWeight > 200.0
        }
        passSec && passTri
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map behind everything
        MapScreen(aps = filteredAps, centerTrigger = centerTrigger, currentLocation = wifiSniffer.currentLocation)

        // Top Badges mapping the exact states we're in
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
                label = { Text("Security: ${secFilter.name}") },
                modifier = Modifier.padding(end = 8.dp)
            )
            InputChip(
                selected = true,
                onClick = { showTriangulationMenu = true },
                label = { Text("Triangulation: ${triFilter.name}") }
            )
        }

        // Middle Status Banner
        if (lastScan > 0L) {
            val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(lastScan))
            val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(lastScan))
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 80.dp) // Align perfectly with the ScanTimerClock box
                    .height(56.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Last Scan:", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    Text("$dateStr $timeStr", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Security Filter Button
            Box {
                FloatingActionButton(onClick = { showSecurityMenu = true }) {
                    Icon(Icons.Filled.Lock, contentDescription = "Security Filter")
                }
                DropdownMenu(
                    expanded = showSecurityMenu,
                    onDismissRequest = { showSecurityMenu = false }
                ) {
                    DropdownMenuItem(text = { Text("All") }, onClick = { secFilter = SecurityFilter.ALL; showSecurityMenu = false })
                    DropdownMenuItem(text = { Text("Open") }, onClick = { secFilter = SecurityFilter.OPEN; showSecurityMenu = false })
                    DropdownMenuItem(text = { Text("Secured") }, onClick = { secFilter = SecurityFilter.SECURED; showSecurityMenu = false })
                }
            }

            // Triangulation Filter Button
            Box {
                FloatingActionButton(onClick = { showTriangulationMenu = true }) {
                    Icon(Icons.Filled.Search, contentDescription = "Triangulation Filter")
                }
                DropdownMenu(
                    expanded = showTriangulationMenu,
                    onDismissRequest = { showTriangulationMenu = false }
                ) {
                    DropdownMenuItem(text = { Text("All") }, onClick = { triFilter = TriangulationFilter.ALL; showTriangulationMenu = false })
                    DropdownMenuItem(text = { Text("Learning (<=200)") }, onClick = { triFilter = TriangulationFilter.LEARNING; showTriangulationMenu = false })
                    DropdownMenuItem(text = { Text("Known (>200)") }, onClick = { triFilter = TriangulationFilter.KNOWN; showTriangulationMenu = false })
                }
            }

            // Play/Pause Button
            FloatingActionButton(onClick = { 
                if (isScanning) wifiSniffer.stopScanning() else wifiSniffer.startScanning() 
            }) {
                if (isScanning) {
                    Icon(painter = painterResource(id = android.R.drawable.ic_media_pause), contentDescription = "Pause", modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                }
            }

            // Center Map Button
            FloatingActionButton(onClick = { centerTrigger++ }) {
                Icon(Icons.Filled.LocationOn, contentDescription = "Center Map")
            }

            // List Button with Scan Indicator
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ScanTimerClock(lastScanTime = lastScan, isScanning = isScanning)
                }
                FloatingActionButton(onClick = { showList = true }) {
                    Icon(Icons.Filled.List, contentDescription = "List APs")
                }
            }
        }

        if (showInfoDialog) {
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
                Text("Version: " + versionName + "\nCreated by craftbeers.app\n\nLicense: MIT License")
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("Close") }
            }
        )
    }

    if (showList) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 120.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
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
                                    Text("RSSI: ${ap.rssi} dBm | Weight: ${ap.totalWeight.toInt()}")
                                    
                                    val secText = if (ap.securityType.isNotEmpty()) ap.securityType else if (ap.isSecured) "Secured" else "Open"
                                    Text("Security: $secText")
                                    
                                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScanTimerClock(lastScanTime: Long, isScanning: Boolean) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            while(true) {
                androidx.compose.runtime.withFrameMillis {
                    currentTime = System.currentTimeMillis()
                }
            }
        }
    }

    val elapsed = (currentTime - lastScanTime).coerceAtLeast(0L)
    val progress = (elapsed % 10000L) / 10000f

    androidx.compose.foundation.Canvas(modifier = Modifier.size(56.dp)) {
        val radius = size.minDimension / 2
        val centerObj = center

        // Transparent round background
        drawCircle(
            color = Color.Black.copy(alpha = 0.4f),
            radius = radius,
            center = centerObj
        )
        // Draw the rim
        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            radius = radius,
            center = centerObj,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )

        // Calculate angle (-90 starts at 12 o'clock)
        val angleRad = (progress * 360f - 90f) * (Math.PI / 180f).toFloat()

        // Clock hand line
        val handLength = radius * 0.8f
        val x = centerObj.x + handLength * kotlin.math.cos(angleRad.toDouble()).toFloat()
        val y = centerObj.y + handLength * kotlin.math.sin(angleRad.toDouble()).toFloat()

        // Trailing shadow
        drawArc(
            color = Color.Black.copy(alpha = 0.5f),
            startAngle = -90f,
            sweepAngle = progress * 360f,
            useCenter = true,
            topLeft = androidx.compose.ui.geometry.Offset(centerObj.x - radius, centerObj.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )

        drawLine(
            color = Color.White,
            start = centerObj,
            end = androidx.compose.ui.geometry.Offset(x, y),
            strokeWidth = 3.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        // Small center dot
        drawCircle(
            color = Color.White,
            radius = 3.dp.toPx()
        )
    }
}






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
