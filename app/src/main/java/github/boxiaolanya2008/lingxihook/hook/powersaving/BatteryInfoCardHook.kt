package github.boxiaolanya2008.lingxihook.hook.powersaving

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import github.boxiaolanya2008.lingxihook.data.LogLevel
import github.boxiaolanya2008.lingxihook.hook.HookConfig
import github.boxiaolanya2008.lingxihook.hook.HookLogger
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import java.io.File

/**
 * 耗电排行页电池详情卡（Hook 点来自 jadx 反编译 com.iqoo.powersaving 源码，非猜测）。
 *
 * 注入点：`com.iqoo.powersaving.PowerRankActivity#H0(int, int)`（电量更新回调，
 * 内部 `findViewById(a1.i.f40a=battery_percent_content)` 即“当前电量”行），
 * 在其父容器中“当前电量”之上插入一张自绘卡片；H0 每次电量变化都会进，
 * 卡片已存在时只刷新数值（幂等，tag 守卫）。
 *
 * 卡片内容：大电量 + 状态行（充/放电 + 预估可用时长）+ 六宫格
 * （温度/电压/电流/健康度/满充容量/循环次数）+ [重置使用统计] 按钮。
 *
 * 数据来源（全部目标进程内直取，无跨进程）：
 * - 电量/温度/电压/状态：`ACTION_BATTERY_CHANGED` 粘性广播，免权限。
 * - 电流：`BatteryManager.getIntProperty(CURRENT_NOW)`，免权限。
 * - 健康度/容量/循环：轮询 sysfs（charge_full/design、cycle_count 等），读不到显示 —。
 * - 预估可用：本卡持久化的电量采样（目标应用私有 SP）算近期掉电速率，
 *   样本不足显示采集中；数值每 5 秒 tick 刷新一次。
 */
class BatteryInfoCardHook {

