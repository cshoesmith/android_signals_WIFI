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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState












import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.platform.LocalContext
import android.content.pm.PackageInfo
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
enum class BleInterestFilter { ALL, INTERESTING }

private val COMMON_BLE_TYPES = setOf(
    "Apple Device", "Apple Nearby", "AirDrop", "AirPlay", "Apple Handoff", "AirPods",
    "Samsung Device", "Google Device", "Microsoft Device",
    "BLE Device", "Phone", "Earbuds", "Headphones", "Speaker"
)

fun isCommonBleDevice(ble: ScannedBle): Boolean = ble.deviceType in COMMON_BLE_TYPES

val WifiTypeColor = Color(0xFF1976D2)
val BleTypeColor = Color(0xFF8E24AA)
val CellTypeColor = Color(0xFFE64A19)

// Log-distance path-loss estimate. txPower is the assumed RSSI at 1 meter;
// most BLE devices do not advertise their actual calibrated value.
private const val BLE_ASSUMED_TX_POWER = -59.0
private const val BLE_PATH_LOSS_EXPONENT = 2.5

fun estimateBleDistanceMeters(rssi: Int): Double {
    return Math.pow(10.0, (BLE_ASSUMED_TX_POWER - rssi) / (10.0 * BLE_PATH_LOSS_EXPONENT))
}

fun formatDistance(meters: Double): String {
    return when {
        meters < 1.0 -> "${(meters * 100).toInt()} cm"
        meters >= 1000.0 -> "%.1f km".format(meters / 1000.0)
        else -> "%.1f m".format(meters)
    }
}

// Log-distance estimate for cell towers. Very rough: a single RSRP/RSSI reading
// maps poorly to real range, so treat this as an order-of-magnitude hint only.
private const val CELL_ASSUMED_TX_POWER = -40.0
private const val CELL_PATH_LOSS_EXPONENT = 3.0

fun estimateCellDistanceMeters(rssi: Int): Double {
    if (rssi >= 0) return 0.0
    return Math.pow(10.0, (CELL_ASSUMED_TX_POWER - rssi) / (10.0 * CELL_PATH_LOSS_EXPONENT))
}

// Normalize a cell reading to 0 (poor) .. 1 (good). 5G NR uses a higher usable floor
// than 4G/legacy, so the good/poor window shifts by network type.
fun cellSignalQuality(rssi: Int, networkType: String): Float {
    val nr = networkType.contains("NR")
    val weak = if (nr) -115f else -120f
    val strong = if (nr) -80f else -85f
    return ((rssi - weak) / (strong - weak)).coerceIn(0f, 1f)
}

private const val HEAT_GRID_DEG = 0.00015 // ~16 m grid cells

// Collapse raw samples onto a grid, keeping the best signal ever seen at each cell,
// so the map shows where a call is most likely to succeed rather than momentary dips.
fun aggregateHeatSamples(raw: List<CellSample>, targetMccMnc: String? = null): List<HeatSample> {
    val grid = HashMap<Long, HeatSample>()
    for (s in raw) {
        if (s.lat == 0.0 && s.lon == 0.0) continue
        if (!targetMccMnc.isNullOrBlank() && s.mccMnc != targetMccMnc) continue
        val q = cellSignalQuality(s.rssi, s.networkType)
        val latKey = Math.round(s.lat / HEAT_GRID_DEG)
        val lonKey = Math.round(s.lon / HEAT_GRID_DEG)
        val key = latKey * 4_000_000L + lonKey
        val existing = grid[key]
        if (existing == null || q > existing.quality) {
            grid[key] = HeatSample(latKey * HEAT_GRID_DEG, lonKey * HEAT_GRID_DEG, q)
        }
    }
    return grid.values.toList()
}

// Free-space path loss estimate for WiFi; rough indoors but useful for relative comparison.
fun estimateWifiDistanceMeters(rssi: Int, freqMhz: Int): Double {
    if (freqMhz <= 0) return 0.0
    val exp = (27.55 - 20.0 * Math.log10(freqMhz.toDouble()) + Math.abs(rssi)) / 20.0
    return Math.pow(10.0, exp)
}

fun channelFromFrequency(freq: Int): Int = when {
    freq == 2484 -> 14
    freq in 2412..2472 -> (freq - 2407) / 5
    freq in 5160..5895 -> (freq - 5000) / 5
    freq in 5955..7115 -> (freq - 5950) / 5
    else -> 0
}

