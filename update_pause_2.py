import sys

with open('app/src/main/java/com/example/androidsignalswifi/MainActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

# Make sure imports are perfect
text = text.replace('import androidx.compose.material.icons.filled.Close', '')
text = text.replace('import androidx.compose.material.icons.filled.Pause', '')

imp_fixed = '''import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.filled.Pause'''

text = text.replace('import androidx.compose.material.icons.filled.PlayArrow', imp_fixed)

# Button Fix
text = text.replace('Icon(if (isScanning) Icons.Filled.Close else Icons.Filled.PlayArrow, contentDescription = "Play/Pause")', 
'Icon(if (isScanning) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "Play/Pause")')


# ScanTimerClock Signature Fix
def_old = '''@Composable
fun ScanTimerClock(lastScanTime: Long) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while(true) {
            androidx.compose.runtime.withFrameMillis {
                currentTime = System.currentTimeMillis()
            }
        }
    }'''

def_new = '''@Composable
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

text = text.replace(def_old, def_new)

# ScanTimerClock Call Fix
call_old = 'ScanTimerClock(lastScanTime = lastScan)'
call_new = 'ScanTimerClock(lastScanTime = lastScan, isScanning = isScanning)'

text = text.replace(call_old, call_new)

with open('app/src/main/java/com/example/androidsignalswifi/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)

