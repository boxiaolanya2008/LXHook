package github.boxiaolanya2008.lingxihook.update

/**
 * 远程更新描述，与 update.json 字段一一对应。
 * 5 标识：versionCode / versionName / force_update / download_url / changelog
 * 远程示例见项目根目录 update.json。
 */
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val forceUpdate: Boolean = false,
    val downloadUrl: String,
    val changelog: String = ""
) {
    fun isNewerThan(currentCode: Int): Boolean = versionCode > currentCode

    companion object {
        fun fromJson(json: String): UpdateInfo {
            val obj = org.json.JSONObject(json)
            return UpdateInfo(
                versionCode = obj.optInt("versionCode", 0),
                versionName = obj.optString("versionName", "1.0"),
                forceUpdate = obj.optBoolean("force_update", false),
                downloadUrl = obj.optString("download_url", ""),
                changelog = obj.optString("changelog", "")
            )
        }
    }
}
