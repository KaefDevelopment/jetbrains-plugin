package io.nautime.jetbrains.model

import kotlinx.serialization.Serializable

@Serializable
enum class EventType {
//    APP_INIT,

//    PROJECT_OPEN,
    DOCUMENT_OPEN,
    DOCUMENT_CHANGE,
    DOCUMENT_FOCUS,
    DOCUMENT_SAVE,

    ACTION,

    FRAME_IN,
    FRAME_OUT,

    KEY,
}
