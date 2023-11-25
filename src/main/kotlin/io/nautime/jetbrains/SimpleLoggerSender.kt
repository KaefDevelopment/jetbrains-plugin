package io.nautime.jetbrains

import io.nautime.jetbrains.model.SendEventsRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SimpleLoggerSender : Sender {
    override fun send(eventsRequest: SendEventsRequest): Boolean {
        NauPlugin.log.info("-------------------------------------------------------------------------------------------")
        NauPlugin.log.info(Json.encodeToString(eventsRequest))
        NauPlugin.log.info("-------------------------------------------------------------------------------------------")
        return true
    }
}
