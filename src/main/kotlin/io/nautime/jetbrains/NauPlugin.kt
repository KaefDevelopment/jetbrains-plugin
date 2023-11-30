package io.nautime.jetbrains

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
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
import io.nautime.jetbrains.statusbar.NauStatusBar
import io.nautime.jetbrains.utils.FileDb
import io.nautime.jetbrains.utils.IdeUtils
import io.nautime.jetbrains.utils.getCurrentFile
import io.nautime.jetbrains.utils.getFile
import io.nautime.jetbrains.utils.orLast
import java.net.InetAddress
import java.time.Duration
import java.time.Instant
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit.SECONDS
import javax.swing.UIManager

const val JOB_PERIOD_SEC = 60L
const val MIN_DURATION_FOR_SAME_TARGET_SEC = 60L // min duration before add events for same target

const val SERVER_ADDRESS = "https://nautime.io"

@Service
class NauPlugin() : Disposable {

    private val httpSender = HttpSender()
    private val cliExecutor = CliExecutor()

    private val osName: String
    private val systemName: String
    private val ideType: String
    private val ideVersion: String
    private val eventQueue: Queue<EventDto>


    init {
        log.info("Initialize plugin! project: ")
        ideType = ApplicationNamesInfo.getInstance().productName
        ideVersion = ApplicationInfo.getInstance().fullVersion
        log.info("IDE type: $ideType IDE version: $ideVersion")
        eventQueue = ConcurrentLinkedQueue()
        osName = SystemInfo.getOsNameAndVersion()
        systemName = InetAddress.getLocalHost().hostName

        pluginStateHolder = service<PluginStateHolder>()
        pluginState = pluginStateHolder.pluginState
        log.info("Plugin state: $pluginState")

        notificationService = NotificationService()

        fileDb = FileDb()
        fileDb.init()

        plugin = this
        initPlugin = true
        log.info("Plugin initialization is done")

        if (!pluginState.isLinked) {
            log.info("Not linked. Start check job")
            notificationService.showGoToLinkNotif()
        }
    }

    private val mainJobFuture: ScheduledFuture<*> = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay({ mainJob() }, 0, JOB_PERIOD_SEC, SECONDS)

    private fun mainJob() {
        if (!pluginState.isLinked || pluginState.timeToCheck()) {
            check()
        }

        if (!pluginState.isCliReady) {
            checkCli()
        }

        if (pluginState.needCliUpdate) {
            initCli()
        }

        if (pluginState.isCliReady) {
            if (sendByCli()) return
        }

        sendByHttp()
    }

    private fun checkCli() {
        if (!CliHolder.isCliReady()) {
            getState().needCliUpdate = true
            log.info("Cli not found")
            return
        }

        pluginState.isCliReady = true
        pluginState.currentCliVer = cliExecutor.version()
        pluginState.needCliUpdate = getState().needCliUpdate()

        log.info("Cli was found. currentCliVer: ${pluginState.currentCliVer} needCliUpdate: ${pluginState.needCliUpdate}")
    }

    private fun initCli() {
        if (!CliHolder.isCliReady() || getState().needCliUpdate) {
            getState().isCliReady = false
            CliHolder.installCli()
        }

        if (CliHolder.isCliReady()) {
            getState().isCliReady = true
            getState().needCliUpdate = false
            getState().currentCliVer = cliExecutor.version()
            log.info("Setup currentCliVer to ${getState().currentCliVer}")
        } else {
            log.warn("Cli not installed..")
        }
    }

