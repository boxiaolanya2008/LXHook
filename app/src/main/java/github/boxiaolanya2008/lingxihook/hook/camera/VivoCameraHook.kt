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
        ),
        HookFeature(
            key = FEATURE_ICONS,
            title = "水印图标全显",
            description = "清空所有 WMTemplate.WMItem / RelatedWMItem 的 unShowList（原按 isSupportGoldMaterial / isSupportShowWatermarkIcon 过滤），" +
                "并把 isSupportShowWatermarkIcon 强制 true、IQOO 边框版本抬至 2（threecolor_logo/iqoo_logo/kpl_logo 全量），" +
                "使水印编辑页的图标选择器一次性展示全部官方图标（含 ZEISS / vivo / 赛事联名等），不再按机型阉割。",
            defaultEnabled = true
        ),
        HookFeature(
            key = FEATURE_CAMPUS,
            title = "校园水印修复",
            description = "拦截 ISettingManager#getSettingValueFromKey(pref_camera_watermark_graduate_school) 空值回退为 “浙江大学”，" +
                "并在 oi/f#beforeOnItemClick 中对 GRADUATE_SCHOOL 模板自动写入默认学校，绕过空学校弹窗，使校园水印（华中科大/浙大）选择后可直接出片并正常落盘边框、校徽与口号。",
            defaultEnabled = true
        )
    )

    private val zeissWatermarkHook = ZeissWatermarkHook()
    private val watermarkIconHook = WatermarkIconHook()
    private val campusWatermarkHook = CampusWatermarkHook()

    override fun install(module: XposedModule, param: PackageLoadedParam) {
        HookLogger.log(LogLevel.INFO, "camera", "适配器已注入：${param.packageName}")
        zeissWatermarkHook.install(module, param)
        watermarkIconHook.install(module, param)
        campusWatermarkHook.install(module, param)
    }

    companion object {
        /** ZEISS 水印解锁开关持久化键 */
        const val FEATURE_ZEISS = "lingxi_hook_camera_zeiss"
        /** 水印图标全显开关持久化键 */
        const val FEATURE_ICONS = "lingxi_hook_camera_icons"
        /** 校园水印修复开关持久化键 */
        const val FEATURE_CAMPUS = "lingxi_hook_camera_campus"
    }
}
