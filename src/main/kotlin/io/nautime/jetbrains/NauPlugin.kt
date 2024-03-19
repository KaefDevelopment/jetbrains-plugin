package io.nautime.jetbrains

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.WindowManager
import com.intellij.util.concurrency.AppExecutorUtil
import io.nautime.jetbrains.model.EventDto
import io.nautime.jetbrains.model.EventParamsMap
import io.nautime.jetbrains.model.EventType
import io.nautime.jetbrains.model.PluginStatusResponse
import io.nautime.jetbrains.model.SendEventsRequest
import io.nautime.jetbrains.model.Stats
import io.nautime.jetbrains.senders.CliExecutor
import io.nautime.jetbrains.senders.HttpSender
import io.nautime.jetbrains.statusbar.NauStatusBarFactory
import io.nautime.jetbrains.utils.IdeUtils
import io.nautime.jetbrains.utils.getCurrentFile
import io.nautime.jetbrains.utils.getFile
import io.nautime.jetbrains.utils.orLast
import kotlinx.serialization.json.Json
import java.net.InetAddress
import java.time.Duration
import java.time.Instant
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.locks.ReentrantLock

const val JOB_PERIOD_SEC = 60L
const val MIN_DURATION_FOR_SAME_TARGET_SEC = 60L // min duration before add events for same target

const val SERVER_ADDRESS = "https://nautime.io"

@Service
class NauPlugin() : Disposable {

    private val httpSender = HttpSender(this)
    private val cliExecutor = CliExecutor(this)

    private val eventQueue: Queue<EventDto>

    private var pluginState: PluginState
    private var pluginStateHolder: PluginStateHolder
    private var notificationService: NotificationService

    //        private lateinit var fileDb: FileDb
    private var stats: Stats? = null

    init {
        log.info("Initialize plugin! project: ")
        eventQueue = ConcurrentLinkedQueue()

        pluginStateHolder = service<PluginStateHolder>()
        pluginState = pluginStateHolder.pluginState
        log.info("Plugin state: $pluginState")

        notificationService = NotificationService(this)
        updateStatusBar()

//        fileDb = FileDb()
//        fileDb.init()

        log.info("Plugin initialization is done")

        if (!pluginState.isLinked) {
            log.info("Not linked.")
            notificationService.showGoToLinkNotif()
        }

        getState().latestCliVer = MIN_CLI_VERSION

        addInitEvent()
    }

    private val mainJobFuture: ScheduledFuture<*> = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay({ mainJob() }, 1, JOB_PERIOD_SEC, SECONDS)

    private val lock = ReentrantLock()

    private fun mainJob() {
        if (!lock.tryLock()) return

        try {
//            log.info("Execute main job with state: $pluginState")

            if (!pluginState.isLinked || pluginState.timeToCheck()) {
                check()
            }

            checkCliExist()

            if (!pluginState.isCliReady) {
                checkCli()
            }

            if (pluginState.needCliUpdate) {
                initCli()
            }

            if (pluginState.isCliReady) {
                sendByCli(send = true)
            }

            updateStatusBar()
        } catch (ex: Exception) {
            log.info("mainJob error", ex)
        } finally {
            lock.unlock()
        }
    }

    fun checkLink() {
        AppExecutorUtil.getAppScheduledExecutorService().schedule({ if (!getState().isLinked) mainJob() }, 5, SECONDS)
        AppExecutorUtil.getAppScheduledExecutorService().schedule({ if (!getState().isLinked) mainJob() }, 15, SECONDS)
        AppExecutorUtil.getAppScheduledExecutorService().schedule({ if (!getState().isLinked) mainJob() }, 30, SECONDS)
        AppExecutorUtil.getAppScheduledExecutorService().schedule({ if (!getState().isLinked) mainJob() }, 45, SECONDS)
    }

    private fun checkCliExist(): Boolean {
        if (CliHolder.isCliReady()) return true
        pluginState.isCliReady = false
        getState().needCliUpdate = true
        log.info("Cli not found")
        return false
    }

    private fun checkCli() {
        if (!checkCliExist()) return

        pluginState.isCliReady = true
        pluginState.currentCliVer = cliExecutor.version()
        pluginState.needCliUpdate = getState().needCliUpdate()

        log.info("Cli was found. currentCliVer: ${pluginState.currentCliVer} needCliUpdate: ${pluginState.needCliUpdate}")
    }

    private fun initCli() {
        if (!CliHolder.isCliReady() || getState().needCliUpdate) {
            getState().isCliReady = false
            CliHolder.installCli(this)
        }

        if (CliHolder.isCliReady()) {
            getState().isCliReady = true
            getState().needCliUpdate = false
            getState().currentCliVer = cliExecutor.version()
            log.info("Setup currentCliVer to ${getState().currentCliVer}")
        } else {
            log.info("Cli not installed..")
        }
    }

