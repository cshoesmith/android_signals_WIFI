import sys

with open('app/src/main/java/com/example/androidsignalswifi/MainActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

# Fix duplicates in MainActivity
text = text.replace('    val isScanning by wifiSniffer.isScanning.collectAsState()\\n    val isScanning by wifiSniffer.isScanning.collectAsState()', '    val isScanning by wifiSniffer.isScanning.collectAsState()')

# Add imports for Play/Close
imp_loc = '''import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close'''
text = text.replace('import androidx.compose.material.icons.filled.Info', imp_loc)

# Rewrite the button
old_btn = '''            // Play/Pause Button
            FloatingActionButton(onClick = { 
                if (isScanning) wifiSniffer.stopScanning() else wifiSniffer.startScanning() 
            }) {
                Icon(if (isScanning) androidx.compose.material.icons.filled.Close else androidx.compose.material.icons.filled.PlayArrow, contentDescription = "Play/Pause")
            }'''
new_btn = '''            // Play/Pause Button
            FloatingActionButton(onClick = { 
                if (isScanning) wifiSniffer.stopScanning() else wifiSniffer.startScanning() 
            }) {
                Icon(if (isScanning) Icons.Filled.Close else Icons.Filled.PlayArrow, contentDescription = "Play/Pause")
            }'''
text = text.replace(old_btn, new_btn)

with open('app/src/main/java/com/example/androidsignalswifi/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)

