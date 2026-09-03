package github.boxiaolanya2008.lingxihook.hook.gamecube

import android.content.Context
import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.HookConfig
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

/**
 * Q 芯片专区超分/插帧入口与全部挡位强制开启（Hook 点来自 jadx 反编译 com.vivo.gamecube 14.0.15，非猜测）。
 *
 * 三道坎（按拦截点分列）：
 * 1. 功能白名单总闸 e0#h0(ConfiguredFunction,String)：
 *    - 插帧条目 e0.p0 → hb.d#p → h0(BOOST_FRAME/OPTIMIZE_POWER, pkg)
 *    - Q 区挡位 hb.i#f/g → frameinterpolation.k#t/u → h0(BOOST_FRAME_V3 与 OPTIMIZE_POWER_V3 系列, pkg)
 *    - 超分条目 e0.a1 → h0(SUPER_RESOLUTION/SUPER_HD_ENGINE, pkg)
 *    数据库行已覆盖 per-game 维度，但设备级 hardware_dimen 白名单
 *    无 PD2520 行（boost_frame_v3_60_48_144 仅 PD2243/PD2254），机型维度缺口在此补。
 * 2. MEMC 硬件特性 e0#p0(String)：插帧条目第一环 X0()=FtFeature.isFeatureSupport
 *    ("vivo.hardware.game.memc")，V2520A 无该特性恒 false，导致 Q 区只渲染超分视图
 *    （CommonSuperResolutionFrameView f23673r=1），插帧视图整个不挂。
 *    放行前用 frameinterpolation.k#m(pkg) 判非空（game_dimen 存在该游戏 V3 插帧行）——
 *    QSuperFrameView 构造对 k.m 结果强解引用，无行放行会 NPE 崩 assistantui（侧滑“自动关闭”）。
 * 3. 超高分辨率挡 com.vivo.common.a#z0()：QSuperResolutionView/SuperResolutionView 的
 *    layout_ultra 显示条件为 (z0() 或 MonsterPlus) 且 n0(pkg)，z0()="vivo.hardware.game.supersr"
 *    特性等于 "nova"，非旗舰恒 false → 超高挡被藏。强制 z0=true 放出全部挡位。
 *
 * 关闭开关时全部走原逻辑；所有拦截均为 PROTECTIVE 容错。
 */
class QZoneGateHook {

    fun install(module: XposedModule, param: PackageLoadedParam) {
        val loader = param.defaultClassLoader
        var hooked = 0
        hooked += hookGate(module, loader)
        hooked += hookFrameEntry(module, loader)
        hooked += hookUltraResSupport(module, loader)
        hooked += hookUltraResEntry(module, loader)
        hooked += hookResolutionTipSkip(module, loader)
        hooked += hookFrameRowFallback(module, loader)
        hooked += hookLegacyFrameView(module, loader)
        if (hooked == 0) {
            HookLogger.log(LogLevel.WARN, TAG, "no qzone methods hooked, maybe obfuscation changed")
        } else {
            HookLogger.log(LogLevel.INFO, TAG, "installed ($hooked hooks) qzone entries unlocked")
        }
    }

