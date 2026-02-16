package org.isoron.uhabits.core.models

import java.lang.IllegalStateException

enum class HabitDirection(val value: Int) {
    POSITIVE(0), NEGATIVE(1);

    companion object {
        fun fromInt(value: Int): HabitDirection {
            return when (value) {
                POSITIVE.value -> POSITIVE
                NEGATIVE.value -> NEGATIVE
                else -> throw IllegalStateException()
            }
        }
    }
}