    fun init() {
        if (isInit()) return

        log.info("Init NAU plugin. IDE type: $ideType IDE version: $ideVersion State: $pluginState")
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

    private var lastEvent: EventDto = EventDto(type = EventType.ACTION, target = "<empty>", project = null, projectBaseDir = null, branch = null, language = null)
    private var lastTime: Instant = Instant.MIN
    private var lastTimeEmptyTarget: Instant = Instant.MIN
    private var count: Int = 1
    private var keys: Int = 1

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
                addToQueueAndDb(event)
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
                addToQueueAndDb(event)
                count = 1
            }
            return
        }

        if (count > 0 || keys > 0) {
            addToQueueAndDb(lastEvent.copy(createdAt = event.createdAt, params = mapOf("count" to "$count", "keys" to "$keys") + event.params))
        }

        addToQueueAndDb(event)
        lastEvent = event
        count = 1
        keys = 0

        log.info("Event added $event")
        lastTime = now
    }


    private fun addToQueueAndDb(event: EventDto) {
        eventQueue.add(event)
        fileDb.add(event.print())
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


    private fun check() = executeSend {
        if (pluginState.isLinked && !IdeUtils.isIdeInFocus()) return@executeSend

        try {
            val statusResponse = try {
                val response = httpSender.getStatus(getPluginId())
                pluginState.tryUpdateCliVersion(response.cliVersion)
                stats = response.stats
                pluginState.latestCheck = Instant.now()
                updateStatusBar()
                response
            } catch (ex: Exception) {
                log.info("Error during get status request", ex)
                PluginStatusResponse.DEFAULT
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

        // todo create view of queue
        val eventsToSend = eventQueue.toList()
        if (eventsToSend.isEmpty()) {
            return@executeSend true
        }

        /**
         *
         *                 pluginVersion = "1.0.0", // todo add to params
         *                 cliType = "todo",
         *                 cliVersion = "todo",
         *
         *                 osName = osName,
         *                 deviceName = systemName,
         *                 ideType = ideType,
         *                 ideVersion = ideVersion,
         */

        val sendRequest = SendEventsRequest(
            events = eventsToSend
        )

        val isSuccess = sendBlock(sendRequest)
        if (isSuccess) repeat(eventsToSend.size) { eventQueue.poll() }

        return@executeSend isSuccess
    }

    private fun sendByCli(): Boolean {
        return try {
            baseSend { sendRequest ->
                cliExecutor.send(sendRequest)
            }
        } catch (ex: Exception) {
            log.info("Send by cli error", ex)
            false
        }
    }

    private fun sendByHttp(): Boolean {
        return try {
            baseSend { sendRequest ->
                return@baseSend httpSender.send(sendRequest)
            }
        } catch (ex: Exception) {
            log.info("Send by http error", ex)
            false
        }
    }

    private fun updateStatusBar() {
        ProjectManager.getInstance().openProjects.filter { !it.isDisposed }.map { project ->
            val statusbar = WindowManager.getInstance().getStatusBar(project) ?: return
            statusbar.updateWidget(NauStatusBar.WIDGET_ID)
        }
    }


    override fun dispose() {
        mainJobFuture.cancel(false)
        fileDb.close()
    }

    companion object {
        val log = Logger.getInstance("nautime.io")
        private var initPlugin = false

        private lateinit var plugin: NauPlugin
        private lateinit var pluginState: PluginState
        private lateinit var pluginStateHolder: PluginStateHolder
        private lateinit var notificationService: NotificationService
        private lateinit var fileDb: FileDb
        private var stats: Stats? = null

        fun getInstance(): NauPlugin = plugin

        fun isInit(): Boolean = initPlugin

        fun getState(): PluginState = pluginState

        fun getStats(): Stats? = stats

        fun getPluginId(): String = pluginState.pluginId

        fun getPluginLinkUrl(): String = "https://nautime.io/link/${getPluginId()}?utm_source=plugin-jetbrains&utm_content=plugin_link"

        fun getDashboardUrl(): String = "https://nautime.io/dashboard?utm_source=plugin-jetbrains&utm_content=status_bar"

        fun getNotificationService(): NotificationService = notificationService

        fun getStatusBarText(): String {
            if(!getState().isLinked) return "Nau"
            if (stats == null) return "Nau"
            val duration = Duration.ofSeconds(stats!!.total)
            if (duration.toMinutes() == 0L) return "Nau"
            val hours = duration.toHours()
            val mins = duration.minusHours(hours).toMinutes()
            if (hours == 0L) return "${mins}m"
            if (mins == 0L) return "${hours}h"
            return "${hours}h ${mins}m"
        }

        fun isUnderDarcula(): Boolean = UIManager.getLookAndFeel().name.contains("Darcula")
    }
}
