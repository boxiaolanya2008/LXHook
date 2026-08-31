package github.boxiaolanya2008.lingxihook.hook.camera

import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.AppHooker
import github.boxiaolanya2008.lingxihook.hook.HookFeature
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * com.android.camera（vivo / iQOO 相机）适配入口。
 * 只做分发，各功能独立成类；新增功能时在 install 里追加一行。
 */
class VivoCameraHook : AppHooker {

    override val packageName = "com.android.camera"
    override val label = "相机（vivo / iQOO）"
    override val description =
        "vivo / iQOO 相机主程序，含水印、滤镜、人像等拍摄能力"

    override val features = listOf(
        HookFeature(
            key = FEATURE_ZEISS,
            title = "ZEISS 水印解锁",
            description = "强制放行蔡司联名判定（isCameraSignedByZeiss / isSupportWatermarkZEISS / isSupportWatermarkBorder / isSupportZeissColor），" +
                "并补齐旗舰级水印模板（BORDER / MASTER / FEATURE / CHINOISERIES）且伪装机型为 vivo X500 BETA（参数对齐 vivo X300 Pro），" +
                "使 iQOO 机型可在水印设置中选用 vivo X 系列同款 ZEISS 边框与大师签名水印，成片 EXIF 与水印落款均显示 ZEISS 联名。",
            defaultEnabled = true
        )
    )

    private val zeissWatermarkHook = ZeissWatermarkHook()

    override fun install(module: XposedModule, param: PackageLoadedParam) {
        HookLogger.log(LogLevel.INFO, "camera", "适配器已注入：${param.packageName}")
        zeissWatermarkHook.install(module, param)
    }

    companion object {
        /** ZEISS 水印解锁开关持久化键 */
        const val FEATURE_ZEISS = "lingxi_hook_camera_zeiss"
    }
}
