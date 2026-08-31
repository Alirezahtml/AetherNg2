package io.github.immaghzbad.aetherst.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.immaghzbad.aetherst.shared.model.UpdateInfo
import io.github.immaghzbad.aetherst.platform.isDesktop

private val IosActiveBlue = Color(0xFF007AFF)
private val IosCardBg = Color(0xFF1C1C1E)
private val IosSecondaryLabel = Color(0xFF8E8E93)

@Composable
fun UpdateScreen(
    info: UpdateInfo,
    onDismiss: () -> Unit,
    scaleFactor: Float = 1f
) {
    val uriHandler = LocalUriHandler.current
    val navBarPadding = if (isDesktop) 0.dp else WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = (24 * scaleFactor).dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(if (isDesktop) 24.dp else 48.dp))

            Box(
                modifier = Modifier
                    .size((80 * scaleFactor).dp)
                    .background(
                        Brush.linearGradient(listOf(IosActiveBlue, Color(0xFF5856D6))),
                        RoundedCornerShape((20 * scaleFactor).dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size((40 * scaleFactor).dp)
                )
            }

            Spacer(modifier = Modifier.height((24 * scaleFactor).dp))

            Text(
                text = "New Update Available",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = (24 * scaleFactor).sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height((8 * scaleFactor).dp))

            Surface(
                shape = RoundedCornerShape((50 * scaleFactor).dp),
                color = IosActiveBlue.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "v${info.version}${if (info.isBeta) "-BETA" else ""}",
                    modifier = Modifier.padding(horizontal = (12 * scaleFactor).dp, vertical = (4 * scaleFactor).dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = IosActiveBlue,
                    fontSize = (12 * scaleFactor).sp
                )
            }

            Spacer(modifier = Modifier.height((24 * scaleFactor).dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape((18 * scaleFactor).dp),
                colors = CardDefaults.cardColors(containerColor = IosCardBg)
            ) {
                Column(modifier = Modifier.padding((18 * scaleFactor).dp)) {
                    Text(
                        text = "What's New:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = (14 * scaleFactor).sp
                    )
                    Spacer(modifier = Modifier.height((12 * scaleFactor).dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = (200 * scaleFactor).dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = info.changelog,
                            style = MaterialTheme.typography.bodyMedium,
                            color = IosSecondaryLabel,
                            fontSize = (13 * scaleFactor).sp,
                            lineHeight = (20 * scaleFactor).sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height((24 * scaleFactor).dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(horizontal = (24 * scaleFactor).dp)
                .padding(bottom = (16 * scaleFactor).dp + navBarPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { uriHandler.openUri(info.releaseUrl) },
                modifier = Modifier.fillMaxWidth().height((56 * scaleFactor).dp),
                shape = RoundedCornerShape((16 * scaleFactor).dp),
                colors = ButtonDefaults.buttonColors(containerColor = IosActiveBlue, contentColor = Color.White)
            ) {
                Text("Download Now", fontWeight = FontWeight.Bold, fontSize = (16 * scaleFactor).sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height((8 * scaleFactor).dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height((48 * scaleFactor).dp)
            ) {
                Text("Remind Me Later", color = Color.White, fontSize = (14 * scaleFactor).sp)
            }
        }
    }
}
