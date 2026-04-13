import sys

with open('app/src/main/java/com/example/androidsignalswifi/MainActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

imps = '''import androidx.compose.material3.*
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import android.content.pm.PackageInfo
import android.content.pm.PackageManager'''

text = text.replace('import androidx.compose.material3.*', imps)

with open('app/src/main/java/com/example/androidsignalswifi/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)
