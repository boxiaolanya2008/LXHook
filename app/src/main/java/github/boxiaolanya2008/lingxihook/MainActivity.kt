package github.boxiaolanya2008.lingxihook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import com.materialkolor.PaletteStyle
import github.boxiaolanya2008.lingxihook.data.AppPrefs
import github.boxiaolanya2008.lingxihook.ui.AppRoot
import github.boxiaolanya2008.lingxihook.ui.theme.ColorMode
import github.boxiaolanya2008.lingxihook.ui.theme.灵犀HookTheme
import github.boxiaolanya2008.lingxihook.update.UpdateChecker
import github.boxiaolanya2008.lingxihook.update.UpdateDialog
import github.boxiaolanya2008.lingxihook.util.RootUtil
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // ---- M3 Expressive 主题状态（设置页可改，持久化到内部存储）----
    private var colorMode by mutableIntStateOf(0)
    private var keyColor by mutableIntStateOf(0)
    private var paletteStyle by mutableStateOf("TonalSpot")
    private var updateInfo by mutableStateOf<github.boxiaolanya2008.lingxihook.update.UpdateInfo?>(null)
    private var isRooted by mutableStateOf<Boolean?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        colorMode = AppPrefs.colorMode(this)
        keyColor = AppPrefs.keyColor(this)
        paletteStyle = AppPrefs.paletteStyle(this)
        lifecycleScope.launch {
            isRooted = RootUtil.isRooted()
            val result = UpdateChecker.check(this@MainActivity)
            result.onSuccess { info ->
                if (info.isNewerThan(UpdateChecker.currentVersionCode(this@MainActivity)) && info.downloadUrl.isNotBlank()) {
                    updateInfo = info
                }
            }
        }

        setContent {
            灵犀HookTheme(
                colorMode = ColorMode.fromValue(colorMode),
                keyColor = keyColor,
                paletteStyle = runCatching { PaletteStyle.valueOf(paletteStyle) }
                    .getOrDefault(PaletteStyle.TonalSpot)
            ) {
                AppRoot(
                    colorMode = colorMode,
                    keyColor = keyColor,
                    paletteStyle = paletteStyle,
                    onColorModeChange = { mode ->
                        AppPrefs.setColorMode(this, mode)
                        colorMode = mode
                    },
                    onKeyColorChange = { value ->
                        AppPrefs.setKeyColor(this, value)
                        keyColor = value
                    },
                    onPaletteStyleChange = { style ->
                        AppPrefs.setPaletteStyle(this, style)
                        paletteStyle = style
                    }
                )
                updateInfo?.let { info ->
                    UpdateDialog(
                        info = info,
                        onUpdate = { UpdateChecker.openDownload(this, info.downloadUrl) },
                        onDismiss = { updateInfo = null }
                    )
                }
                if (isRooted == false) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("未检测到 Root 权限") },
                        text = { Text("本模块部分功能（机型伪装、屏蔽更新、高像素等）需 Root 才能完整生效。未 Root 时仅基础 Hook 可用，建议获取 Root 后重启应用。") },
                        confirmButton = {
                            TextButton(onClick = { isRooted = true }) { Text("继续使用") }
                        },
                        dismissButton = {
                            TextButton(onClick = { finishAffinity() }) { Text("退出应用") }
                        }
                    )
                }
            }
        }
    }
}