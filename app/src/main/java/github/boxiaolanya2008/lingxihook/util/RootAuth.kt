package github.boxiaolanya2008.lingxihook.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Root 一键授权（应用启动时执行，无需用户手动点）。
 *
 * 背景：Hook 开关镜像写 Settings.System 要“修改系统设置”，有 Root 时一条
 * appops 命令即可在启动时静默拿下，免手动跳页。
 * - 本包 `WRITE_SETTINGS`：Hook 功能开关镜像必需。
 */
object RootAuth {

    data class Result(val command: String, val ok: Boolean)

    suspend fun authorizeAll(ownPackage: String): List<Result> = withContext(Dispatchers.IO) {
        if (!RootUtil.isRooted()) return@withContext emptyList()
        listOf(
            "appops set $ownPackage WRITE_SETTINGS allow"
        ).map { cmd -> Result(cmd, RootUtil.su(cmd)) }
    }
}
