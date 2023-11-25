package io.nautime.jetbrains

import io.nautime.jetbrains.model.SendEventsRequest

interface Sender {
    fun send(eventsRequest: SendEventsRequest): Boolean
}

class AuthEx() : RuntimeException()
