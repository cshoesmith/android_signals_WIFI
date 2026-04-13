import sys

with open('app/src/main/java/com/example/androidsignalswifi/WifiSniffer.kt', 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('val lastScanTime: StateFlow<Long> = _lastScanTime', 
'''val lastScanTime: StateFlow<Long> = _lastScanTime

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning''')

text = text.replace('scanningJob = CoroutineScope(Dispatchers.IO).launch {',
'''_isScanning.value = true
        scanningJob = CoroutineScope(Dispatchers.IO).launch {''')

text = text.replace('scanningJob?.cancel()',
'''_isScanning.value = false
        scanningJob?.cancel()''')

with open('app/src/main/java/com/example/androidsignalswifi/WifiSniffer.kt', 'w', encoding='utf-8') as f:
    f.write(text)
