package com.bookverse.contacts.settings

enum class TransitionType {
    SCALE,
    SLIDE,
    FADE,
    FLIP,
    SHRINK,
    EXPAND,
    ELASTIC,
    CIRCLE_REVEAL,
    ROTATE,
    ZOOM_BLUR,
    WAVE;

    companion object {
        fun fromName(name: String?): TransitionType = entries.find {
            it.name == name
        } ?: SLIDE
    }
}
