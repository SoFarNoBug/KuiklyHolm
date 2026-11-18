package com.jlj.kuiklyholmui

import androidx.compose.runtime.Composable
import com.jlj.kuiklyholmui.base.BasePager
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: Holm Demo 入口主页（示例 / 双渲染后端）。
 */
@Page("home", supportInLocal = true)
internal class HomePager : BasePager() {
    override fun willInit() {
        super.willInit()
        HolmDemoBootstrap.ensure()
        setContent {
            HomeScreen()
        }
    }
}

@Composable
private fun HomeScreen() {
    val localPager = LocalActivity.current.getPager() as Pager
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoTheme.Background)
            .padding(top = (statusBarHeight + 24).dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Holm",
            fontSize = 28.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            color = DemoTheme.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "通用渲染底座 · 喂 shader 出帧",
            fontSize = 15.sp,
            lineHeight = 15.sp,
            color = DemoTheme.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(40.dp))
        HomeButton(
            label = "示例 / 双渲染后端 (holm_dualtrack)",
            onClick = { openPage(localPager, "holm_dualtrack") },
        )
        Spacer(modifier = Modifier.height(16.dp))
        HomeButton(
            label = "Model / glTF (holm_model)",
            onClick = { openPage(localPager, "holm_model") },
        )
        Spacer(modifier = Modifier.height(16.dp))
        HomeButton(
            label = "kmp-wgpu Compute 回读 (v1.1)",
            onClick = { openPage(localPager, "kmp_wgpu_compute") },
        )
    }
}

@Composable
private fun HomeButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DemoTheme.Primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 40.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = DemoTheme.TextPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

private fun openPage(pager: Pager, pageName: String) {
    pager.acquireModule<RouterModule>(RouterModule.MODULE_NAME)
        .openPage(pageName, JSONObject())
}
