package io.nautime.jetbrains

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Transient
import java.time.Instant
import java.util.UUID

@Service
@State(name = "nautime.io", storages = [Storage(value = "nautime.io.xml")])
class PluginStateHolder() : PersistentStateComponent<PluginState> {

    val pluginState: PluginState = PluginState()

    override fun getState(): PluginState {
        NauPlugin.log.info("getState $pluginState")
        return pluginState
    }

    override fun loadState(state: PluginState) {
        NauPlugin.log.info("loadState $state")
        XmlSerializerUtil.copyBean(state, pluginState);
    }

}


data class PluginState(
    var pluginId: String = UUID.randomUUID().toString(),
    var isLinked: Boolean = false,
    var isCliReady: Boolean = false,
    var needCliUpdate: Boolean = false,
    var currentCliVer: String = "",
    var latestCliVer: String = "v1.0.0",
    var latestCheckVal: Long = 0L,
) {

    @get:Transient
    var latestCheck: Instant
        set(value) {
            latestCheckVal = value.epochSecond
        }
        get() = Instant.ofEpochSecond(latestCheckVal)

    fun timeToCheck(): Boolean {
        return true
//        return latestCheck.isBefore(Instant.now().minus(2, ChronoUnit.MINUTES))
    }

    fun tryUpdateCliVersion(newVersion: String) {
        if (latestCliVer != newVersion) {
            latestCliVer = newVersion
            needCliUpdate = true
            NauPlugin.log.info("Set new latestCliVer: $latestCliVer")
        }
    }

    fun needCliUpdate() = latestCliVer != currentCliVer

}
