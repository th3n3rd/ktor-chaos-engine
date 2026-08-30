package io.th3n3rd.ktor.client.chaos

import io.ktor.client.request.*

fun interface ChaosTrigger {
    operator fun invoke(request: HttpRequestData): Boolean
}

object ChaosTriggers {
    object Always {
        operator fun invoke() = ChaosTrigger { true }
    }

    object Never {
        operator fun invoke() = ChaosTrigger { false }
    }
}