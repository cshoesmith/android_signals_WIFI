import sys

with open('app/src/main/java/com/example/androidsignalswifi/MainActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

# 1. Imports
text = text.replace('import androidx.compose.material.icons.filled.Close', 'import androidx.compose.material.icons.filled.Pause')

# 2. Update the button
btn_old = '''            // Play/Pause Button
            FloatingActionButton(onClick = {
                if (isScanning) wifiSniffer.stopScanning() else wifiSniffer.startScanning()
            }) {
                Icon(if (isScanning) Icons.Filled.Close else Icons.Filled.PlayArrow, contentDescription = "Play/Pause")
            }'''
btn_new = '''            // Play/Pause Button
            FloatingActionButton(onClick = {
                if (isScanning) wifiSniffer.stopScanning() else wifiSniffer.startScanning()
            }) {
                Icon(if (isScanning) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "Play/Pause")
            }'''
text = text.replace(btn_old, btn_new)

# 3. Update ScanTimerClock call
call_old = '''                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ScanTimerClock(lastScanTime = lastScan)
                }'''
call_new = '''                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ScanTimerClock(lastScanTime = lastScan, isScanning = isScanning)
                }'''
text = text.replace(call_old, call_new)

# 4. Update ScanTimerClock definition
func_old = '''@Composable
fun ScanTimerClock(lastScanTime: Long) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while(true) {
            androidx.compose.runtime.withFrameMillis {
                currentTime = System.currentTimeMillis()
            }
        }
    }'''

func_new = '''@Composable
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
    }'''
text = text.replace(func_old, func_new)

with open('app/src/main/java/com/example/androidsignalswifi/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)

