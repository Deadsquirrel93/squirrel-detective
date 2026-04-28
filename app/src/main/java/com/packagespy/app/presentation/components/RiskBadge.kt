package com.packagespy.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.packagespy.app.R
import com.packagespy.app.domain.model.RiskLevel
import com.packagespy.app.presentation.theme.RiskColors

@Composable
fun RiskBadge(level: RiskLevel, modifier: Modifier = Modifier) {
    val labelRes = when (level) {
        RiskLevel.RED -> R.string.risk_red
        RiskLevel.YELLOW -> R.string.risk_yellow
        RiskLevel.GREEN -> R.string.risk_green
        RiskLevel.SAFE -> R.string.risk_safe
    }
    Text(
        text = stringResource(labelRes),
        color = RiskColors.textFor(level),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(RiskColors.bgFor(level))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