    /**
     * 坎 7：极致挡"请调高系统超分等级"提示 l.l2(Context) 强制 true。
     * l2 检测系统显示分辨率模式（当前屏宽==1080 即未开高分辨率 → 恒提示），
     * 与游戏内渲染精度无关；V2520A 无分辨率切换选项时提示永远消不掉，直接放行。
     * 注意：这只去掉提示文案，极致挡实际渲染仍需系统图形栈配合。
     */
    private fun hookResolutionTipSkip(module: XposedModule, loader: ClassLoader): Int {
        val comm = runCatching { Class.forName(CLASS_COMM_UTILS, false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_COMM_UTILS not found: $it")
            return 0
        }
        val l2 = comm.declaredMethods.firstOrNull {
            it.name == METHOD_RES_TIP && it.parameterTypes.size == 1 &&
                it.parameterTypes[0] == Context::class.java
        } ?: run {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_COMM_UTILS#$METHOD_RES_TIP(Context) not found")
            return 0
        }
        return runCatching {
            module.hook(l2)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    if (!HookConfig.isEnabled(VivoGameCubeHook.FEATURE_QZONE, true)) return@intercept chain.proceed()
                    HookLogger.log(LogLevel.INFO, TAG, "$METHOD_RES_TIP -> true（极致挡分辨率提示去除）", persist = false)
                    true
                }
            HookLogger.log(LogLevel.INFO, TAG, "hooked $CLASS_COMM_UTILS#$METHOD_RES_TIP -> true")
            1
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "hook $CLASS_COMM_UTILS#$METHOD_RES_TIP failed: $it")
        }.getOrDefault(0)
    }

    /**
     * 坎 6：极致分辨率挡的游戏级判定 e0#n0(String) 强制 true。
     * layout_ultra 显示条件 = (z0() 或 MonsterPlus) 且 n0(pkg)；z0 已放行，
     * 而 n0 查 da.f.f30298a（super_resolution_support_game 内存列表），该列表仅在
     * G1()=aisr 特性=="nova" 时加载，非 nova 机恒空 → n0 恒 false，极致挡被藏。
     * 调用方：QSuperResolutionView/SuperResolutionView 的挡位可见性与刷新。
     */
    private fun hookUltraResEntry(module: XposedModule, loader: ClassLoader): Int {
        val utils = runCatching { Class.forName(CLASS_UTILS, false, loader) }.getOrElse { return 0 }
        val n0 = utils.declaredMethods.firstOrNull {
            it.name == METHOD_ULTRA_ENTRY && it.parameterTypes.size == 1 &&
                it.parameterTypes[0] == String::class.java
        } ?: run {
            HookLogger.log(LogLevel.WARN, TAG, "${utils.name}#$METHOD_ULTRA_ENTRY(String) not found")
            return 0
        }
        return runCatching {
            module.hook(n0)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    if (!HookConfig.isEnabled(VivoGameCubeHook.FEATURE_QZONE, true)) return@intercept chain.proceed()
                    val pkg = chain.args.getOrNull(0) as? String
                    HookLogger.log(LogLevel.INFO, TAG, "$METHOD_ULTRA_ENTRY($pkg) -> true（极致分辨率挡放行）", persist = false)
                    true
                }
            HookLogger.log(LogLevel.INFO, TAG, "hooked ${utils.name}#$METHOD_ULTRA_ENTRY -> true")
            1
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "hook ${utils.name}#$METHOD_ULTRA_ENTRY failed: $it")
        }.getOrDefault(0)
    }

    /** 坎 1：功能白名单总闸，Q 区相关 funcName 强制 true */
    private fun hookGate(module: XposedModule, loader: ClassLoader): Int {
        val utils = runCatching { Class.forName(CLASS_UTILS, false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_UTILS not found: $it")
            return 0
        }
        val cfg = runCatching { Class.forName(CLASS_CONFIGURED_FUNCTION, false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_CONFIGURED_FUNCTION not found: $it")
            return 0
        }
        val gate = utils.declaredMethods.firstOrNull {
            it.name == METHOD_GATE && it.parameterTypes.size == 2 &&
                it.parameterTypes[0] == cfg && it.parameterTypes[1] == String::class.java
        } ?: run {
            HookLogger.log(LogLevel.WARN, TAG, "${utils.name}#$METHOD_GATE(ConfiguredFunction,String) not found")
            return 0
        }
        return runCatching {
            module.hook(gate)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    if (!HookConfig.isEnabled(VivoGameCubeHook.FEATURE_QZONE, true)) return@intercept chain.proceed()
                    val func = chain.args.getOrNull(0) ?: return@intercept chain.proceed()
                    val name = runCatching {
                        cfg.getMethod(METHOD_FUNC_NAME).invoke(func) as? String
                    }.getOrNull() ?: return@intercept chain.proceed()
                    if (isQZoneFunc(name)) {
                        HookLogger.log(LogLevel.INFO, TAG, "$METHOD_GATE($name) -> true（Q区判定放行）", persist = false)
                        return@intercept true
                    }
                    chain.proceed()
                }
            HookLogger.log(LogLevel.INFO, TAG, "hooked ${utils.name}#$METHOD_GATE -> Q区判定放行")
            1
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "hook ${utils.name}#$METHOD_GATE failed: $it")
        }.getOrDefault(0)
    }

    /** 坎 2：MEMC 特性缺失。p0 仅在该游戏存在 V3 插帧白名单行时放行（数据驱动，防构造 NPE） */
    private fun hookFrameEntry(module: XposedModule, loader: ClassLoader): Int {
        val utils = runCatching { Class.forName(CLASS_UTILS, false, loader) }.getOrElse { return 0 }
        // frameinterpolation.k#m(String)：查 game_dimen 中该游戏的 boost_frame_v3_*/optimize_power_v3_*
        // 行并构建 ga.b；查不到返回 null。QSuperFrameView 构造对结果强解引用（bVarC.e()），
        // null 会直接 NPE 崩掉 assistantui 进程（表现为侧滑点进去“自动关闭”），故放行前必须判非空。
        val frameUtils = runCatching { Class.forName(CLASS_FRAME_UTILS, false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_FRAME_UTILS not found: $it")
            return 0
        }
        val hasRow = runCatching {
            frameUtils.getDeclaredMethod(METHOD_HAS_ROW, String::class.java)
        }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_FRAME_UTILS#$METHOD_HAS_ROW(String) not found: $it")
            return 0
        }
        val p0 = utils.declaredMethods.firstOrNull {
            it.name == METHOD_FRAME_ENTRY && it.parameterTypes.size == 1 &&
                it.parameterTypes[0] == String::class.java
        } ?: run {
            HookLogger.log(LogLevel.WARN, TAG, "${utils.name}#$METHOD_FRAME_ENTRY(String) not found")
            return 0
        }
        return runCatching {
            module.hook(p0)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    if (!HookConfig.isEnabled(VivoGameCubeHook.FEATURE_QZONE, true)) return@intercept chain.proceed()
                    val pkg = chain.args.getOrNull(0) as? String ?: return@intercept chain.proceed()
                    val hasInterRow = runCatching { hasRow.invoke(null, pkg) }.getOrNull() != null
                    if (hasInterRow) {
                        HookLogger.log(LogLevel.INFO, TAG, "$METHOD_FRAME_ENTRY($pkg) -> true（插帧条目放行，补 MEMC 特性）", persist = false)
                        return@intercept true
                    }
                    // 无插帧白名单行的游戏维持原判定（false），否则视图构造必崩
                    chain.proceed()
                }
            HookLogger.log(LogLevel.INFO, TAG, "hooked ${utils.name}#$METHOD_FRAME_ENTRY -> 有行放行")
            1
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "hook ${utils.name}#$METHOD_FRAME_ENTRY failed: $it")
        }.getOrDefault(0)
    }

    /** 坎 3：supersr 特性缺失，超高分辨率挡 a#z0() 强制 true */
    private fun hookUltraResSupport(module: XposedModule, loader: ClassLoader): Int {
        val common = runCatching { Class.forName(CLASS_COMMON, false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_COMMON not found: $it")
            return 0
        }
        val z0 = common.declaredMethods.firstOrNull {
            it.name == METHOD_ULTRA_SUPPORT && it.parameterTypes.isEmpty()
        } ?: run {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_COMMON#$METHOD_ULTRA_SUPPORT() not found")
            return 0
        }
        return runCatching {
            module.hook(z0)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    if (!HookConfig.isEnabled(VivoGameCubeHook.FEATURE_QZONE, true)) return@intercept chain.proceed()
                    val origin = chain.proceed() as? Boolean
                    if (origin != true) {
                        HookLogger.log(LogLevel.INFO, TAG, "$METHOD_ULTRA_SUPPORT -> true（超高分辨率挡放行）", persist = false)
                    }
                    true
                }
            HookLogger.log(LogLevel.INFO, TAG, "hooked $CLASS_COMMON#$METHOD_ULTRA_SUPPORT -> true")
            1
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "hook $CLASS_COMMON#$METHOD_ULTRA_SUPPORT failed: $it")
        }.getOrDefault(0)
    }

    /**
     * 坎 4：k#m(String) 查不到该游戏的 V3 插帧行时兜底返回默认配置。
     * QSuperFrameView 构造对 k.m 结果强解引用（bVarC.e()），null 即 NPE 崩 assistantui。
     * 兜底值：boost_frame_v3_60_48_144 / white / channel=* / game_index=3 / 无 SDK，
     * 与编辑器写库的通用行一致；已写库的游戏 k.m 命中原行，不经过兜底。
     */
    private fun hookFrameRowFallback(module: XposedModule, loader: ClassLoader): Int {
        val frameUtils = runCatching { Class.forName(CLASS_FRAME_UTILS, false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_FRAME_UTILS not found: $it")
            return 0
        }
        val gaB = runCatching { Class.forName(CLASS_GAME_DIMEN, false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_GAME_DIMEN not found: $it")
            return 0
        }
        val m = frameUtils.declaredMethods.firstOrNull {
            it.name == METHOD_HAS_ROW && it.parameterTypes.size == 1 &&
                it.parameterTypes[0] == String::class.java
        } ?: run {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_FRAME_UTILS#$METHOD_HAS_ROW(String) not found")
            return 0
        }
        val ctor = gaB.declaredConstructors.firstOrNull { it.parameterTypes.size == 9 } ?: run {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_GAME_DIMEN 9 参构造器 not found")
            return 0
        }
        ctor.isAccessible = true
        return runCatching {
            module.hook(m)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    if (!HookConfig.isEnabled(VivoGameCubeHook.FEATURE_QZONE, true)) return@intercept chain.proceed()
                    val origin = chain.proceed()
                    if (origin != null) return@intercept origin
                    val pkg = chain.args.getOrNull(0) as? String ?: return@intercept origin
                    val fallback = ctor.newInstance(
                        "boost_frame_v3_60_48_144", "white", pkg, pkg, "*",
                        false, 3, false, -1
                    )
                    HookLogger.log(LogLevel.INFO, TAG, "$METHOD_HAS_ROW($pkg) null -> fallback ga.b（插帧视图防崩）", persist = false)
                    fallback
                }
            HookLogger.log(LogLevel.INFO, TAG, "hooked $CLASS_FRAME_UTILS#$METHOD_HAS_ROW -> null 兜底")
            1
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "hook $CLASS_FRAME_UTILS#$METHOD_HAS_ROW failed: $it")
        }.getOrDefault(0)
    }

    /**
     * 坎 5：实际运行路径 hb.j（FrameInterConfigList.xml 实体列表分支）的三处放行。
     * hb.c#b() 按机型选 hb.i/hb.j：PD2520 走 hb.j，其数据源 AssistantUIService.f21156h
     * 仅在 MEMC 特性机（X0()）加载，V2520A 恒空列表 →
     * - c(String)：s(pkg)==null 返回 null，QSuperFrameView 构造强解引用 bVarC.e() → NPE 崩；
     *   兜底返回默认 ga.b（game_index=3 / 无 SDK，与写库通用行一致）。
     * - f(String)/g(String)：极致帧率/省电帧率挡可见性，空列表恒 false → 强制 true。
     * 已写库或列表含该游戏时走原逻辑。
     */
    private fun hookLegacyFrameView(module: XposedModule, loader: ClassLoader): Int {
        val legacy = runCatching { Class.forName(CLASS_LEGACY, false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_LEGACY not found: $it")
            return 0
        }
        val gaB = runCatching { Class.forName(CLASS_GAME_DIMEN, false, loader) }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_GAME_DIMEN not found: $it")
            return 0
        }
        var count = 0

        // c(String)：null → 默认 ga.b，防构造 NPE
        val c = legacy.declaredMethods.firstOrNull {
            it.name == METHOD_LEGACY_CFG && it.parameterTypes.size == 1 &&
                it.parameterTypes[0] == String::class.java
        }
        if (c == null) {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_LEGACY#$METHOD_LEGACY_CFG(String) not found")
        } else {
            runCatching {
                module.hook(c)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept { chain ->
                        if (!HookConfig.isEnabled(VivoGameCubeHook.FEATURE_QZONE, true)) return@intercept chain.proceed()
                        val origin = chain.proceed()
                        if (origin != null) return@intercept origin
                        val pkg = chain.args.getOrNull(0) as? String ?: return@intercept origin
                        val fallback = legacyCtor(gaB, pkg)
                        HookLogger.log(LogLevel.INFO, TAG, "$METHOD_LEGACY_CFG($pkg) null -> fallback ga.b（插帧视图防崩）", persist = false)
                        fallback
                    }
                HookLogger.log(LogLevel.INFO, TAG, "hooked $CLASS_LEGACY#$METHOD_LEGACY_CFG -> null 兜底")
                count++
            }.onFailure {
                HookLogger.log(LogLevel.WARN, TAG, "hook $CLASS_LEGACY#$METHOD_LEGACY_CFG failed: $it")
            }
        }

        // f/g：极致帧率/省电帧率挡可见性，强制 true
        for ((name, desc) in listOf(
            METHOD_LEGACY_BOOST to "极致帧率挡",
            METHOD_LEGACY_OPTI to "省电帧率挡"
        )) {
            val f = legacy.declaredMethods.firstOrNull {
                it.name == name && it.parameterTypes.size == 1 && it.parameterTypes[0] == String::class.java
            } ?: continue
            runCatching {
                module.hook(f)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept { chain ->
                        if (!HookConfig.isEnabled(VivoGameCubeHook.FEATURE_QZONE, true)) return@intercept chain.proceed()
                        val pkg = chain.args.getOrNull(0) as? String
                        HookLogger.log(LogLevel.INFO, TAG, "$name($pkg) -> true（${desc}放行）", persist = false)
                        true
                    }
                HookLogger.log(LogLevel.INFO, TAG, "hooked $CLASS_LEGACY#$name -> true")
                count++
            }.onFailure {
                HookLogger.log(LogLevel.WARN, TAG, "hook $CLASS_LEGACY#$name failed: $it")
            }
        }
        return count
    }

    /** 反射构造默认 ga.b（9 参全参构造器） */
    private fun legacyCtor(gaB: Class<*>, pkg: String): Any? =
        gaB.declaredConstructors.firstOrNull { it.parameterTypes.size == 9 }?.let { ctor ->
            ctor.isAccessible = true
            ctor.newInstance("boost_frame_v3_60_48_144", "white", pkg, pkg, "*", false, 3, false, -1)
        }

    /** Q 区超分/插帧入口依赖的全部功能名（枚举 ConfiguredFunction 的 funcName） */
    private fun isQZoneFunc(name: String): Boolean =
        name == "boost_frame" || name.startsWith("boost_frame_v3") ||
            name == "optimize_power" || name.startsWith("optimize_power_v3") ||
            name == "super_resolution" || name == "game_super_hd_engine"

    private companion object {
        const val TAG = "qzone"
        const val CLASS_UTILS = "com.vivo.gameassistant.utils.e0"
        const val CLASS_CONFIGURED_FUNCTION = "com.vivo.common.supportlist.pojo.ConfiguredFunction"
        const val CLASS_COMMON = "com.vivo.common.a"
        const val CLASS_FRAME_UTILS = "com.vivo.gameassistant.frameinterpolation.k"
        const val CLASS_GAME_DIMEN = "ga.b"
        const val CLASS_LEGACY = "hb.j"
        const val CLASS_COMM_UTILS = "com.vivo.common.utils.l"
        const val METHOD_RES_TIP = "l2"
        const val METHOD_LEGACY_CFG = "c"
        const val METHOD_LEGACY_BOOST = "f"
        const val METHOD_LEGACY_OPTI = "g"
        const val METHOD_GATE = "h0"
        const val METHOD_FUNC_NAME = "getFuncName"
        const val METHOD_FRAME_ENTRY = "p0"
        const val METHOD_HAS_ROW = "m"
        const val METHOD_ULTRA_SUPPORT = "z0"
        const val METHOD_ULTRA_ENTRY = "n0"
    }
}
