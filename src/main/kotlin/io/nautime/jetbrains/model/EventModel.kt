package io.nautime.jetbrains.model

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Serializable
data class SendEventsRequest(
    val events: List<EventDto>,
) {

    override fun toString(): String {
        return "SendEventsRequest(" +
                "eventsCount=${events.size}" +
                ")"
    }
}

private const val LOCAL_DATE_TIME_FORMATTER = "yyyy-MM-dd'T'HH:mm:ss"
private val localDateTimeFormatter = DateTimeFormatter.ofPattern(LOCAL_DATE_TIME_FORMATTER)

typealias EventParamsMap = Map<String, String>

@Serializable
data class EventDto(
    val id: String = UUID.randomUUID().toString(),
    val createdAt: String = localDateTimeFormatter.format(LocalDateTime.now()),
    val type: EventType,
    val project: String?,
    val projectBaseDir: String? = null,
    val language: String?,
    val target: String?,
    val branch: String?,
    var params: EventParamsMap = mapOf(),
) {

    val timezone: String = ZoneId.systemDefault().id

    fun print(): String = "$createdAt|$type|$project|$language|$target|$timezone|$params"

}
