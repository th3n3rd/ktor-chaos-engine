package io.th3n3rd.ktor.client.chaos

import io.ktor.client.request.*
import java.util.concurrent.atomic.AtomicInteger

fun interface ChaosTrigger {
    operator fun invoke(request: HttpRequestData): Boolean
}

object ChaosTriggers {
    val Always = ChaosTrigger { true }
    val Never = ChaosTrigger { false }

    object Times {
        operator fun invoke(exactly: Int): ChaosTrigger {
            val remaining = AtomicInteger(exactly)
            return ChaosTrigger { remaining.getAndDecrement() > 0 }
        }
    }
}

fun Int.times() = ChaosTriggers.Times(this@times)