    fun init() {
//        if (isInit()) return

        log.info("Init NAU plugin. State: $pluginState")
    }

    fun addEventProject(type: EventType, project: Project? = null, params: EventParamsMap = emptyMap()) {
        addEvent(type, project, null as VirtualFile?, params)
    }

    fun addEvent(type: EventType, project: Project? = null, document: Document? = null, params: EventParamsMap = emptyMap()) {
        addEvent(type, project, document.getFile(), params)
    }

    fun addEvent(type: EventType, project: Project? = null, virtualFile: VirtualFile? = null, params: EventParamsMap = emptyMap()) {
        val file = virtualFile ?: project.getCurrentFile()

        addEvent(
            EventDto(
                type = type,
                project = project.orLast()?.name,
                projectBaseDir = project?.basePath,
                language = (file ?: project.getCurrentFile())?.extension,
                target = file?.presentableUrl,
                branch = null,
                params = params
            )
        )
    }

    private fun addInitEvent() {
        addToQueue(
            EventDto(
                type = EventType.PLUGIN_INFO,
                project = null,
                branch = null,
                target = null,
                language = null,
                params = mapOf(
                    "pluginType" to PLUGIN_TYPE,
                    "pluginVersion" to getPluginVersion(),
                    "osName" to getOsName(),
                    "deviceName" to InetAddress.getLocalHost().hostName,
                    "ideType" to ApplicationNamesInfo.getInstance().productName,
                    "ideVersion" to ApplicationInfo.getInstance().fullVersion,
                )
            )
        )
    }

    private fun getOsName(): String {
        return when {
            SystemInfo.isMac -> "MAC"
            SystemInfo.isWindows -> "WIN"
            SystemInfo.isLinux -> "LINUX"
            else -> "OTHER"
        }
    }

    private var lastEvent: EventDto = EventDto(type = EventType.ACTION, target = "<empty>", project = null, projectBaseDir = null, branch = null, language = null)
    private var lastTime: Instant = Instant.MIN
    private var lastTimeEmptyTarget: Instant = Instant.MIN
    private var count: Int = 1
    private var keys: Int = 1

    // todo check concurrent
    fun addEvent(event: EventDto?) {
        if (event?.project == null) return

        if (!IdeUtils.isIdeInFocus() && event.type !in setOf(EventType.FRAME_OUT)) return
//        log.info("Focus ${IdeUtils.isIdeInFocus()}")

        val now = Instant.now()

        // todo use lastTime too
        if (event.target == null && Duration.between(now, lastTimeEmptyTarget).seconds < MIN_DURATION_FOR_SAME_TARGET_SEC) {
            if (event.type == EventType.KEY) {
                keys++
            } else {
                addToQueue(event)
                lastTimeEmptyTarget = Instant.now()
            }
//            lastEvent = event
//            log.info("Event added $event")
            return
        }

        // todo check lastEvent
        if (event.target == lastEvent.target && Duration.between(now, lastTime).seconds < MIN_DURATION_FOR_SAME_TARGET_SEC) {
            if (event.type == EventType.KEY) {
                keys++
                count++
            } else if (event.params.isNotEmpty()) {
                addToQueue(event)
                count = 1
            }
            return
        }

        if (count > 0 || keys > 0) {
            addToQueue(lastEvent.copy(createdAt = event.createdAt, params = mapOf("count" to "$count", "keys" to "$keys") + event.params))
        }

        addToQueue(event)
        lastEvent = event
        count = 1
        keys = 0

//        log.info("Event added $event")
        lastTime = now
    }


    private fun addToQueue(event: EventDto) {
        eventQueue.add(event)
    }


    @Volatile
    private var isSending = false

    private fun executeSend(block: () -> Unit) {
        if (isSending) return

        isSending = true

        try {
            block()
        } finally {
            isSending = false
        }
    }

    private fun <T> executeSend(ifSending: T, block: () -> T): T {
        if (isSending) return ifSending

        isSending = true

        try {
            return block()
        } finally {
            isSending = false
        }
    }

    // todo add debounce for unfocused
    private fun check() = executeSend {
//        if (pluginState.isLinked && !IdeUtils.isIdeInFocus()) return@executeSend

        try {
            val statusResponse = try {
                val response = httpSender.getStatus(getPluginId())
                pluginState.tryUpdateCliVersion(response.cliVersion)
                stats = response.stats
                pluginState.latestCheck = Instant.now()
                updateStatusBar()
                response
            } catch (ex: Exception) {
                log.info("Error during get status request: ${ex.message}")
                PluginStatusResponse.default(this)
            }

            if (pluginState.isLinked) {
                if (!statusResponse.auth) {
                    // todo add debounce
                    pluginState.isLinked = false
                    return@executeSend
                }

                statusResponse.notificationList.forEach(notificationService::show)
                return@executeSend
            }

            if (statusResponse.auth) {
                notificationService.showLinkIsDoneNotif()
                pluginState.isLinked = true
                return@executeSend
            }

            // todo save time of last notification about link. Show it later again
        } catch (ex: Exception) {
            log.info("Get status error", ex)
        }
    }

