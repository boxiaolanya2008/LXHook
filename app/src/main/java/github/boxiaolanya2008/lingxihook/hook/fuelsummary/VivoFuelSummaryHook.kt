package github.boxiaolanya2008.lingxihook.hook.fuelsummary

import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.AppHooker
import github.boxiaolanya2008.lingxihook.hook.HookFeature
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * com.vivo.fuelsummary（电源信息）适配入口。
 * 只做分发，各功能独立成类；新增功能时在 install 里追加一行。
 */
class VivoFuelSummaryHook : AppHooker {

    override val packageName = "com.vivo.fuelsummary"
    override val label = "电源信息（vivo FuelSummary）"
    override val description =
        "vivo 电源/电量统计中枢（跑在 daemonService），接管充电限流、健康度、循环次数等 fuelgauge 策略"

    override val features = listOf(
        HookFeature(
            key = FEATURE_CHARGING,
            title = "移除充电限流",
            description = "拦截 r0.f#K/M/H + h()/f0/F + 智能充电开关，强制超快充支持 true、智能限流 false、温控阈值 60℃，阻断高低温停充与 Bypass 限速，充电全程不降速（自担风险）。",
            defaultEnabled = true
        ),
        HookFeature(
            key = FEATURE_CAPACITY,
            title = "电池容量锁最大",
            description = "拦截 com.vivo.fuelsummary.battery.health.a#b/c + g#f/d + h#u/C 对 capacity_mah/soh 节点，健康度与容量始终返回 100/最大设计值，设置页不再显示衰减。",
            defaultEnabled = true
        ),
        HookFeature(
            key = FEATURE_CYCLE,
            title = "循环次数锁 5 次",
            description = "拦截 g#b + h#z/C/u 对 /sys/class/fuelsummary/cycle 与 fuelsummary soh 节点，循环次数恒为 5，上报与健康曲线均按 5 次计算。",
            defaultEnabled = true
        )
    )

    private val chargingHook = ChargingSpeedHook()
    private val capacityHook = BatteryCapacityHook()
    private val cycleHook = CycleCountHook()

    override fun install(module: XposedModule, param: PackageLoadedParam) {
        HookLogger.log(LogLevel.INFO, "fuel", "适配器已注入：${param.packageName}")
        chargingHook.install(module, param)
        capacityHook.install(module, param)
        cycleHook.install(module, param)
    }

    companion object {
        const val FEATURE_CHARGING = "lingxi_hook_fuel_charging_unlimit"
        const val FEATURE_CAPACITY = "lingxi_hook_fuel_capacity_max"
        const val FEATURE_CYCLE = "lingxi_hook_fuel_cycle_5"
    }
}
