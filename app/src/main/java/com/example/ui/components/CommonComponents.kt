package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.FindingSeverity
import com.example.core.model.RiskLevel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodePilotTopBar(
    title: String,
    subtitle: String? = null,
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp, end = 4.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "CodePilot Logo",
                        tint = PrimaryCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun StatusBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .size(14.dp)
                    .padding(end = 4.dp)
            )
        }
        Text(
            text = text,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun RiskLevelBadge(riskLevel: RiskLevel) {
    when (riskLevel) {
        RiskLevel.LOW -> StatusBadge("LOW RISK", AccentGreen.copy(alpha = 0.15f), AccentGreen, Icons.Default.CheckCircle)
        RiskLevel.MEDIUM -> StatusBadge("MED RISK", AccentAmber.copy(alpha = 0.15f), AccentAmber, Icons.Default.Warning)
        RiskLevel.HIGH -> StatusBadge("HIGH RISK", AccentRed.copy(alpha = 0.15f), AccentRed, Icons.Default.Error)
    }
}

@Composable
fun SeverityBadge(severity: FindingSeverity) {
    when (severity) {
        FindingSeverity.CRITICAL -> StatusBadge("CRITICAL", AccentRed.copy(alpha = 0.2f), AccentRed, Icons.Default.Report)
        FindingSeverity.HIGH -> StatusBadge("HIGH", AccentAmber.copy(alpha = 0.2f), AccentAmber, Icons.Default.Warning)
        FindingSeverity.MEDIUM -> StatusBadge("MEDIUM", PrimaryCyan.copy(alpha = 0.2f), PrimaryCyan, Icons.Default.Info)
        FindingSeverity.LOW -> StatusBadge("LOW", TextSecondary.copy(alpha = 0.2f), TextSecondary, Icons.Default.Help)
        FindingSeverity.INFO -> StatusBadge("INFO", AccentGreen.copy(alpha = 0.2f), AccentGreen, Icons.Default.Lightbulb)
    }
}

@Composable
fun UnifiedDiffViewer(
    diffText: String,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val lines = diffText.lines()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(EditorBackground)
            .border(1.dp, SurfaceVariantDark, RoundedCornerShape(8.dp))
            .horizontalScroll(scrollState)
            .padding(8.dp)
    ) {
        for (line in lines) {
            val (bgColor, textColor) = when {
                line.startsWith("+") && !line.startsWith("+++") -> DiffAddBg to DiffAddText
                line.startsWith("-") && !line.startsWith("---") -> DiffDeleteBg to DiffDeleteText
                line.startsWith("@@") -> PrimaryCyan.copy(alpha = 0.15f) to PrimaryCyan
                line.startsWith("---") || line.startsWith("+++") -> Color.Transparent to Color(0xFF94A3B8)
                else -> Color.Transparent to Color(0xFFCBD5E1)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = line.ifEmpty { " " },
                    color = textColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = { Text(text = message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (isDestructive) ButtonDefaults.buttonColors(containerColor = AccentRed)
                else ButtonDefaults.buttonColors()
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}