    private fun baseSend(sendBlock: (request: SendEventsRequest) -> Boolean): Boolean = executeSend(true) {
        if (eventQueue.isEmpty()) {
            return@executeSend true
        }

        val eventsToSend = QueueView(eventQueue, SEND_BATCH_SIZE)
        if (eventsToSend.isEmpty()) {
            return@executeSend true
        }

        val sendRequest = SendEventsRequest(
            events = eventsToSend
        )

        val isSuccess = sendBlock(sendRequest)
        if (isSuccess) repeat(eventsToSend.size) { eventQueue.poll() }

        return@executeSend isSuccess
    }

    private fun sendByCli(send: Boolean): Boolean {
        return try {
            baseSend { sendRequest ->
                cliExecutor.send(sendRequest, send)
            }
        } catch (ex: Exception) {
            log.info("Send by cli error", ex)
            false
        }
    }

    private fun updateStatusBar() {
        ProjectManager.getInstance().openProjects.filter { !it.isDisposed }.map { project ->
            val statusbar = WindowManager.getInstance().getStatusBar(project) ?: return
            statusbar.updateWidget(NauStatusBarFactory.WIDGET_ID)
        }
    }


    override fun dispose() {
        mainJobFuture.cancel(false)
        sendByCli(send = false)
        httpSender.httpClient.close()
    }

    fun getState(): PluginState = pluginState

    fun getStats(): Stats? = stats

    fun getPluginId(): String = pluginState.pluginId

    fun getPluginLinkUrl(): String = "$SERVER_ADDRESS/link/${getPluginId()}?utm_source=plugin-jetbrains&utm_content=plugin_link"

    fun getDashboardUrl(): String = "$SERVER_ADDRESS/dashboard?utm_source=plugin-jetbrains&utm_content=status_bar"

    fun getNotificationService(): NotificationService = notificationService

    fun getStatusBarText(): String {
        if(!getState().isLinked) return "Nau"
        val curStats = stats ?: return "Nau"
        if (!getState().showStats) return "Nau"
        if (Duration.between(pluginState.latestCheck, Instant.now()).toMinutes() > 10) return "Nau.."

        val duration = Duration.ofSeconds(curStats.total)
        if (duration.toMinutes() == 0L) return "Nau"
        return " ${duration.toToTimeStr()}"
    }

    fun getStatusBarTitle(): String {
        if (!getState().isLinked) return "Click on the bar and link plugin"
        val curStats = stats ?: return "Nau"
        if (Duration.between(pluginState.latestCheck, Instant.now()).toMinutes() > 10)
            return "Nau work in offline mode. All your stats will be saved"

        return "<table>" +
                "<tr><td>Total time:</td><td align=right>${Duration.ofSeconds(curStats.total).toToTimeStr()}</td></tr>" +
                (curStats.goal?.let { goalStats ->
                    "<tr><td>Goal progress:</td><td align=right>${goalStats.percent}% of ${Duration.ofSeconds(goalStats.duration).toToTimeStr()}</td></tr>"
                } ?: "") +
                "<tr><td colspan=\"2\">Find additional statistics on the web dashboard</td></tr>" +
                "</table>"
    }

    fun Duration.toToTimeStr(): String {
        if (this.toMinutes() == 0L) return "0m"
        val hours = this.toHours()
        val mins = this.minusHours(hours).toMinutes()
        if (hours == 0L) return "${mins}m"
        if (mins == 0L) return "${hours}h"
        return "${hours}h ${mins}m"
    }

    class QueueView<T>(
        private val queue: Queue<T>,
        sizeLimit: Int = Int.MAX_VALUE,
    ) : Collection<T> {
        override val size: Int = when {
            queue.size < sizeLimit -> queue.size
            else -> sizeLimit
        }

        override fun isEmpty(): Boolean = size == 0

        override fun iterator(): Iterator<T> {
            val iterator = queue.iterator()

            return object : Iterator<T> {
                var i = 0

                override fun hasNext(): Boolean = i < size

                override fun next(): T {
                    i++
                    return iterator.next()
                }
            }
        }

        override fun containsAll(elements: Collection<T>): Boolean = throw NotImplementedError()
        override fun contains(element: T): Boolean = throw NotImplementedError()
    }

    companion object {
        const val PLUGIN_ID = "nautime.io"
        const val PLUGIN_TYPE = "jetbrains"
        const val MIN_CLI_VERSION = "v1.0.4"

        const val SEND_BATCH_SIZE = 100

        val log = Logger.getInstance(PLUGIN_ID)
        val json = Json { ignoreUnknownKeys = true }

        fun getPluginVersion(): String = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))?.version ?: "0.0.0"
    }
}
