package io.nautime.jetbrains.utils

import io.nautime.jetbrains.NauPlugin
import java.io.File

class OsHelper {

    companion object {
        fun getSystemProperty(name: String): String = System.getProperty(name) ?: ""
        fun getSystemEnv(name: String): String = System.getenv(name) ?: ""


        private val osNameStr = getSystemProperty("os.name")
        private val archStr = getSystemProperty("os.arch")
        val os = detectOs()
        val arch = detectArch()

        fun isWindows(): Boolean {
            return os == Os.WINDOWS
        }

        fun isMacosx(): Boolean {
            return os == Os.MACOSX
        }

        fun isLinux(): Boolean {
            return os == Os.LINUX
        }

        private fun detectOs(): Os {
            return when {
                osNameStr.contains("windows", ignoreCase = true) -> Os.WINDOWS
                osNameStr.contains("mac", ignoreCase = true) || osNameStr.contains("darwin", ignoreCase = true) -> Os.MACOSX
                osNameStr.contains("linux", ignoreCase = true) -> Os.LINUX
                else -> Os.UNKNOWN // todo send error to server
            }
        }

        private fun detectArch(): String {
            if (archStr.contains("386") || archStr.contains("32")) return "386"
            if (archStr == "aarch64") return "arm64"
            if (isMacosx() && archStr.contains("arm")) return "arm64"
            return if (archStr.contains("64")) "amd64" else archStr
        }

        fun makeExecutable(file: File) {
            try {
                file.setExecutable(true)
            } catch (ex: SecurityException) {
                NauPlugin.log.warn(ex)
            }
        }
    }

}

enum class Os(
    val tag: String,
    val ext: String,
) {
    WINDOWS("windows", ".exe"),
    MACOSX("darwin", ""),
    LINUX("linux", ""),
    UNKNOWN("", ""),
}
