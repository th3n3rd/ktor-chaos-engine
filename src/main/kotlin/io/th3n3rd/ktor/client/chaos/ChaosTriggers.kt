package io.th3n3rd.ktor.client.chaos

import io.ktor.client.request.*
import io.th3n3rd.ktor.client.chaos.ChaosTriggers.Percentage
import io.th3n3rd.ktor.client.chaos.ChaosTriggers.Times
import java.util.*
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

    object Percentage {
        operator fun invoke(value: Int, random: Random = Random()) = ChaosTrigger { request ->
            require(value in 0..100) { "$value should be between 0 and 100" }
            random.nextInt(100) < value
        }
    }
}

fun Int.times() = Times(this@times)
fun Int.percent(random: Random = Random()) = Percentage(this@percent, random)