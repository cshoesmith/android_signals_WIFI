import sys

with open('app/src/main/java/com/example/androidsignalswifi/MainActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('import androidx.compose.material.icons.filled.Close', '')
text = text.replace('import androidx.compose.material.icons.filled.Close', '')

import_close = '''import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close'''
text = text.replace('import androidx.compose.material.icons.filled.PlayArrow', import_close)

with open('app/src/main/java/com/example/androidsignalswifi/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)
