import sys

with open('app/src/main/java/com/example/androidsignalswifi/MainActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

# Add imports for Play/Pause icons
imports = '''import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Close'''

# wait close? No, there is no pause icon by default I think. Well there is Icons.Filled.Close, Icons.Filled.PlayArrow

# Let's add isScanning
text = text.replace('val aps by wifiSniffer.scannedAps.collectAsState()',
'''val aps by wifiSniffer.scannedAps.collectAsState()
    val isScanning by wifiSniffer.isScanning.collectAsState()''')

# Add the Play/Pause button and import Pause if it exists
imp_base = '''import androidx.core.content.ContextCompat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog'''
text = text.replace('''import androidx.core.content.ContextCompat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog''', imp_base)

button_code = '''            // Play/Pause Button
            FloatingActionButton(onClick = { 
                if (isScanning) wifiSniffer.stopScanning() else wifiSniffer.startScanning() 
            }) {
                Icon(if (isScanning) androidx.compose.material.icons.filled.Close else androidx.compose.material.icons.filled.PlayArrow, contentDescription = "Play/Pause")
            }

            // Center Map Button'''

text = text.replace('            // Center Map Button', button_code)

with open('app/src/main/java/com/example/androidsignalswifi/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)
