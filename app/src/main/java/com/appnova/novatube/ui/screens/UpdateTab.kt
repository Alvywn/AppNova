package com.appnova.novatube.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appnova.novatube.BuildConfig
import com.appnova.novatube.ui.components.GlassCard
import com.appnova.novatube.ui.components.SectionHeader
import com.appnova.novatube.ui.theme.LocalNovaThemeStyle
import com.appnova.novatube.ui.theme.NovaColors
import com.appnova.novatube.updater.GitHubUpdater
import com.appnova.novatube.updater.UpdateInfo
import kotlinx.coroutines.launch

@Composable
fun UpdateTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val theme = LocalNovaThemeStyle.current
    val accentColor = Color(theme.accentHex)

    var updateStatus by remember { mutableStateOf<String?>(null) }
    var availableUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            SectionHeader(
                title = "NovaTube Updates",
                subtitle = "Cek dan install versi terbaru langsung dari GitHub Releases (Alvywn/AppNova)"
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Versi Terinstall: v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                color = NovaColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            updateStatus?.let { status ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = status,
                    color = NovaColors.TextSecondary,
                    fontSize = 12.sp
                )
            }

            availableUpdate?.let { info ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Catatan Rilis (${info.versionName}):",
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = info.changelog,
                    color = NovaColors.TextPrimary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            if (isDownloading) {
                LinearProgressIndicator(
                    progress = { downloadProgress / 100f },
                    color = accentColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                )
                Text(
                    text = "Mengunduh APK... $downloadProgress%",
                    color = Color.White,
                    fontSize = 12.sp
                )
            } else if (availableUpdate != null) {
                val info = availableUpdate!!
                Button(
                    onClick = {
                        isDownloading = true
                        downloadProgress = 0
                        scope.launch {
                            val file = GitHubUpdater.downloadApk(context, info.downloadUrl) { pct ->
                                downloadProgress = pct
                            }
                            isDownloading = false
                            if (file != null) {
                                GitHubUpdater.installApk(context, file)
                            } else {
                                updateStatus = "Gagal mengunduh file APK."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.SystemUpdate, contentDescription = null)
                    Text("  Download & Install ${info.versionName}", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    enabled = !isChecking,
                    onClick = {
                        isChecking = true
                        updateStatus = "Mengecek versi terbaru di GitHub..."
                        scope.launch {
                            val info = GitHubUpdater.checkForUpdate(BuildConfig.VERSION_NAME)
                            isChecking = false
                            availableUpdate = info
                            updateStatus = if (info != null) {
                                "Update baru tersedia: ${info.versionName}"
                            } else {
                                "Kamu sudah menggunakan versi terbaru (v${BuildConfig.VERSION_NAME})."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor.copy(alpha = 0.22f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = accentColor)
                    Text(
                        if (isChecking) "  Mengecek..." else "  Cek Update Sekarang",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
