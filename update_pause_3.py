import sys
import re

with open('app/src/main/java/com/example/androidsignalswifi/MainActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

# 1. Imports
imp_fixed = '''import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.res.painterResource'''
text = text.replace('import androidx.compose.material.icons.filled.PlayArrow', imp_fixed)

# 2. Button Fix
text = re.sub(
    r'Icon\(if \(isScanning\) Icons\.Filled\.(Pause|Close) else Icons\.Filled\.PlayArrow, contentDescription = "Play/Pause"\)',
    '''if (isScanning) {
                    Icon(painter = painterResource(id = android.R.drawable.ic_media_pause), contentDescription = "Pause", modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                }''',
    text
)

# 3. ScanTimerClock Signature Fix
old_func = r'''@Composable
fun ScanTimerClock\(lastScanTime: Long\)\s*\{.*?var currentTime.*?LaunchedEffect.*?\{.*?\}.*?\}'''
new_func = '''@Composable
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
text = re.sub(old_func, new_func, text, flags=re.DOTALL)

# 4. ScanTimerClock Call Fix
text = text.replace('ScanTimerClock(lastScanTime = lastScan)', 'ScanTimerClock(lastScanTime = lastScan, isScanning = isScanning)')

# 5. Clean up duplicate local val isScanning: Boolean
text = text.replace('    val isScanning by wifiSniffer.isScanning.collectAsState()\\n    val isScanning by wifiSniffer.isScanning.collectAsState()', '    val isScanning by wifiSniffer.isScanning.collectAsState()')

# 6. Remove wrong imports causing unresolved errors
text = text.replace('import androidx.compose.material.icons.rounded.Pause', '')
text = text.replace('import androidx.compose.material.icons.filled.Pause', '')
text = text.replace('import androidx.compose.material.icons.filled.Close', '')

with open('app/src/main/java/com/example/androidsignalswifi/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)

