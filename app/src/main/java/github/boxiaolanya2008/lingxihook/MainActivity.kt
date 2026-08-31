package github.boxiaolanya2008.lingxihook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.materialkolor.PaletteStyle
import github.boxiaolanya2008.lingxihook.data.AppPrefs
import github.boxiaolanya2008.lingxihook.ui.AppRoot
import github.boxiaolanya2008.lingxihook.ui.theme.ColorMode
import github.boxiaolanya2008.lingxihook.ui.theme.灵犀HookTheme

class MainActivity : ComponentActivity() {

    // ---- M3 Expressive 主题状态（设置页可改，持久化到内部存储）----
    private var colorMode by mutableIntStateOf(0)
    private var keyColor by mutableIntStateOf(0)
    private var paletteStyle by mutableStateOf("TonalSpot")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        colorMode = AppPrefs.colorMode(this)
        keyColor = AppPrefs.keyColor(this)
        paletteStyle = AppPrefs.paletteStyle(this)

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
            }
        }
    }
}