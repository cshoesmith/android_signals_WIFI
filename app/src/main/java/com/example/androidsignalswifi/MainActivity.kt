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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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

        wifiSniffer = WifiSniffer(this)

        checkPermissionsAndStart()

        setContent {
            var showSplash by remember { mutableStateOf(true) }

            LaunchedEffect(key1 = true) {
                delay(2000)
                showSplash = false
            }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showSplash) {
                        SplashScreen()
                    } else {
                        val scannedAps by wifiSniffer.scannedAps.collectAsState()
                        MainScreenWithDrawer(scannedAps = scannedAps)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wifiSniffer.stopScanning()
    }

    private fun checkPermissionsAndStart() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            wifiSniffer.startScanning()
        }
    }
}

enum class SecurityFilter { ALL, OPEN, SECURED }
enum class TriangulationFilter { ALL, LEARNING, KNOWN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithDrawer(scannedAps: List<ScannedAp>) {
    var secFilter by remember { mutableStateOf(SecurityFilter.ALL) }
    var triFilter by remember { mutableStateOf(TriangulationFilter.ALL) }
    
    var showList by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()

    val filteredAps = scannedAps.filter { ap ->
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
        MapScreen(aps = filteredAps)

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
                onClick = { showBottomSheet = true },
                label = { Text("Security: \") },
                modifier = Modifier.padding(end = 8.dp)
            )
            InputChip(
                selected = true,
                onClick = { showBottomSheet = true },
                label = { Text("Triangulation: \") }
            )
        }

        // Bottom Bar Area containing filter button and list button
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(onClick = { showBottomSheet = true }) {
                Icon(Icons.Filled.FilterList, contentDescription = "Menu")
            }
            
            FloatingActionButton(onClick = { showList = true }) {
                Text("List (\)", modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Filter Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text("Security", fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = secFilter == SecurityFilter.ALL,
                            onClick = { secFilter = SecurityFilter.ALL },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                        ) { Text("All") }
                        SegmentedButton(
                            selected = secFilter == SecurityFilter.OPEN,
                            onClick = { secFilter = SecurityFilter.OPEN },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                        ) { Text("Open") }
                        SegmentedButton(
                            selected = secFilter == SecurityFilter.SECURED,
                            onClick = { secFilter = SecurityFilter.SECURED },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                        ) { Text("Secured") }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text("Triangulation Status", fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = triFilter == TriangulationFilter.ALL,
                            onClick = { triFilter = TriangulationFilter.ALL },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                        ) { Text("All") }
                        SegmentedButton(
                            selected = triFilter == TriangulationFilter.LEARNING,
                            onClick = { triFilter = TriangulationFilter.LEARNING },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                        ) { Text("Learning") }
                        SegmentedButton(
                            selected = triFilter == TriangulationFilter.KNOWN,
                            onClick = { triFilter = TriangulationFilter.KNOWN },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                        ) { Text("Known") }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showBottomSheet = false
                                }
                            }
                        }
                    ) {
                        Text("Apply & Close")
                    }
                }
            }
        }

        if (showList) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .systemBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Discovered APs (\)", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showList = false }) { Text("Close") }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(filteredAps) { ap ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    val ssidDisp = if (ap.ssid.isEmpty()) "[Hidden]" else ap.ssid
                                    val triangState = if (ap.totalWeight > 200.0) "Known" else "Learning"
                                    Text(text = "SSID: \", fontWeight = FontWeight.Bold)
                                    Text(text = "BSSID: \", style = MaterialTheme.typography.bodyMedium)
                                    Text(text = "RSSI: \ dBm", style = MaterialTheme.typography.bodyMedium)
                                    val secDisp = if (ap.isSecured) "Secured" else "Open"
                                    Text(text = "Security: \ | Triangulation: \", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "Est. Loc: \, \",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
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
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Color(0xFF003C8F), shape = RoundedCornerShape(percent = 50)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "App Icon",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "WIFI Sniffer",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}
