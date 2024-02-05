package io.nautime.jetbrains.senders

import io.nautime.jetbrains.CliHolder
import io.nautime.jetbrains.NauPlugin
import io.nautime.jetbrains.SERVER_ADDRESS
import io.nautime.jetbrains.ex.CliNotReadyEx
import io.nautime.jetbrains.model.SendEventsRequest
import io.nautime.jetbrains.utils.OsHelper
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

class CliExecutor(
    private val nauPlugin: NauPlugin,
) {

    fun send(eventsRequest: SendEventsRequest): Boolean {
        val json = NauPlugin.json.encodeToString(eventsRequest)

        NauPlugin.log.info("[Cli] start sending events [${nauPlugin.getPluginId()}] $eventsRequest")

        if (!CliHolder.isCliReady()) {
            nauPlugin.getState().isCliReady = false
            NauPlugin.log.warn("Cli not found. Set isCliReady to false")
            return false
        }

        try {

            val path = CliHolder.CLI_FILE.absolutePath
            val cmds = "$path event -d events:${eventsRequest.events.size} -k ${nauPlugin.getPluginId()} -s $SERVER_ADDRESS/api/plugin/v1/events"

            NauPlugin.log.info("Execute cli with cmds $cmds")

            val pb = ProcessBuilder(path, "event", "-d", formatJson(json), "-k", nauPlugin.getPluginId(), "-s", "$SERVER_ADDRESS/api/plugin/v1/events")
            val proc = pb.start()

            try {
                val stdin = BufferedWriter(OutputStreamWriter(proc.outputStream))
                stdin.write(json)
                stdin.write("\n")
//                try {
                stdin.flush()
                stdin.close()
//                } catch (e: IOException) {
//                }
            } catch (e: Exception) {
                NauPlugin.log.warn("[Cli] Events sent error", e)
                return false
            }

            val stdout = BufferedReader(InputStreamReader(proc.inputStream))
            val stderr = BufferedReader(InputStreamReader(proc.errorStream))
            val procResult = proc.waitFor(60, TimeUnit.SECONDS)
            if (!procResult) {
                NauPlugin.log.warn("[Cli] Events sent error timeout")
                return false
            }

            val stdoutStr = stdout.lines().collect(Collectors.joining())
            if (stdoutStr.isNotBlank()) {
                NauPlugin.log.info("[Cli] Events sent out $stdoutStr")
                val response: StatusResponse = NauPlugin.json.decodeFromString(stdoutStr)
                if (response.status) NauPlugin.log.info("[Cli] Events sent")
                return response.status
            }

            val stderrStr = stderr.lines().collect(Collectors.joining())
            if (stderrStr.isNotBlank()) {
                NauPlugin.log.warn("[Cli] Events sent error $stderrStr")
                return false
            }

            NauPlugin.log.info("[Cli] Events sent error. Empty stdout")
            return false
        } catch (ex: Exception) {
            if (OsHelper.isWindows() && ex.toString().contains("Access is denied")) {
                nauPlugin.getNotificationService().showWarningNotif(
                    title = "Error",
                    message = "Microsoft Defender is blocking NAU app. Please allow ${CliHolder.CLI_FILE.absolutePath} to run to be able to upload your stats."
                )
            }

            NauPlugin.log.warn("Events sent error", ex)
            return false
        }

    }

    fun version(): String {

        NauPlugin.log.info("Cli: check version")

        if (!CliHolder.isCliReady()) {
            nauPlugin.getState().isCliReady = false
            NauPlugin.log.info("Cli not found. Set isCliReady to false")
            throw CliNotReadyEx()
        }

        try {
            val path = CliHolder.CLI_FILE.absolutePath
            val cmds = "$path version"

//            KaefPlugin.log.info("Execute cli with cmds $cmds")

            val proc = Runtime.getRuntime().exec(cmds)

            val stdout = BufferedReader(InputStreamReader(proc.inputStream))
            val stderr = BufferedReader(InputStreamReader(proc.errorStream))
            proc.waitFor()

            val out = stdout.lines().collect(Collectors.joining())
            val err = stderr.lines().collect(Collectors.joining())

            if (out.isNotBlank()) NauPlugin.log.info("Get version out $out")
            if (err.isNotBlank()) NauPlugin.log.warn("Get version err $err")

            return out
        } catch (ex: Exception) {
            if (OsHelper.isWindows() && ex.toString().contains("Access is denied")) {
                nauPlugin.getNotificationService().showWarningNotif(
                    title = "Error",
                    message = "Microsoft Defender is blocking NAU app. Please allow ${CliHolder.CLI_FILE.absolutePath} to run to be able to upload your stats."
                )
            }

            NauPlugin.log.warn("Get version error", ex)
            throw CliNotReadyEx()
        }

    }

    private fun formatJson(json: String): String {
        return when {
            OsHelper.isWindows() -> "\"${json.replace("\"", "\\\"")}\""
            else -> json
        }
    }

    @Serializable
    data class StatusResponse(
        val status: Boolean,
        val error: String,
    )
}
