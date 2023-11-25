package io.nautime.jetbrains.senders

import io.nautime.jetbrains.NauPlugin
import io.nautime.jetbrains.SERVER_ADDRESS
import io.nautime.jetbrains.Sender
import io.nautime.jetbrains.model.PluginStatusResponse
import io.nautime.jetbrains.model.SendEventsRequest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import java.nio.charset.Charset

class HttpSender : Sender {

    // когда слинкуется, отображать в статус баре и забирать partId и инфу по кодингу

    override fun send(eventsRequest: SendEventsRequest): Boolean {
        val json = Json.encodeToString(eventsRequest)

        NauPlugin.log.info("[HTTP] start sending events [${NauPlugin.getPluginId()}] $eventsRequest")

        HttpClients.createDefault().use { client ->
            val httpPost = HttpPost("$SERVER_ADDRESS/api/plugin/v1/events")

            val entity = StringEntity(json)
            httpPost.entity = entity
            httpPost.setHeader("Accept", "application/json")
            httpPost.setHeader("Content-type", "application/json")
            httpPost.setHeader("Authorization", NauPlugin.getPluginId())

            client.execute(httpPost).use { response ->
                val responseBody = response.entity.content.readBytes().toString(Charset.defaultCharset())
                NauPlugin.log.info("[HTTP] send events result: ${response.statusLine.statusCode} $responseBody")

                if (response.statusLine.statusCode != 200) {
                    NauPlugin.log.info("[HTTP] Events send error: ${response.statusLine.statusCode}")
                    return false
                }
            }
        }

        NauPlugin.log.info("[HTTP] Events sent")
        return true
    }


    fun getStatus(pluginId: String): PluginStatusResponse {
        NauPlugin.log.info("Get status $pluginId")

        HttpClients.createDefault().use { client ->
            val entity = StringEntity("{}")

            val httpPost = HttpPost("$SERVER_ADDRESS/api/web/v1/user/plugin/status")
            httpPost.setHeader("Authorization", NauPlugin.getPluginId())
            httpPost.setHeader("Accept", "application/json")
            httpPost.setHeader("Content-type", "application/json")
            httpPost.entity = entity

            client.execute(httpPost).use { response ->
                val responseBody = response.entity.content.readBytes().toString(Charset.defaultCharset())
                NauPlugin.log.info("Get status response: ${response.statusLine.statusCode} $responseBody")
                if (response.statusLine.statusCode != 200) {
                    // todo rewrite it
                    return PluginStatusResponse.DEFAULT
                }

                return Json.decodeFromString<PluginStatusResponse>(responseBody)
            }
        }
    }


}
