# 以下是Ai编码规则

## 基础规则
> [!NOTE]
>
> 编码规范：（`禁止编写SlopCode`指：`死代码、没注释代码、变量名乱使用代码、语法使用不规范代码`）
>
> 注释规范：（`禁止规范化注释`指：`装饰符号注释（// === example ==、/* === example === */、/*--- example === */ 等等`）
>
> 文档规范：（`去Ai味` 指：`表达要具体、有细节、接地气。例如：不说"优化用户体验"，而说"把按钮从灰色改成蓝色，点击率提升了20%"；不说"团队协作很重要"，而说"我们每天10点站会同步进度"`）
>
> 指令遵守规范：（`遵守用户消息中的指令` 指：`优先以用户最新消息中的具体要求为准，包括但不限于格式约束、输出长度、语气风格、任务范围等，若用户指令间存在冲突，以最后一条为准并及时指出矛盾`）

### 润色规则

> [!NOTE]
>
> `消息润色` 指：`将用户原始消息中的模糊、口语化、碎片化表达，自动转换为清晰、专业、结构化的正式消息，保留原意不变

**1. 语义补全**
- 将省略的主语、宾语、谓词补充完整
- 将指代不明的"这个、那个、它"替换为具体对象
- 示例：`"改一下那个"` → `"请修改首页轮播图的自动播放间隔时间"`

**2. 口语转书面**
- 将"嗯、那个、就是说"等口头禅删除
- 将"搞一下、弄个"等口语替换为"执行、创建、配置"等
- 示例：`"帮我把这个弄好看点"` → `"请优化该页面的视觉设计，提升美观度"`

**3. 抽象转具体**
- 将"优化、调整"等模糊动词补充具体操作对象和方式
- 示例：`"优化下性能"` → `"请针对首页首屏加载速度进行性能优化，目标将LCP控制在2.5秒以内"`

**4. 单一指令补全要素（Who/What/How/When）**
- 补全执行对象、操作内容、执行方式、约束条件
- 示例：`"导个数据"` → `"请从后台导出2026年8月的用户注册数据，格式为CSV，包含用户名、注册时间、渠道来源三列"`

**5. 复合指令拆解与结构化**
- 将多任务消息拆分为编号清单
- 示例：`"改下页面然后看看速度"` → `"1. 修改首页轮播图尺寸为1200x600px；2. 使用Lighthouse重新检测首屏加载速度并报告结果"`

**6. 模糊目标转化为可衡量指标**
- 将定性描述转化为定量或可观测指标
- 示例：`"让网站更快"` → `"优化网站性能，首屏加载时间控制在2秒内，PageSpeed Insights移动端得分不低于90分"`

### 输出格式

| 原始消息 | 润色后消息 |
|---------|----------|
| `[用户原始输入]` | `[扩展后的专业消息]`（附带简要的补全说明） |

### 约束条件

- 不改变用户原始意图
- 补全信息需基于上下文合理推断，不确定部分需向用户确认
- 保持语气中立专业，不添加个人评价
- 润色结果需带编号或分层结构，便于用户快速理解

## 文档同步规则

> [!IMPORTANT]
> 每次改动代码必须同步改文档，否则视为未完成。

1. 触发条件：改 `app/src/main/java`、`.gradle.kts`、`AndroidManifest.xml`、`scope.list/arrays.xml` 中任意一项，就视作需同步。
2. 必改文档：
   - 改 Hook 逻辑 → 更新 `README.md` 的“已适配”与“目录”两段，写明类名 `HookRegistry.kt:11` 与开关 `lingxi_hook_xxx`。
   - 改主题/颜色/圆角 → 更新 `README.md` 的“主题”段，写明 `G2Shapes large=16.dp` 与验证机型 `V2520A`。
   - 改提交/分支流程 → 同步 `COMMIT_CONVENTION.md` 与 `README.md` 的“提交规范”“分支与合并”两章。
3. 提交要求：同一 `commit` 内代码与文档一起 `git add`，正文写清 `docs(readme): 同步 xxx Hook 说明` 或与功能 commit 同条，避免“代码已合、文档滞后”的孤儿提交。
4. 自检清单（`git diff --stat` 时逐项过）：
    - `README.md` 是否出现本次新增的类/方法/开关名
    - `COMMIT_CONVENTION.md` 的示例是否仍能对应当前 `type/scope`
    - 若未同步，需在 PR 描述首行写“待补文档”并打 `docs` 标签，禁止直接 `gh pr merge`。

## 文件与文件夹创建规范

> [!IMPORTANT]
> 按目标应用分包，主 Hook 只做分发，子 Hook 单功能单文件。新增适配严格参照 `app/src/main/java/github/boxiaolanya2008/lingxihook/hook/powersaving/` 的主/子分层，禁止跨包混放。

### 1. 目录结构（以 `powersaving` 为样板）

```
app/src/main/java/github/boxiaolanya2008/lingxihook/hook/
  HookRegistry.kt:11          注册表唯一入口，新增适配加一行
  AppHooker.kt:22             目标应用接口
  LingXiHook.kt:31            按包名分发，不写具体逻辑
  powersaving/                单目标应用文件夹（包名尾段小写，如 powersaving / systemui / settings）
    IqooPowerSavingHook.kt:14 主 Hook，implements AppHooker，只做分发
    WirelessChargeHook.kt:28  子 Hook，单功能单文件
    DeepOptimizationHook.kt:22 子 Hook，单功能单文件