    fun install(module: XposedModule, param: PackageLoadedParam) {
        val loader = param.defaultClassLoader
        val activityClass = runCatching {
            Class.forName(CLASS_RANK_ACTIVITY, false, loader)
        }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "$CLASS_RANK_ACTIVITY not found: $it")
            return
        }
        // H0 只在电量变化时回调，电量不动卡片永远没机会插；
        // 加两个常开触发点，插入幂等（tag 守卫），三处任一命中即生效。
        hookBatteryCallback(module, activityClass)
        hookInitDone(module, activityClass)
        hookResume(module, activityClass)
    }

    /** 原触发点：PowerRankActivity#H0(int, int)，同时负责刷新数值 */
    private fun hookBatteryCallback(module: XposedModule, activityClass: Class<*>) {
        val onBattery = activityClass.declaredMethods.firstOrNull {
            it.name == METHOD_BATTERY && it.parameterTypes.size == 2 &&
                it.parameterTypes.all { p -> p == Int::class.javaPrimitiveType }
        } ?: run {
            HookLogger.log(LogLevel.WARN, TAG, "${activityClass.name}#$METHOD_BATTERY(int,int) not found")
            return
        }
        runCatching {
            module.hook(onBattery)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    chain.proceed()
                    if (!HookConfig.isEnabled(IqooPowerSavingHook.FEATURE_BATTERY_CARD, true)) return@intercept null
                    val activity = chain.thisObject as? Activity ?: return@intercept null
                    runCatching { refreshCard(activity) }.onFailure {
                        HookLogger.log(LogLevel.WARN, TAG, "refresh card failed: $it", persist = false)
                    }
                    null
                }
            HookLogger.log(LogLevel.INFO, TAG, "hooked ${activityClass.name}#$METHOD_BATTERY -> card")
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "hook ${activityClass.name}#$METHOD_BATTERY failed: $it")
        }
    }

    /**
     * 初始化完成触发点：BasePowerRankActivity#I(*Toolbar)，此时 ViewStub（含“当前电量”）
     * 已经 inflate。按名 + 单参 + 类型名含 Toolbar 查找，不依赖混淆类名。
     */
    private fun hookInitDone(module: XposedModule, activityClass: Class<*>) {
        var clazz: Class<*>? = activityClass
        var initMethod: java.lang.reflect.Method? = null
        while (clazz != null && clazz != Any::class.java && initMethod == null) {
            initMethod = clazz.declaredMethods.firstOrNull {
                it.name == METHOD_INIT && it.parameterTypes.size == 1 &&
                    it.parameterTypes[0].name.contains("Toolbar")
            }
            clazz = clazz.superclass
        }
        if (initMethod == null) {
            HookLogger.log(LogLevel.WARN, TAG, "init method $METHOD_INIT(*Toolbar) not found")
            return
        }
        runCatching {
            module.hook(initMethod)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    chain.proceed()
                    if (!HookConfig.isEnabled(IqooPowerSavingHook.FEATURE_BATTERY_CARD, true)) return@intercept null
                    val activity = chain.thisObject as? Activity ?: return@intercept null
                    runCatching { refreshCard(activity) }.onFailure {
                        HookLogger.log(LogLevel.WARN, TAG, "refresh card failed: $it", persist = false)
                    }
                    null
                }
            HookLogger.log(LogLevel.INFO, TAG, "hooked ${initMethod.declaringClass.name}#${initMethod.name} -> card")
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "hook init failed: $it")
        }
    }

    /**
     * 常开触发点：进程内所有 Activity.onResume，类名命中排行页才插卡。
     * 覆盖 H0 长期不回调、I() 签名漂移等全部情况。
     */
    private fun hookResume(module: XposedModule, activityClass: Class<*>) {
        val onResume = runCatching {
            Class.forName("android.app.Activity").getDeclaredMethod("onResume")
        }.getOrElse {
            HookLogger.log(LogLevel.WARN, TAG, "Activity.onResume not found: $it")
            return
        }
        runCatching {
            module.hook(onResume)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    chain.proceed()
                    val activity = chain.thisObject as? Activity ?: return@intercept null
                    if (activity.javaClass.name != activityClass.name) return@intercept null
                    if (!HookConfig.isEnabled(IqooPowerSavingHook.FEATURE_BATTERY_CARD, true)) return@intercept null
                    runCatching { refreshCard(activity) }.onFailure {
                        HookLogger.log(LogLevel.WARN, TAG, "refresh card failed: $it", persist = false)
                    }
                    null
                }
            HookLogger.log(LogLevel.INFO, TAG, "hooked Activity.onResume guard=${activityClass.name}")
        }.onFailure {
            HookLogger.log(LogLevel.WARN, TAG, "hook onResume failed: $it")
        }
    }

    private data class Holder(
        val levelBig: TextView,
        val statusLine: TextView,
        val values: List<TextView>
    )

    private fun refreshCard(activity: Activity) {
        activity.runOnUiThread {
            // 已注入且仍在树上：只刷新数值，避免每次全树查找
            val live = currentCard?.takeIf { it.parent != null }
            if (live != null) {
                currentHolder?.let { updateValues(activity, it) }
                return@runOnUiThread
            }
            currentCard = null
            currentHolder = null
            val anchor = findAnchor(activity)
            if (anchor == null) {
                HookLogger.log(LogLevel.WARN, TAG, "anchor battery row not found, skip")
                return@runOnUiThread
            }
            val parent = anchor.parent as? ViewGroup
            if (parent == null) {
                HookLogger.log(LogLevel.WARN, TAG, "anchor has no parent, skip")
                return@runOnUiThread
            }
            val card = buildCard(activity)
            // 字体与系统行同款：从锚点行里抓第一只 TextView 的 Typeface（OriginOS 定制字体），
            // 全套应用到卡片所有文字，写死字体永远对不上版本。
            runCatching {
                findFirstTextView(anchor)?.typeface?.let { face -> applyTypeface(card, face) }
            }
            // 外框与锚点行同款：克隆锚点 LayoutParams（同类 + 同边距），宽高按卡片重写，
            // 写死 dp 在不同版本/主题下永远对不齐系统卡。
            card.layoutParams = cloneRowLayoutParams(parent, anchor)
            parent.addView(card, parent.indexOfChild(anchor))
            currentCard = card
            val bodyBgNull = runCatching {
                val body = (card as ViewGroup).getChildAt(0)
                val bgNull = body.background == null
                HookLogger.log(
                    LogLevel.INFO, TAG,
                    "battery card injected (parent=${parent.javaClass.name}, bodyBgNull=$bgNull)"
                )
                bgNull
            }.getOrDefault(true)
            if (bodyBgNull) {
                HookLogger.log(LogLevel.WARN, TAG, "inner body background missing!")
            }
            currentHolder?.let {
                updateValues(activity, it)
                startTick(activity, card, it)
            }
        }
    }

    /**
     * 锚点“三级跳”（版本漂移也不怕）：
     * 1. 按资源名 `battery_percent_content` 找（反编译版一致时命中）；
     * 2. 写死的 a1.i.f40a 值回退；
     * 3. 按可见文案“当前电量”全树找 TextView，沿父链上走到 VListContent 行。
     */
    private fun findAnchor(activity: Activity): View? {
        findCurrentBatteryRow(activity)?.let { return it }
        return findRowByText(activity)
    }

    /** “当前电量”行：优先按资源名找（抗混淆），回退写死的 a1.i.f40a 值 */
    private fun findCurrentBatteryRow(activity: Activity): View? {
        val byName = runCatching {
            val id = activity.resources.getIdentifier(ID_BATTERY_ROW, "id", activity.packageName)
            if (id != 0) activity.findViewById<View>(id) else null
        }.getOrNull()
        if (byName != null) return byName
        return runCatching { activity.findViewById<View>(ID_BATTERY_ROW_FALLBACK) }.getOrNull()
    }

    /**
     * 克隆锚点行的 LayoutParams：同类构造器拷贝（边距/行为全保留），只重写宽高；
     * 锚点行自身常无外边距（间距靠父容器），上下边距强制保底 8dp，免得两卡粘连；
     * 拷贝失败回退默认边距，保证 addView 不崩。
     */
    private fun cloneRowLayoutParams(parent: ViewGroup, anchor: View): ViewGroup.LayoutParams {
        val minGap = (8 * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
        val fallback = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = minGap
            bottomMargin = minGap
        }
        val src = anchor.layoutParams ?: return fallback
        return runCatching {
            val copy = src.javaClass.getConstructor(ViewGroup.LayoutParams::class.java).newInstance(src)
            copy.width = ViewGroup.LayoutParams.MATCH_PARENT
            copy.height = ViewGroup.LayoutParams.WRAP_CONTENT
            (copy as? ViewGroup.MarginLayoutParams)?.let {
                if (it.topMargin < minGap) it.topMargin = minGap
                if (it.bottomMargin < minGap) it.bottomMargin = minGap
            }
            copy as ViewGroup.LayoutParams
        }.getOrElse {
            (src as? ViewGroup.MarginLayoutParams)?.let {
                fallback.leftMargin = it.leftMargin
                fallback.rightMargin = it.rightMargin
                fallback.topMargin = maxOf(it.topMargin, minGap)
                fallback.bottomMargin = maxOf(it.bottomMargin, minGap)
            }
            fallback
        }
    }

    /** 按可见文案兜底：找文字为“当前电量”的 TextView，沿父链上走到 VListContent 行 */
    private fun findRowByText(activity: Activity): View? {
        val root = activity.window?.decorView as? ViewGroup ?: return null
        val tv = findTextView(root, TEXT_CURRENT_BATTERY) ?: return null
        var node: View = tv
        repeat(5) {
            val p = node.parent as? View ?: return node
            if (p === root) return node
            if (p.javaClass.name.contains("VListContent")) return p
            node = p
        }
        return node
    }

    /** 锚点行内第一只 TextView（取它的 Typeface 用） */
    private fun findFirstTextView(anchor: View): TextView? {
        if (anchor is TextView) return anchor
        if (anchor !is ViewGroup) return null
        val stack = ArrayDeque<View>()
        stack.add(anchor)
        var steps = 0
        while (stack.isNotEmpty() && steps < 200) {
            val v = stack.removeLast()
            steps++
            if (v is TextView) return v
            if (v is ViewGroup) {
                for (i in v.childCount - 1 downTo 0) {
                    v.getChildAt(i)?.let { stack.add(it) }
                }
            }
        }
        return null
    }

    private fun applyTypeface(root: View, face: android.graphics.Typeface) {
        if (root is TextView) {
            runCatching { root.typeface = face }
            return
        }
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                root.getChildAt(i)?.let { applyTypeface(it, face) }
            }
        }
    }

    private fun findTextView(root: ViewGroup, text: String): TextView? {        val stack = ArrayDeque<View>()
        stack.add(root)
        var steps = 0
        while (stack.isNotEmpty() && steps < 4000) {
            val v = stack.removeLast()
            steps++
            if (v is TextView && v.text?.toString() == text) return v
            if (v is ViewGroup) {
                for (i in v.childCount - 1 downTo 0) {
                    v.getChildAt(i)?.let { stack.add(it) }
                }
            }
        }
        return null
    }

    private fun isDark(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    private fun dp(context: Context, v: Int): Int = (v * context.resources.displayMetrics.density).toInt()

    private fun buildCard(activity: Activity): LinearLayout {
        val dark = isDark(activity)
        val textMain = if (dark) 0xFFF5F5F7.toInt() else 0xFF1D1D1F.toInt()
        val textSub = if (dark) 0xFFAEAEB2.toInt() else 0xFF8A8A8E.toInt()
        val cardBg = if (dark) 0xFF232328.toInt() else 0xFFFFFFFF.toInt()
        // 双层容器：宿主列表容器会剥直接子 View 的背景，外层只管占位边距，
        // 内层管圆角底 + 内边距 + 全部内容，背景被剥也只影响外层。
        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            tag = CARD_TAG
            val m = dp(context, 12)
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = m
                rightMargin = m
                topMargin = dp(context, 8)
                bottomMargin = dp(context, 8)
            }
        }
        val body = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(context, 16).toFloat()
                setColor(cardBg)
            }
            setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        card.addView(body)
        val title = TextView(activity).apply {
            text = "电池详情 · 灵犀"
            setTextColor(textSub)
            textSize = 13f
        }
        val levelBig = TextView(activity).apply {
            text = "--%"
            setTextColor(textMain)
            textSize = 30f
        }
        val statusLine = TextView(activity).apply {
            text = "采集中…"
            setTextColor(textSub)
            textSize = 13f
        }
        val grid = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        val titles = arrayOf("温度", "电压", "电流", "健康度", "满充容量", "循环次数")
        val values = mutableListOf<TextView>()
        for (row in 0 until 2) {
            val line = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(context, 8), 0, 0)
            }
            for (col in 0 until 3) {
                val cell = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                val label = TextView(activity).apply {
                    text = titles[row * 3 + col]
                    setTextColor(textSub)
                    textSize = 12f
                }
                val value = TextView(activity).apply {
                    text = "—"
                    setTextColor(textMain)
                    textSize = 15f
                    setPadding(0, dp(context, 2), 0, 0)
                }
                values += value
                cell.addView(label)
                cell.addView(value)
                line.addView(cell)
            }
            grid.addView(line)
        }
        body.addView(title)
        body.addView(levelBig)
        body.addView(statusLine)
        body.addView(grid)
        currentHolder = Holder(levelBig, statusLine, values)
        return card
    }

    /** 已注入卡片与数值持有者：进程内单页，直接字段缓存，不用 setTag（key 须是资源 id） */
    @Volatile
    private var currentCard: View? = null

    @Volatile
    private var currentHolder: Holder? = null

    @Volatile
    private var tickRunnable: Runnable? = null

    private fun updateValues(activity: Activity, holder: Holder) {
        val battery = activity.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.let {
            val raw = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (raw >= 0 && scale > 0) raw * 100 / scale else -1
        } ?: -1
        val temp = battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            ?.takeIf { it != Int.MIN_VALUE }?.let { "%.1f℃".format(it / 10.0) } ?: "—"
        val volt = battery?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            ?.takeIf { it > 0 }?.let { "%.2fV".format(it / 1000.0) } ?: "—"
        val bm = activity.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val nowUa = runCatching { bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) }.getOrNull()
        val current = if (nowUa != null && nowUa != Int.MIN_VALUE && nowUa != 0) {
            "%d mA".format(kotlin.math.abs(nowUa / 1000))
        } else "—"
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val statusText = when {
            status == BatteryManager.BATTERY_STATUS_CHARGING -> when (plugged) {
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "充电中 · 无线"
                BatteryManager.BATTERY_PLUGGED_USB -> "充电中 · USB"
                else -> "充电中 · 有线"
            }
            status == BatteryManager.BATTERY_STATUS_FULL -> "已充满"
            status == BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
            else -> "—"
        }
        val health = readHealth()
        val estimate = estimateRemaining(activity, level, status == BatteryManager.BATTERY_STATUS_CHARGING)
        holder.levelBig.text = if (level >= 0) "$level%" else "--%"
        holder.statusLine.text = if (estimate != null) "$statusText · 预计可用$estimate" else statusText
        val vals = listOf(temp, volt, current, health.first, health.second, health.third)
        holder.values.forEachIndexed { i, tv -> tv.text = vals.getOrElse(i) { "—" } }
    }

    /** 健康度/满充容量/循环次数：轮询常见 sysfs，读不到显示 — */
    private fun readHealth(): Triple<String, String, String> {
        fun readFirst(vararg paths: String): Long? {
            for (p in paths) {
                val v = runCatching { File(p).takeIf { it.canRead() }?.readText()?.trim()?.toLongOrNull() }.getOrNull()
                if (v != null) return v
            }
            return null
        }
        val full = readFirst(
            "/sys/class/power_supply/battery/charge_full",
            "/sys/class/power_supply/bms/charge_full",
            "/sys/class/power_supply/battery/charge_full_design"
        )
        val design = readFirst(
            "/sys/class/power_supply/battery/charge_full_design",
            "/sys/class/power_supply/bms/charge_full_design"
        )
        val health = if (full != null && design != null && design > 0 && full != design) {
            "${(full * 100 / design).coerceIn(0, 100)}%"
        } else "—"
        val capacity = if (full != null && full > 10000) "${full / 1000} mAh" else "—"
        val cycles = readFirst(
            "/sys/class/power_supply/battery/cycle_count",
            "/sys/class/power_supply/bms/cycle_count",
            "/sys/class/power_supply/battery/battery_cycle_count"
        )?.let { "$it 次" } ?: "—"
        return Triple(health, capacity, cycles)
    }

    /**
     * 预估可用时长：目标应用私有 SP 存（时间,电量,是否充电）采样，
     * 取 12h 内放电样本算 %/h 速率；样本不足或充电中返回 null。
     */
    private fun estimateRemaining(context: Context, level: Int, charging: Boolean): String? {
        if (charging || level < 0) return null
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val raw = sp.getString(KEY_SAMPLES, "").orEmpty()
        val samples = raw.split(";").mapNotNull {
            val p = it.split(",")
            if (p.size == 3) Triple(p[0].toLongOrNull(), p[1].toIntOrNull(), p[2] == "1") else null
        }.filter { it.first != null && it.second != null && now - it.first!! < 12 * 3600_000L } +
            Triple(now, level, false)
        sp.edit().putString(KEY_SAMPLES, samples.takeLast(48).joinToString(";") { "${it.first},${it.second},${if (it.third) 1 else 0}" }).apply()
        val discharge = samples.filter { it.third == false }
        if (discharge.size < 2) return null
        val first = discharge.first()
        val last = discharge.last()
        val dtH = (last.first!! - first.first!!) / 3600000.0
        val dLevel = (first.second!! - last.second!!).toDouble()
        if (dtH < 0.5 || dLevel < 1.0) return null
        val remainH = level / (dLevel / dtH)
        if (remainH.isNaN() || remainH > 72) return null
        val h = remainH.toInt()
        val m = ((remainH - h) * 60).toInt()
        return if (h > 0) "${h}小时${m}分钟" else "${m}分钟"
    }

    /**
     * 5 秒定时刷新：卡片数值（温度/电压/电流等）平时只在 H0 电量变化时刷，
     * 加 tick 保证静置也每 5 秒刷新一次；页面 detached 或 Activity 销毁即停，不泄漏。
     */
    private fun startTick(activity: Activity, card: View, holder: Holder) {
        stopTick(card)
        val tick = object : Runnable {
            override fun run() {
                val alive = card.parent != null &&
                    !(activity.isFinishing || activity.isDestroyed)
                if (!alive) return
                runCatching { updateValues(activity, holder) }
                card.postDelayed(this, TICK_MS)
            }
        }
        tickRunnable = tick
        card.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                v.removeCallbacks(tick)
                v.removeOnAttachStateChangeListener(this)
            }
        })
        card.postDelayed(tick, TICK_MS)
    }

    private fun stopTick(card: View) {
        tickRunnable?.let { card.removeCallbacks(it) }
        tickRunnable = null
    }

    private companion object {
        const val TAG = "batterycard"
        const val CLASS_RANK_ACTIVITY = "com.iqoo.powersaving.PowerRankActivity"
        const val METHOD_BATTERY = "H0"
        const val METHOD_INIT = "I"
        const val CARD_TAG = "lingxi_battery_card"
        const val ID_BATTERY_ROW = "battery_percent_content"
        /** 可见文案兜底（版本 id 漂移时用） */
        const val TEXT_CURRENT_BATTERY = "当前电量"
        /** a1.i.f40a 写死值，getIdentifier 失败时回退 */
        const val ID_BATTERY_ROW_FALLBACK = 2131230871
        const val PREFS = "lingxi_battery_card"
        const val KEY_SAMPLES = "samples"
        /** 数值刷新间隔 */
        const val TICK_MS = 5000L
    }
}