fun formatTimestamp(ts: Long): String {
    if (ts <= 0L) return "Unknown"
    return java.text.SimpleDateFormat("MMM dd, yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(ts))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(wifiSniffer: WifiSniffer, bleSniffer: BleSniffer, cellSniffer: CellSniffer) {
    val aps by wifiSniffer.scannedAps.collectAsState()
    val bles by bleSniffer.scannedBle.collectAsState()
    val cells by cellSniffer.scannedCells.collectAsState()
    
    val isScanning by wifiSniffer.isScanning.collectAsState()
    val lastScan by wifiSniffer.lastScanTime.collectAsState()
    val sims by cellSniffer.activeSims.collectAsState()
    
    var secFilter by rememberSaveable { mutableStateOf(SecurityFilter.ALL) }
    var triFilter by rememberSaveable { mutableStateOf(TriangulationFilter.ALL) }
    var bandFilter by rememberSaveable { mutableStateOf(BandFilter.ALL) }
    var bleInterestFilter by rememberSaveable { mutableStateOf(BleInterestFilter.ALL) }
    var sniffWifi by rememberSaveable { mutableStateOf(true) }
    var sniffBle by rememberSaveable { mutableStateOf(true) }
    var sniffCell by rememberSaveable { mutableStateOf(true) }
    var excludeTvs by rememberSaveable { mutableStateOf(false) }
    var cellHeatmapMode by rememberSaveable { mutableStateOf(false) }
    var selectedSubId by rememberSaveable { mutableStateOf<Int?>(null) }

    var showSecurityMenu by remember { mutableStateOf(false) }
    var showTriangulationMenu by remember { mutableStateOf(false) }
    var showBandMenu by remember { mutableStateOf(false) }
    var showBleInterestMenu by remember { mutableStateOf(false) }
    var centerTrigger by remember { mutableIntStateOf(0) }
    var zoomInTrigger by remember { mutableIntStateOf(0) }
    var zoomOutTrigger by remember { mutableIntStateOf(0) }

    var showList by remember { mutableStateOf(false) }
    var showAllInList by remember { mutableStateOf(false) }
    var mapBounds by remember { mutableStateOf<org.osmdroid.util.BoundingBox?>(null) }
    var apsGroupExpanded by remember { mutableStateOf(true) }
    var blesGroupExpanded by remember { mutableStateOf(true) }
    var cellsGroupExpanded by remember { mutableStateOf(true) }
    var selectedListAp by remember { mutableStateOf<ScannedAp?>(null) }
    var selectedListBle by remember { mutableStateOf<ScannedBle?>(null) }
    var selectedListCell by remember { mutableStateOf<ScannedCell?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var bleGroupDialogKey by remember { mutableStateOf<String?>(null) }
    var bleGroupDialogDevices by remember { mutableStateOf<List<ScannedBle>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val appContext = LocalContext.current.applicationContext
    val dbHelper = remember { DatabaseHelper(appContext) }

    // Default the heatmap to the default SIM once SIMs are known.
    LaunchedEffect(sims) {
        if (sims.isNotEmpty() && (selectedSubId == null || sims.none { it.subId == selectedSubId })) {
            selectedSubId = sims.firstOrNull { it.isDefault }?.subId ?: sims.first().subId
        }
    }
    val selectedSim = sims.firstOrNull { it.subId == selectedSubId }

    // Rebuild the heatmap when entering the mode, on every new cell scan, or on SIM switch.
    // Filter by the selected SIM's carrier so each SIM reports individually.
    var heatSamples by remember { mutableStateOf<List<HeatSample>>(emptyList()) }
    LaunchedEffect(cellHeatmapMode, cells, selectedSubId) {
        if (cellHeatmapMode) {
            val mcc = sims.firstOrNull { it.subId == selectedSubId }?.mccMnc
            heatSamples = withContext(Dispatchers.IO) { aggregateHeatSamples(dbHelper.getCellSignalSamples(), mcc) }
        }
    }

    // Memoized so recompositions triggered by unrelated state (map pans, timer ticks)
    // reuse the same list instances and do not force a map overlay rebuild.
    val filteredAps = remember(aps, secFilter, triFilter, bandFilter, sniffWifi, excludeTvs) {
        if (!sniffWifi) emptyList() else aps.filter { ap ->
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
            val passDev = !excludeTvs || (!ap.capabilities.contains("P2P") && !ap.capabilities.contains("WFD"))
            passSec && passTri && passBand && passDev
        }
    }

    val filteredBles = remember(bles, sniffBle, bleInterestFilter) {
        val base = if (sniffBle) bles else emptyList()
        if (bleInterestFilter == BleInterestFilter.INTERESTING) base.filter { !isCommonBleDevice(it) } else base
    }
    val filteredCells = remember(cells, sniffCell) {
        if (sniffCell) cells else emptyList()
    }

    // "Local" for the list view means "currently within the map's visible area".
    // When the "All" checkbox is unchecked, restrict the list to that area.
    // Sorted here once per data change instead of on every recomposition inside the list.
    val bounds = mapBounds
    val listAps = remember(filteredAps, showAllInList, bounds) {
        (if (showAllInList || bounds == null) filteredAps else filteredAps.filter { bounds.contains(it.estLat, it.estLon) })
            .sortedByDescending { it.rssi }
    }
    val listBles = remember(filteredBles, showAllInList, bounds) {
        (if (showAllInList || bounds == null) filteredBles else filteredBles.filter { bounds.contains(it.estLat, it.estLon) })
            .sortedByDescending { it.rssi }
    }
    val listCells = remember(filteredCells, showAllInList, bounds) {
        (if (showAllInList || bounds == null) filteredCells else filteredCells.filter { bounds.contains(it.estLat, it.estLon) })
            .sortedByDescending { it.rssi }
    }

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
            zoomOutTrigger = zoomOutTrigger,
            cellHeatmapMode = cellHeatmapMode,
            heatSamples = heatSamples,
            onBleGroupClick = { key, devices ->
                bleGroupDialogKey = key
                bleGroupDialogDevices = devices
            },
            onVisibleBoundsChanged = { mapBounds = it }
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            // Map legend: counts found per signal type; dimmed when that type is unchecked
            Column(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(
                    if (cellHeatmapMode) "Cell Signal" else "Devices Found",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                if (cellHeatmapMode) {
                    HeatLegend()
                    if (sims.size > 1) {
                        SimToggleButton(selectedSim?.carrierName ?: "SIM") {
                            val idx = sims.indexOfFirst { it.subId == selectedSubId }
                            val next = sims[(if (idx < 0) 0 else idx + 1) % sims.size]
                            selectedSubId = next.subId
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FoundBox(Icons.Filled.Bluetooth, BleTypeColor, bles.size, sniffBle) {
                            sniffBle = !sniffBle
                            if (sniffBle) bleSniffer.startScanning() else bleSniffer.stopScanning()
                        }
                        FoundBox(Icons.Filled.Wifi, WifiTypeColor, aps.size, sniffWifi) {
                            sniffWifi = !sniffWifi
                            if (sniffWifi) wifiSniffer.startScanning() else wifiSniffer.stopScanning()
                        }
                        FoundBox(Icons.Filled.SettingsInputAntenna, CellTypeColor, cells.size, sniffCell) {
                            sniffCell = !sniffCell
                            if (sniffCell) cellSniffer.startScanning() else cellSniffer.stopScanning()
                        }
                    }
                }
            }
            HeatmapToggleButton(active = cellHeatmapMode) {
                cellHeatmapMode = !cellHeatmapMode
                if (cellHeatmapMode) cellSniffer.startScanning()
            }
            }

            // Right side: Info button only
            IconButton(
                onClick = { showInfoDialog = true }
            ) {
                Icon(Icons.Filled.Info, contentDescription = "Info", tint = Color.White)
            }
        }

        // Scrim overlay to close filters when tapping outside
        if (showFilters) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showFilters = false }
            )
        }

        // Filters tab (only visible when collapsed and not in heatmap mode)
        if (!showFilters && !cellHeatmapMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(y = (-190).dp)
                    .clickable { showFilters = true }
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                    )
                    .padding(vertical = 16.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    "Filters".forEach { ch ->
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
        }

        // Filter panel (slides in/out from right edge)
        androidx.compose.animation.AnimatedVisibility(
            visible = showFilters,
            enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }),
            exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(y = (-190).dp)
        ) {
            Column(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    .padding(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "\u2715",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { showFilters = false }
                        .padding(bottom = 4.dp)
                        .align(Alignment.End)
                )
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
                SnifferCheckRow("Hide TVs & Printers", excludeTvs) { excludeTvs = it }
                Box {
                    InputChip(
                        selected = bleInterestFilter == BleInterestFilter.INTERESTING,
                        onClick = { showBleInterestMenu = true },
                        label = { Text("BLE: ${if (bleInterestFilter == BleInterestFilter.ALL) "All" else "Interesting"}") }
                    )
                    DropdownMenu(
                        expanded = showBleInterestMenu,
                        onDismissRequest = { showBleInterestMenu = false }
                    ) {
                        DropdownMenuItem(text = { Text("All BLE Devices") }, onClick = { bleInterestFilter = BleInterestFilter.ALL; showBleInterestMenu = false })
                        DropdownMenuItem(text = { Text("Interesting Only") }, onClick = { bleInterestFilter = BleInterestFilter.INTERESTING; showBleInterestMenu = false })
                    }
                }
            }
        }

        // Empty heatmap hint
        if (cellHeatmapMode && heatSamples.isEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    "Walk around to build the signal map.",
                    color = Color.White,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
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

            // List Button (hidden in heatmap mode)
            if (!cellHeatmapMode) {
                FloatingActionButton(onClick = { showList = true }) {
                    Icon(Icons.Filled.List, contentDescription = "List Devices")
                }
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

    // BLE Group detail modal
    if (bleGroupDialogKey != null) {
        AlertDialog(
            onDismissRequest = {
                bleGroupDialogKey = null
                bleGroupDialogDevices = emptyList()
            },
            title = { Text("BLE Group: ${bleGroupDialogKey}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("${bleGroupDialogDevices.size} devices", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(bleGroupDialogDevices.sortedByDescending { it.rssi }) { ble ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Text(ble.mac, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                val displayName = if (ble.name != "Unknown" && ble.name.isNotEmpty()) ble.name else ble.deviceType
                                Text("Name: $displayName", fontSize = 13.sp)
                                Text("RSSI: ${ble.rssi} dBm | Type: ${ble.deviceType}", fontSize = 12.sp, color = Color.Gray)
                                Text("Last seen: ${formatTimestamp(ble.lastSeen)}", fontSize = 12.sp, color = Color.Gray)
                                HorizontalDivider(modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    bleGroupDialogKey = null
                    bleGroupDialogDevices = emptyList()
                }) { Text("Close") }
            }
        )
    }

    AnimatedVisibility(
        visible = showList,
        enter = fadeIn() + scaleIn(initialScale = 0.96f),
        exit = fadeOut() + scaleOut(targetScale = 0.96f)
    ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 120.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 12.dp)
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.Gray.copy(alpha = 0.35f))
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${if (showAllInList) "All Devices" else "Devices Nearby"} (${listAps.size + listBles.size + listCells.size})",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { showList = false }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = showAllInList, onCheckedChange = { showAllInList = it })
                            Text(
                                "All Devices",
                                fontSize = 13.sp
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            val listState = rememberLazyListState()
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(end = 8.dp)
                            ) {
                                stickyHeader {
                                    DeviceGroupHeader(
                                        title = "WiFi Networks",
                                        count = listAps.size,
                                        icon = Icons.Filled.Wifi,
                                        tint = WifiTypeColor,
                                        expanded = apsGroupExpanded,
                                        onToggle = { apsGroupExpanded = !apsGroupExpanded }
                                    )
                                }
                                if (apsGroupExpanded) {
                                    if (listAps.isEmpty()) {
                                        item { EmptyGroupRow("No WiFi networks in view") }
                                    } else {
                                        items(listAps, key = { it.bssid }) { ap ->
                                            DeviceListRow(
                                                primary = if (ap.ssid.isNotEmpty()) ap.ssid else "[Hidden]",
                                                secondary = "${ap.frequency} MHz",
                                                rssi = ap.rssi,
                                                icon = Icons.Filled.Wifi,
                                                tint = WifiTypeColor,
                                                onClick = { selectedListAp = ap },
                                                modifier = Modifier.animateItemPlacement(),
                                                secured = ap.isSecured,
                                                distanceMeters = estimateWifiDistanceMeters(ap.rssi, ap.frequency)
                                            )
                                        }
                                    }
                                }
                                stickyHeader {
                                    DeviceGroupHeader(
                                        title = "Bluetooth Devices",
                                        count = listBles.size,
                                        icon = Icons.Filled.Bluetooth,
                                        tint = BleTypeColor,
                                        expanded = blesGroupExpanded,
                                        onToggle = { blesGroupExpanded = !blesGroupExpanded }
                                    )
                                }
                                if (blesGroupExpanded) {
                                    if (listBles.isEmpty()) {
                                        item { EmptyGroupRow("No Bluetooth devices in view") }
                                    } else {
                                        items(listBles, key = { it.mac }) { ble ->
                                            DeviceListRow(
                                                primary = if (ble.name.isNotEmpty()) ble.name else "[Unknown]",
                                                secondary = ble.deviceType,
                                                rssi = ble.rssi,
                                                icon = Icons.Filled.Bluetooth,
                                                tint = BleTypeColor,
                                                onClick = { selectedListBle = ble },
                                                modifier = Modifier.animateItemPlacement(),
                                                distanceMeters = estimateBleDistanceMeters(ble.rssi)
                                            )
                                        }
                                    }
                                }
                                stickyHeader {
                                    DeviceGroupHeader(
                                        title = "Cell Towers",
                                        count = listCells.size,
                                        icon = Icons.Filled.SettingsInputAntenna,
                                        tint = CellTypeColor,
                                        expanded = cellsGroupExpanded,
                                        onToggle = { cellsGroupExpanded = !cellsGroupExpanded }
                                    )
                                }
                                if (cellsGroupExpanded) {
                                    if (listCells.isEmpty()) {
                                        item { EmptyGroupRow("No cell towers in view") }
                                    } else {
                                        items(listCells, key = { it.cellId }) { cell ->
                                            DeviceListRow(
                                                primary = cell.cellId,
                                                secondary = cell.networkType,
                                                rssi = cell.rssi,
                                                icon = Icons.Filled.SettingsInputAntenna,
                                                tint = CellTypeColor,
                                                onClick = { selectedListCell = cell },
                                                modifier = Modifier.animateItemPlacement(),
                                                distanceMeters = estimateCellDistanceMeters(cell.rssi)
                                            )
                                        }
                                    }
                                }
                            }
                            SimpleVerticalScrollbar(
                                listState = listState,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }

        // AP detail modal
        if (selectedListAp != null) {
            val ap = selectedListAp!!
            var apStats by remember(ap.bssid) { mutableStateOf<ObservationStats?>(null) }
            var testing by remember(ap.bssid) { mutableStateOf(false) }
            var testResult by remember(ap.bssid) { mutableStateOf<WifiTestResult?>(null) }
            LaunchedEffect(ap.bssid) {
                apStats = withContext(Dispatchers.IO) { dbHelper.getObservationStats(ap.bssid) }
            }
            val canTest = !ap.isSecured && ap.ssid.isNotEmpty() &&
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
            AlertDialog(
                onDismissRequest = { selectedListAp = null },
                title = { Text(if (ap.ssid.isNotEmpty()) ap.ssid else "[Hidden]", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("BSSID: ${ap.bssid}")
                        Text("Vendor: ${VendorLookup.getVendor(ap.bssid)}")
                        val ch = channelFromFrequency(ap.frequency)
                        Text("Freq: ${ap.frequency} MHz${if (ch > 0) " (ch $ch)" else ""} | ${ap.wifiStandard}")
                        Text("RSSI: ${ap.rssi} dBm | Weight: ${ap.totalWeight.toInt()}")
                        Text("Estimated distance: ~${formatDistance(estimateWifiDistanceMeters(ap.rssi, ap.frequency))}")
                        val secText = if (ap.securityType.isNotEmpty()) ap.securityType else if (ap.isSecured) "Secured" else "Open"
                        Text("Security: $secText")
                        val stats = apStats
                        if (stats != null && stats.count > 0) {
                            Text("Observations: ${stats.count}")
                            Text("First seen: ${formatTimestamp(stats.firstSeen)}", fontSize = 12.sp, color = Color.Gray)
                            Text("Last seen: ${formatTimestamp(stats.lastSeen)}", fontSize = 12.sp, color = Color.Gray)
                        }
                        Text("Capabilities: ${ap.capabilities}", fontSize = 12.sp, color = Color.Gray)

                        if (testing) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Text("Testing. Approve the connect dialog.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 6.dp))
                        }
                        val res = testResult
                        if (res != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(6.dp))
                            if (!res.connected) {
                                Text("Connect failed: ${res.error ?: "unknown"}", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                            } else {
                                val status = when {
                                    res.hasInternet -> "Internet OK" to Color(0xFF2E7D32)
                                    res.captivePortal -> "Captive portal (sign-in required)" to Color(0xFFF9A825)
                                    else -> "Connected, no internet" to Color(0xFFC62828)
                                }
                                Text(status.first, color = status.second, fontWeight = FontWeight.Bold)
                                if (res.ipAddress.isNotEmpty()) Text("IP: ${res.ipAddress}", fontSize = 12.sp)
                                if (res.gateway.isNotEmpty()) Text("Gateway: ${res.gateway}", fontSize = 12.sp)
                                if (res.dns.isNotEmpty()) Text("DNS: ${res.dns}", fontSize = 12.sp)
                                if (res.linkSpeedMbps >= 0) Text("Link speed: ${res.linkSpeedMbps} Mbps", fontSize = 12.sp)
                                if (res.latencyMs >= 0) Text("Probe latency: ${res.latencyMs} ms", fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedListAp = null }) { Text("Close") }
                },
                dismissButton = {
                    if (canTest) {
                        TextButton(
                            enabled = !testing,
                            onClick = {
                                testing = true
                                testResult = null
                                scope.launch {
                                    val r = WifiTester(appContext).testOpenNetwork(ap.ssid)
                                    testResult = r
                                    testing = false
                                }
                            }
                        ) { Text(if (testing) "Testing..." else "Test connect") }
                    }
                }
            )
        }

        // BLE detail modal
        if (selectedListBle != null) {
            val ble = selectedListBle!!
            var bleStats by remember(ble.mac) { mutableStateOf<ObservationStats?>(null) }
            LaunchedEffect(ble.mac) {
                bleStats = withContext(Dispatchers.IO) { dbHelper.getObservationStats(ble.mac) }
            }
            AlertDialog(
                onDismissRequest = { selectedListBle = null },
                title = { Text(if (ble.name.isNotEmpty()) ble.name else "[Unknown]", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("MAC: ${ble.mac}")
                        Text("Vendor: ${VendorLookup.getVendor(ble.mac)}")
                        Text("RSSI: ${ble.rssi} dBm | Weight: ${ble.totalWeight.toInt()}")
                        Text("Estimated distance: ~${formatDistance(estimateBleDistanceMeters(ble.rssi))}")
                        Text("Type: ${ble.deviceType}")
                        Text("Last seen: ${formatTimestamp(ble.lastSeen)}")
                        val stats = bleStats
                        if (stats != null && stats.count > 0) {
                            Text("Observations: ${stats.count}")
                            Text("First seen: ${formatTimestamp(stats.firstSeen)}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedListBle = null }) { Text("Close") }
                }
            )
        }

        // Cell tower detail modal
        if (selectedListCell != null) {
            val cell = selectedListCell!!
            var cellStats by remember(cell.cellId) { mutableStateOf<ObservationStats?>(null) }
            LaunchedEffect(cell.cellId) {
                cellStats = withContext(Dispatchers.IO) { dbHelper.getObservationStats(cell.cellId) }
            }
            AlertDialog(
                onDismissRequest = { selectedListCell = null },
                title = { Text(cell.cellId, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Operator: ${cell.owner}")
                        Text("Network: ${cell.networkType}")
                        Text("RSSI: ${cell.rssi} dBm | Weight: ${cell.totalWeight.toInt()}")
                        Text("Estimated distance: ~${formatDistance(estimateCellDistanceMeters(cell.rssi))} (rough)")
                        Text("MCC/MNC: ${cell.mccMnc}")
                        Text("LAC/TAC: ${cell.lac}")
                        Text("PCI: ${cell.pci}")
                        Text("Band: ${cell.band}")
                        val stats = cellStats
                        if (stats != null && stats.count > 0) {
                            Text("Observations: ${stats.count}")
                            Text("First seen: ${formatTimestamp(stats.firstSeen)}", fontSize = 12.sp, color = Color.Gray)
                            Text("Last seen: ${formatTimestamp(stats.lastSeen)}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedListCell = null }) { Text("Close") }
                }
            )
        }
    }
}

@Composable
fun HeatmapToggleButton(active: Boolean, onClick: () -> Unit) {
    // Sized and shaped like the app's FABs (56 dp, 16 dp corners, soft shadow) so it
    // reads as a peer control; the radial gradient keeps its signal-heatmap identity.
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .shadow(6.dp, shape)
            .size(56.dp)
            .clip(shape)
            .background(
                androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(Color(0xFFFFEB3B), Color(0xFFFF9800), Color(0xFFD32F2F))
                )
            )
            .then(if (active) Modifier.border(3.dp, Color.White, shape) else Modifier)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.Call,
            contentDescription = "Cell signal heatmap",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun SimToggleButton(label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(Icons.Filled.SwapHoriz, contentDescription = "Switch SIM", tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HeatLegend() {
    Column(modifier = Modifier.padding(top = 4.dp)) {
        Box(
            modifier = Modifier
                .padding(vertical = 2.dp)
                .width(120.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(Color(0xFFD32F2F), Color(0xFFFFC107), Color(0xFF2E7D32))
                    )
                )
        )
        Row(modifier = Modifier.width(120.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Poor", color = Color.White, fontSize = 10.sp)
            Text("Good", color = Color.White, fontSize = 10.sp)
        }
    }
}

@Composable
fun FoundBox(icon: ImageVector, tint: Color, count: Int, enabled: Boolean, onClick: () -> Unit) {
    val alpha = if (enabled) 1f else 0.3f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint.copy(alpha = alpha), modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("$count", color = Color.White.copy(alpha = alpha), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SnifferCheckRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 13.sp)
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(uncheckedColor = Color.White)
        )
    }
}

@Composable
fun DeviceGroupHeader(
    title: String,
    count: Int,
    icon: ImageVector,
    tint: Color,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "chevronRotation")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            }
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(
                "$count",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.rotate(rotation)
            )
        }
        HorizontalDivider()
    }
}

@Composable
fun EmptyGroupRow(text: String) {
    Text(
        text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        fontSize = 13.sp,
        color = Color.Gray
    )
    HorizontalDivider()
}

@Composable
fun SignalIndicator(rssi: Int, distanceMeters: Double? = null) {
    val color = when {
        rssi >= -60 -> Color(0xFF2E7D32)
        rssi >= -80 -> Color(0xFFF9A825)
        else -> Color(0xFFC62828)
    }
    Column(horizontalAlignment = Alignment.End) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text("$rssi dBm", fontSize = 11.sp, color = Color.Gray)
        if (distanceMeters != null) {
            Text("~${formatDistance(distanceMeters)}", fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun SimpleVerticalScrollbar(listState: androidx.compose.foundation.lazy.LazyListState, modifier: Modifier = Modifier) {
    val layoutInfo = listState.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val visibleItems = layoutInfo.visibleItemsInfo.size
    if (totalItems == 0 || visibleItems >= totalItems) return

    val thumbFraction = (visibleItems.toFloat() / totalItems.toFloat()).coerceIn(0.08f, 1f)
    val maxScrollIndex = (totalItems - visibleItems).coerceAtLeast(1)
    val topFraction = (listState.firstVisibleItemIndex.toFloat() / maxScrollIndex.toFloat()).coerceIn(0f, 1f) * (1f - thumbFraction)

    BoxWithConstraints(modifier = modifier.width(6.dp)) {
        val trackHeight = maxHeight
        Box(
            modifier = Modifier
                .offset(y = trackHeight * topFraction)
                .width(4.dp)
                .height(trackHeight * thumbFraction)
                .align(Alignment.TopEnd)
                .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
        )
    }
}

@Composable
fun DeviceListRow(
    primary: String,
    secondary: String,
    rssi: Int,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    distanceMeters: Double? = null,
    secured: Boolean? = null
) {
    ListItem(
        headlineContent = {
            Text(primary, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
        },
        supportingContent = {
            Text(secondary, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
            Box(modifier = Modifier.size(34.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.TopStart)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                }
                // Lock badge marks WiFi rows as secured or open
                if (secured != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (secured) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = if (secured) "Secured" else "Open",
                            tint = if (secured) Color(0xFF2E7D32) else Color(0xFFE64A19),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        },
        trailingContent = { SignalIndicator(rssi, distanceMeters) },
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    )
    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
}

@Composable
fun ScanTimerClock(lastScanTime: Long, isScanning: Boolean) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // 10 Hz is smooth enough for a sweep hand and avoids recomposing every frame.
    LaunchedEffect(isScanning) {
        if (isScanning) {
            while(true) {
                currentTime = System.currentTimeMillis()
                delay(100L)
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
