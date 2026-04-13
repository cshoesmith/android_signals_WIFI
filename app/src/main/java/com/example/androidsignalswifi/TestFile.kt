package com.example.androidsignalswifi

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestTool() {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text("A Tooltip") } },
        state = rememberTooltipState()
    ) {
        Text("Hover me")
    }
}
