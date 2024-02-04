package io.nautime.jetbrains

import io.nautime.jetbrains.utils.OsHelper
import io.nautime.jetbrains.utils.OsHelper.Companion.getSystemEnv
import io.nautime.jetbrains.utils.OsHelper.Companion.isWindows
import io.nautime.jetbrains.utils.OsHelper.Companion.makeExecutable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.cert.X509Certificate
import java.util.zip.ZipFile
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


class CliHolder {

    companion object {
        private val BASE_DIR: File = getBaseDir()
        val CLI_FILE: File = getCliFile()

        fun getCliFile(): File {
            return File(BASE_DIR, "$CLI_NAME${OsHelper.os.ext}")
        }

        private fun getBaseDir(): File {
            val nauHome: String = getSystemEnv(NAU_HOME)
            val homeDir: File = when {
                nauHome.isNotBlank() -> nauHome
                isWindows() -> getSystemEnv(WINDOWS_HOME)
                else -> System.getProperty(NIX_HOME)
            }.let { File(it) }

            val nauDir = File(homeDir, NAU_DIR)
            if (!nauDir.exists()) {
                nauDir.mkdir()
                makeHiddenFolder(nauDir)
            }
            return nauDir
        }

        private fun makeHiddenFolder(nauDir: File) {
            try {
                Files.setAttribute(nauDir.toPath(), "dos:hidden", true, LinkOption.NOFOLLOW_LINKS)
            } catch (_: Exception) {
            }
        }

        fun isCliReady(): Boolean = CLI_FILE.exists()


        fun installCli(nauPlugin: NauPlugin) {
            // todo checkMissingPlatformSupport
            val zipFile: File? = downloadCli(nauPlugin)
            if (zipFile == null) {
                // todo send error to server
                return
            }

            try {
                CLI_FILE.delete()
                unzip(zipFile, CLI_FILE)
                makeExecutable(CLI_FILE)
                zipFile.delete()
                NauPlugin.log.info("Cli installed version ${nauPlugin.getState().latestCliVer}")
                // todo send event about install
            } catch (ex: IOException) {
                NauPlugin.log.info("Cli install error version ${nauPlugin.getState().latestCliVer}", ex)
            }
        }

        private fun getGithubCliUrl(nauPlugin: NauPlugin): String {
            return "https://github.com/KaefDevelopment/cli-service/releases/download/${nauPlugin.getState().latestCliVer}/cli-${OsHelper.os.tag}-${OsHelper.arch}${OsHelper.os.ext}.zip"
        }

        private fun downloadCli(nauPlugin: NauPlugin): File? {
            try {
                if (!BASE_DIR.exists()) {
                    BASE_DIR.mkdir()
                }

                val zipFile = File(BASE_DIR, "$CLI_NAME.zip") // todo add ide type?
                val githubUrl = URL(getGithubCliUrl(nauPlugin))

                NauPlugin.log.info("Download zip $githubUrl")

                try {
                    val byteChannel: ReadableByteChannel = Channels.newChannel(githubUrl.openStream())
                    val fos: FileOutputStream = zipFile.outputStream()
                    fos.getChannel().transferFrom(byteChannel, 0, Long.MAX_VALUE)
                } catch (ex: Exception) {
                    NauPlugin.log.warn(ex)
                    NauPlugin.log.info("Next attempt")

                    val sslContext = SSLContext.getInstance("SSL")
                    sslContext.init(null, arrayOf<TrustManager>(trustManager), null)
                    HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
                    val conn = githubUrl.openConnection()
                    conn.setRequestProperty("User-Agent", "github.com/KaefDevelopment/cli-service")

                    conn.inputStream.use { inputStream ->
                        FileOutputStream(zipFile).use { fos ->
                            var bytesRead: Int
                            val buffer = ByteArray(4096)
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                fos.write(buffer, 0, bytesRead)
                            }
                            inputStream.close()
                        }
                    }
                }

                NauPlugin.log.info("Zip downloaded $githubUrl")

                return zipFile
            } catch (ex: Exception) {
                NauPlugin.log.warn(ex)
                return null
            }
        }

        @Throws(IOException::class)
        private fun unzip(zipFile: File, outputFile: File) {
            ZipFile(zipFile, 1).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    zip.getInputStream(entry).use { input ->
                        outputFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }

        private val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate>? {
                return null
            }

        }

        const val NAU_HOME = "NAU_HOME"
        const val NAU_DIR = ".nau"
        const val WINDOWS_HOME = "USERPROFILE"
        const val NIX_HOME = "user.home"
        const val CLI_NAME = "cli"
    }

}
