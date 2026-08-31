# 提交规范

> 本项目专用前缀 `lxhook:`，所有 `git log --oneline` 与 `gh pr` 标题必须以它开头，便于在 OriginOS 多模块仓库中一眼区分。

## 格式

```
lxhook: <subject>

<body>

# 或带 scope
lxhook(<scope>): <subject>
```

- `lxhook:` 固定小写，后跟半角 `:` + 空格，全角 `：` 亦兼容但推荐半角。
- `scope` 选填，取 `powersaving` `wireless` `deepopt` `theme` `ui` `log` `build`，不在表内省略。
- `subject` 用祈使句写清做了什么，首行不超过 50 字符，正文每行 72 字符内，末尾不加句号。

## scope 与示例

| 场景 | 示例 |
|---|---|
| 新 Hook | `lxhook: 新增 deepopt 空列表拦截 HookRegistry.kt:11 + IqooPowerSavingHook` |
| 带 scope | `lxhook(powersaving): 新增 deepopt startDexoptJob 强制 false` |
| 修复 | `lxhook: 修复动态取色下 paletteStyle 被绕过，改用 sys.primary 重算` |
| 带 scope 修复 | `lxhook(theme): 修复 palette 动态取色不跟随` |
| 文档 | `lxhook: docs 同步 README 已适配与目录` |
| 样式 | `lxhook: style G2 16.dp 圆角统一 SegmentedColumn` |
| 构建 | `lxhook: chore 升级 composeBom 2026.02.01` |

## body 要求

- 写清改了哪两个文件以上，例如 `HookRegistry.kt:11`、`ColorScheme.kt:26 effectiveSeed`、`SegmentedColumn.kt:76 containerColor`。
- 写清验证：`./gradlew :app:assembleDebug` + `installDebug` 到 `V2520A` + `adb logcat -s LingXiHook | grep deepopt` 或录屏。

## 正确 vs 错误

```
新增 deepopt 功能                                    // 错：缺 lxhook: 前缀
lxhook 新增 deepopt                                  // 错：缺冒号
lxhook: 新增 deepopt 空列表拦截                      // 对
lxhook(powersaving): 新增 deepopt 空列表拦截         // 对，带 scope 更易筛
```

```
lxhook: palette 无效                                 // 错：未说明是 Monet seed 导致 style 被绕过
lxhook: 修复动态取色下 paletteStyle 被 dynamicColorScheme 绕过，改用 sys.primary 重算  // 对
```

## 与分支联动

- 分支 `feat/deepopt-20260831` → 首个 commit 必须是 `lxhook: ...` 或 `lxhook(powersaving): ...`，PR 标题与首 commit 同字。
- 一个分支只做一件事，对应 1-3 个 commit，超 3 个时 `git rebase -i master` 合并后再推。
- 合并用 `gh pr merge --squash`，squash 信息取 PR 标题，禁止 `merge --no-ff` 带多条杂乱信息。

## AI 检查清单

推送前逐项勾：

- [ ] `git log --oneline -1` 以 `lxhook` 开头
- [ ] `git status` 无未跟踪的 `*.log` 或 `powersaving.apk`
- [ ] `scope.list` 与 `arrays.xml` 双份作用域已同步
- [ ] `README.md` 对应章节已同步（见 `CodeRule.md` 文档同步规则）
- [ ] 改动都有 `HookLogger` 日志或 `adb logcat` 截图对应
