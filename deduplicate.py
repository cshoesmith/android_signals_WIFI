import sys

with open('app/src/main/java/com/example/androidsignalswifi/MainActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

# Fix duplicates in MainActivity
text = text.replace('    val isScanning by wifiSniffer.isScanning.collectAsState()\\n    val isScanning by wifiSniffer.isScanning.collectAsState()', '    val isScanning by wifiSniffer.isScanning.collectAsState()')

text = text.replace('import androidx.compose.material.icons.filled.Close', '')
text = text.replace('import androidx.compose.material.icons.filled.PlayArrow', '')

imp_base_insert = '''import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog'''

text = text.replace('''import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog''', imp_base_insert)


with open('app/src/main/java/com/example/androidsignalswifi/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)

with open('app/src/main/java/com/example/androidsignalswifi/WifiSniffer.kt', 'r', encoding='utf-8') as f:
    sniffer_text = f.read()

sniffer_text = sniffer_text.replace('''    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning''', '''    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning''')

sniffer_text = sniffer_text.replace('''        _isScanning.value = true
        _isScanning.value = true''', '''        _isScanning.value = true''')

sniffer_text = sniffer_text.replace('''        _isScanning.value = false
        _isScanning.value = false''', '''        _isScanning.value = false''')

with open('app/src/main/java/com/example/androidsignalswifi/WifiSniffer.kt', 'w', encoding='utf-8') as f:
    f.write(sniffer_text)
