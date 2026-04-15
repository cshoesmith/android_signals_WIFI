package com.example.androidsignalswifi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Search
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
    private lateinit var bleSniffer: BleSniffer
    private lateinit var cellSniffer: CellSniffer

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            wifiSniffer.startScanning()
        }
        if (permissions[Manifest.permission.BLUETOOTH_SCAN] == true && permissions[Manifest.permission.BLUETOOTH_CONNECT] == true) {
            bleSniffer.startScanning()
        }
        if (permissions[Manifest.permission.READ_PHONE_STATE] == true) {
            cellSniffer.startScanning()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        VendorLookup.init(this) // Load MAC vendors async

        wifiSniffer = WifiSniffer(this)
        bleSniffer = BleSniffer(this)
        cellSniffer = CellSniffer(this)

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
                    MainScreen(wifiSniffer, bleSniffer, cellSniffer)
                }
            }
        }
    }

    private fun checkPermissionsAndStart() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        val allGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            wifiSniffer.startScanning()
            bleSniffer.startScanning()
            cellSniffer.startScanning()
        } else {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
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
enum class BandFilter { ALL, BAND_2_4, BAND_5_GHZ, BAND_6_GHZ }
enum class DeviceFilter { ALL, ROUTERS_ONLY, BLUETOOTH_ONLY, CELL_TOWERS_ONLY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(wifiSniffer: WifiSniffer, bleSniffer: BleSniffer, cellSniffer: CellSniffer) {
    val aps by wifiSniffer.scannedAps.collectAsState()
    val bles by bleSniffer.scannedBle.collectAsState()
    val cells by cellSniffer.scannedCells.collectAsState()
    
    val isScanning by wifiSniffer.isScanning.collectAsState()
    val lastScan by wifiSniffer.lastScanTime.collectAsState()
    
    var secFilter by remember { mutableStateOf(SecurityFilter.ALL) }
    var triFilter by remember { mutableStateOf(TriangulationFilter.ALL) }
    var bandFilter by remember { mutableStateOf(BandFilter.ALL) }
    var devFilter by remember { mutableStateOf(DeviceFilter.ALL) }

    var showSecurityMenu by remember { mutableStateOf(false) }
    var showTriangulationMenu by remember { mutableStateOf(false) }
    var showBandMenu by remember { mutableStateOf(false) }
    var showDevMenu by remember { mutableStateOf(false) }
    var centerTrigger by remember { mutableIntStateOf(0) }
    var zoomInTrigger by remember { mutableIntStateOf(0) }
    var zoomOutTrigger by remember { mutableIntStateOf(0) }

    var showList by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
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
        val passBand = when (bandFilter) {
            BandFilter.ALL -> true
            BandFilter.BAND_2_4 -> ap.frequency in 2400..2499
            BandFilter.BAND_5_GHZ -> ap.frequency in 5000..5999
            BandFilter.BAND_6_GHZ -> ap.frequency in 5925..7125
        }
        val passDev = when (devFilter) {
            DeviceFilter.ALL -> true
            DeviceFilter.ROUTERS_ONLY -> !ap.capabilities.contains("P2P") && !ap.capabilities.contains("WFD")
            DeviceFilter.BLUETOOTH_ONLY -> false
            DeviceFilter.CELL_TOWERS_ONLY -> false
        }
        passSec && passTri && passBand && passDev
    }

    val filteredBles = if (devFilter == DeviceFilter.ALL || devFilter == DeviceFilter.BLUETOOTH_ONLY) bles else emptyList()
    val filteredCells = if (devFilter == DeviceFilter.ALL || devFilter == DeviceFilter.CELL_TOWERS_ONLY) cells else emptyList()

    Box(modifier = Modifier.fillMaxSize()) {
        // Map behind everything
        MapScreen(
            aps = filteredAps,
            bles = filteredBles,
            cells = filteredCells,
            centerTrigger = centerTrigger,
            currentLocation = wifiSniffer.currentLocation,
            isScanning = isScanning,
            zoomInTrigger = zoomInTrigger,
            zoomOutTrigger = zoomOutTrigger
        )

        // Semi-transparent dark strip behind the status bar
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .windowInsetsTopHeight(WindowInsets.statusBars)
        )

        // Semi-transparent dark strip behind the navigation bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
        )

        // Top Info & Badges
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left Table / Column
            Column(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                when (devFilter) {
                    DeviceFilter.BLUETOOTH_ONLY -> {
                        Text("Bluetooth Devices", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val totalSeen = filteredBles.size
                        val totalIdentified = filteredBles.count { it.name != "Unknown" && it.name.isNotEmpty() }
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            Text("Seen:", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(76.dp))
                            Text("$totalSeen", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row {
                            Text("Identified:", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(76.dp))
                            Text("$totalIdentified", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    DeviceFilter.CELL_TOWERS_ONLY -> {
                        Text("Cell Towers", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val totalSeen = filteredCells.size
                        val totalTriangulated = filteredCells.count { it.totalWeight > 200.0 }
                        val uniqueOperators = filteredCells.map { it.owner }.distinct().size
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            Text("Seen:", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(76.dp))
                            Text("$totalSeen", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row {
                            Text("Triangulated:", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(76.dp))
                            Text("$totalTriangulated", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row {
                            Text("Operators:", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(76.dp))
                            Text("$uniqueOperators", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        Text("Access Points", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val totalSeen = aps.size
                        val totalTriangulated = filteredAps.count { it.totalWeight > 200.0 }
                        val totalOpen = filteredAps.count { !it.isSecured }
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            Text("Seen:", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(76.dp))
                            Text("$totalSeen", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row {
                            Text("Triangulated:", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(76.dp))
                            Text("$totalTriangulated", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row {
                            Text("Open:", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(76.dp))
                            Text("$totalOpen", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Right side: Info button only
            IconButton(
                onClick = { showInfoDialog = true }
            ) {
                Icon(Icons.Filled.Info, contentDescription = "Info", tint = Color.White)
            }
        }

        // Filters side tab - anchored to right edge
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(y = (-190).dp)
        ) {
            // "Filters" vertical tab label
            Box(
                modifier = Modifier
                    .clickable { showFilters = !showFilters }
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                    )
                    .padding(vertical = 16.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val label = if (showFilters) "\u25B6" else "Filters"
                    label.forEach { ch ->
                        Text(
                            text = ch.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 13.sp
                        )
                    }
                }
            }

            // Filter panel (slides in/out)
            androidx.compose.animation.AnimatedVisibility(
                visible = showFilters,
                enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }),
                exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it })
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Box {
                        InputChip(
                            selected = true,
                            onClick = { showSecurityMenu = true },
                            label = { Text("Security: ${secFilter.name}") },
                            modifier = Modifier.padding(bottom = 0.dp)
                        )
                        DropdownMenu(
                            expanded = showSecurityMenu,
                            onDismissRequest = { showSecurityMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text("All") }, onClick = { secFilter = SecurityFilter.ALL; showSecurityMenu = false })
                            DropdownMenuItem(text = { Text("Open") }, onClick = { secFilter = SecurityFilter.OPEN; showSecurityMenu = false })
                            DropdownMenuItem(text = { Text("Secured") }, onClick = { secFilter = SecurityFilter.SECURED; showSecurityMenu = false })
                        }
                    }
                    Box {
                        InputChip(
                            selected = true,
                            onClick = { showTriangulationMenu = true },
                            label = { Text("Triangulation: ${triFilter.name}") },
                            modifier = Modifier.padding(bottom = 0.dp)
                        )
                        DropdownMenu(
                            expanded = showTriangulationMenu,
                            onDismissRequest = { showTriangulationMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text("All") }, onClick = { triFilter = TriangulationFilter.ALL; showTriangulationMenu = false })
                            DropdownMenuItem(text = { Text("Learning (<=200)") }, onClick = { triFilter = TriangulationFilter.LEARNING; showTriangulationMenu = false })
                            DropdownMenuItem(text = { Text("Known (>200)") }, onClick = { triFilter = TriangulationFilter.KNOWN; showTriangulationMenu = false })
                        }
                    }
                    Box {
                        InputChip(
                            selected = true,
                            onClick = { showBandMenu = true },
                            label = { Text("Band: ${bandFilter.name}") },
                            modifier = Modifier.padding(bottom = 0.dp)
                        )
                        DropdownMenu(
                            expanded = showBandMenu,
                            onDismissRequest = { showBandMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text("All Bands") }, onClick = { bandFilter = BandFilter.ALL; showBandMenu = false })
                            DropdownMenuItem(text = { Text("2.4 GHz Only") }, onClick = { bandFilter = BandFilter.BAND_2_4; showBandMenu = false })
                            DropdownMenuItem(text = { Text("5 GHz Only") }, onClick = { bandFilter = BandFilter.BAND_5_GHZ; showBandMenu = false })
                            DropdownMenuItem(text = { Text("6 GHz Only") }, onClick = { bandFilter = BandFilter.BAND_6_GHZ; showBandMenu = false })
                        }
                    }
                    Box {
                        InputChip(
                            selected = true,
                            onClick = { showDevMenu = true },
                            label = { Text("Type: ${devFilter.name}") }
                        )
                        DropdownMenu(
                            expanded = showDevMenu,
                            onDismissRequest = { showDevMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text("All Devices") }, onClick = { devFilter = DeviceFilter.ALL; showDevMenu = false })
                            DropdownMenuItem(text = { Text("Exclude Smart TVs & Printers") }, onClick = { devFilter = DeviceFilter.ROUTERS_ONLY; showDevMenu = false })
                            DropdownMenuItem(text = { Text("Bluetooth Devices Only") }, onClick = { devFilter = DeviceFilter.BLUETOOTH_ONLY; showDevMenu = false })
                            DropdownMenuItem(text = { Text("Cell Towers Only") }, onClick = { devFilter = DeviceFilter.CELL_TOWERS_ONLY; showDevMenu = false })
                        }
                    }
                }
            }
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
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            // Play/Pause Button
            FloatingActionButton(
                onClick = { 
                    if (isScanning) wifiSniffer.stopScanning() else wifiSniffer.startScanning() 
                },
                modifier = Modifier.padding(end = 32.dp)
            ) {
                if (isScanning) {
                    Icon(painter = painterResource(id = android.R.drawable.ic_media_pause), contentDescription = "Pause", modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                }
            }

            // Center Map Button
            FloatingActionButton(
                onClick = { centerTrigger++ },
                modifier = Modifier.padding(end = 32.dp)
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = "Center Map")
            }

            // List Button
            FloatingActionButton(onClick = { showList = true }) {
                Icon(Icons.Filled.List, contentDescription = "List Devices")
            }
        }
        
        // Map Zoom Controls & Scan Timer Clock
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 80.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Zoom In Button
            FloatingActionButton(
                onClick = { zoomInTrigger++ },
                modifier = Modifier.size(40.dp),
                containerColor = Color.Black.copy(alpha = 0.6f),
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Zoom In")
            }
            
            // Zoom Out Button
            FloatingActionButton(
                onClick = { zoomOutTrigger++ },
                modifier = Modifier.size(40.dp),
                containerColor = Color.Black.copy(alpha = 0.6f),
                contentColor = Color.White
            ) {
                Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))
            }

            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                ScanTimerClock(lastScanTime = lastScan, isScanning = isScanning)
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
                            items(filteredBles.sortedByDescending { it.rssi }) { ble ->
                                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text("BLE Name: ${if(ble.name.isNotEmpty()) ble.name else "[Unknown]"}", fontWeight = FontWeight.Bold)
                                    Text("MAC: ${ble.mac}  ->  Vendor: ${VendorLookup.getVendor(ble.mac)}")
                                    Text("RSSI: ${ble.rssi} dBm")
                                    Text("Type: ${ble.deviceType}")
                                    
                                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                                }
                            }
                            items(filteredCells.sortedByDescending { it.rssi }) { cell ->
                                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text("Cell Identity: ${cell.cellId}", fontWeight = FontWeight.Bold)
                                    Text("Operator: ${cell.owner}")
                                    Text("RSSI: ${cell.rssi} dBm")
                                    Text("Network: ${cell.networkType}")
                                    
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