```

- 新增目标应用 `com.xxx.yyy` → 新建文件夹 `hook/yyy/`，禁止复用 `powersaving/` 或把新功能塞进已有主 Hook。
- 禁止在 `hook/` 根目录直接新建 `XxxHook.kt`，必须先建应用文件夹。

### 2. 主 Hook（管理层）`IqooPowerSavingHook.kt:14`

- 命名：`{前缀} + {应用名}Hook`，如 `IqooPowerSavingHook`，`packageName` 必须与 `scope.list:2` / `arrays.xml:5` 完全一致 `IqooPowerSavingHook.kt:16`。
- 职责：只声明 `label/description/features` 与分发 `install`，不写任何 `Class.forName/hook/intercept` 具体逻辑 `IqooPowerSavingHook.kt:41`。
- `features: List<HookFeature>` 每项对应一个子 Hook，`key` 统一 `lingxi_hook_{snake}` 如 `lingxi_hook_wireless` `IqooPowerSavingHook.kt:49`，`title/description` 写清 Hook 点与用户可见效果，`defaultEnabled=true`。
- `companion object` 集中定义 `FEATURE_XXX` 常量，子 Hook 通过 `IqooPowerSavingHook.FEATURE_XXX` 读取开关，禁止子 Hook 自定字符串键。
- `install` 内仅 3 类语句：`HookLogger.log(INFO, "powersaving", "适配器已注入")` + 逐个 `subHook.install(module,param)`，新增子功能在此追加一行 `IqooPowerSavingHook.kt:41`。

### 3. 子 Hook（功能层）`WirelessChargeHook.kt:28` `DeepOptimizationHook.kt:22`

- 命名：`{功能}Hook` 如 `WirelessChargeHook` / `DeepOptimizationHook`，一文件一功能，禁止一文件多功能。
- 必须结构：
  ```kotlin
  class WirelessChargeHook {
      fun install(module: XposedModule, param: PackageLoadedParam) { /* Class.forName + 逐方法 hook */ }
      private fun hookXxx(...): Int { /* 单方法拦截 */ }
      private companion object { const val TAG="wireless"; const val CLASS_UTILS="..." }
  }
  ```
  参考 `WirelessChargeHook.kt:30 forceTrue` 与 `DeepOptimizationHook.kt:24 hooked 计数`。
- 常量区：`TAG`（日志分类，与 `adb logcat -s LingXiHook | grep TAG` 对应）、`CLASS_XXX`、`METHOD_XXX` 全放 `companion object`，混淆方法名变化时只改此处 `WirelessChargeHook.kt:69`。
- 容错：`Class.forName` 找不到 → `HookLogger.log(WARN, TAG, "... not found") + return/continue`，不抛异常 `WirelessChargeHook.kt:31`；`getDeclaredMethod` 找不到同理；全部 `hook(...).setExceptionMode(PROTECTIVE)` `WirelessChargeHook.kt:48`。
- 开关：每个 `intercept` 首行 `if (!HookConfig.isEnabled(FEATURE_XXX, true)) return@intercept chain.proceed()` `WirelessChargeHook.kt:51` `DeepOptimizationHook.kt:66`，关闭时走原逻辑。
- 日志：`HookLogger.log(INFO/WARN, TAG, "hooked ... -> true/false")` 成功打 INFO，失败打 WARN；高频路径传 `persist=false` 避免广播风暴 `HookLogger.kt:18`。
- 多类/多签名兼容：如 `DeepOptimizationHook.kt:27` 需同时适配 `appoptimize.b/d`，或 `getPredictDexoptTime` 的 `MutableList/List/无参反射` 兜底 `DeepOptimizationHook.kt:94`，`startDexoptJob` 回调双候选 `DeepOptimizationHook.kt:54`。

### 4. 注册与配置闭环

1. 主 Hook 写完后在 `HookRegistry.kt:11 all` 加一行 `IqooPowerSavingHook()`，`LingXiHook.kt:31` 与首页 `HomePage.kt:46` 自动生效，无需改 UI。
2. 同时在 `resources/META-INF/xposed/scope.list` 与 `res/values/arrays.xml:3 xposed_scope` 各加一行包名，保持双份一致 `CodeRule.md:64 文档同步规则`。
3. 开关持久化：UI 侧 `AppPrefs.setFeatureEnabled(context, key, value)` 自动镜像 `Settings.System` `AppPrefs.kt:83`，Hook 侧 `HookConfig.isEnabled(key, def)` 读取 `HookConfig.kt:30`，禁止子 Hook 直接读 `SharedPreferences`。

### 5. 禁止项

- 禁止主 Hook 写 `hook/intercept` 具体逻辑，禁止子 Hook 持有 `features` 定义。
- 禁止跨应用文件夹引用：`powersaving` 子 Hook 不得 `import hook.systemui.*`。
- 禁止新增 `TonalCard` 等已废弃组件，新 UI 走 `SegmentedColumn/SegmentedListItem` `README.md:66`。
- 禁止提交时只改代码不改 `README.md` 已适配段与 `HookRegistry.kt:11` 行号，违者按文档同步规则打回。

### 6. 自检清单（`git diff --stat` 时逐项过）

- [ ] 是否为新目标应用新建了 `hook/{app}/` 文件夹，而非把文件丢进 `powersaving/`？
- [ ] 主 Hook 是否仅分发、子 Hook 是否一文件一功能且 `TAG/CLASS/METHOD` 收敛在 `companion object`？
- [ ] `HookRegistry.kt:11`、`scope.list`、`arrays.xml` 三处是否同步新增包名？
- [ ] 开关 `key` 是否为 `lingxi_hook_xxx` 且在主 Hook `companion object` 唯一定义？
- [ ] 每个 `intercept` 是否先判 `HookConfig.isEnabled` 且 `setExceptionMode(PROTECTIVE)`？
- [ ] `README.md` 已适配与目录是否已写明新类名与开关名？