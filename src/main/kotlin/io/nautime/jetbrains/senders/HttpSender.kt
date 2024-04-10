package io.nautime.jetbrains.senders

import io.nautime.jetbrains.NauPlugin
import io.nautime.jetbrains.NauPlugin.Companion.PLUGIN_TYPE
import io.nautime.jetbrains.SERVER_ADDRESS
import io.nautime.jetbrains.model.PluginStatusResponse
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import java.nio.charset.Charset


class HttpSender(
    private val nauPlugin: NauPlugin,
) {
    private val httpClient: CloseableHttpClient = HttpClients.createDefault()

    private var requestConfig: RequestConfig = RequestConfig.custom()
        .setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
        .setConnectTimeout(CONNECTION_TIMEOUT_MS)
        .setSocketTimeout(CONNECTION_TIMEOUT_MS)
        .build()

    fun getStatus(pluginId: String): PluginStatusResponse {
        NauPlugin.log.info("Get status $pluginId")

        val entity = StringEntity("{}")

        val httpPost = HttpPost("$SERVER_ADDRESS/api/web/v1/user/plugin/status2")
        httpPost.config = requestConfig
        httpPost.setHeader("Authorization", nauPlugin.getPluginId())
        httpPost.setHeader("Accept", "application/json")
        httpPost.setHeader("Content-type", "application/json")
        httpPost.setHeader("X-Version", NauPlugin.getPluginVersion())
        httpPost.setHeader("X-Source", PLUGIN_TYPE)
        httpPost.entity = entity

        // todo send information about status bar hide

        httpClient.execute(httpPost).use { response ->
            val responseBody = response.entity.content.readBytes().toString(Charset.defaultCharset())
            NauPlugin.log.info("Get status response: ${response.statusLine.statusCode} $responseBody")
            if (response.statusLine.statusCode != 200) {
                // todo rewrite it
                return PluginStatusResponse.default(nauPlugin)
            }

            return NauPlugin.json.decodeFromString<PluginStatusResponse>(responseBody)
        }
    }

    fun close() {
        httpClient.close()
    }


    companion object {
        const val CONNECTION_TIMEOUT_MS = 30 * 1000
    }
}
