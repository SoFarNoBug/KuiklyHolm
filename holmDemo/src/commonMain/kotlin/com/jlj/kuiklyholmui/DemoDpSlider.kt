package com.jlj.kuiklyholmui

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.Slider
import com.tencent.kuikly.compose.material3.SliderDefaults
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * @CreateDate: 2026/11/19
 * @Author: jlj
 * @Description: Demo 滑杆（样式对齐 KuiklyPerspectiveCrop DemoDpSlider）。
 */
@Composable
internal fun DemoDpSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    formatValue: (Float) -> String,
    snapToInt: Boolean = false,
    steps: Int = 0,
) {
    val lo = valueRange.start
    val hi = valueRange.endInclusive
    val trackInactive = Color(0xFF3A4150)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = DemoTheme.TextSecondary,
                fontSize = 11.sp,
                lineHeight = 11.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatValue(value),
                color = DemoTheme.TextPrimary,
                fontSize = 11.sp,
                lineHeight = 11.sp,
            )
        }
        Slider(
            value = value.coerceIn(lo, hi),
            onValueChange = { raw ->
                onValueChange(if (snapToInt) raw.roundToInt().toFloat() else raw)
            },
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = DemoTheme.Primary,
                activeTrackColor = DemoTheme.Primary,
                inactiveTrackColor = trackInactive,
                activeTickColor = DemoTheme.Primary,
                inactiveTickColor = trackInactive,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp),
        )
    }
}
