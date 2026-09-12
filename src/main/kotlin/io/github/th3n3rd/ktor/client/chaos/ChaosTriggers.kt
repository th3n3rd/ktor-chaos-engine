package io.github.th3n3rd.ktor.client.chaos

import io.github.th3n3rd.ktor.client.chaos.ChaosTriggers.Percentage
import io.github.th3n3rd.ktor.client.chaos.ChaosTriggers.RequestMatchingCount
import io.ktor.client.request.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

fun interface ChaosTrigger {
    operator fun invoke(request: HttpRequestData): Boolean
}

object ChaosTriggers {

    object Always : ChaosTrigger {
        override fun invoke(request: HttpRequestData) = true
    }

    object Never : ChaosTrigger {
        override fun invoke(request: HttpRequestData) = false
    }

    object RequestMatchingCount {
        operator fun invoke(n: Int, matches: (HttpRequestData) -> Boolean): ChaosTrigger {
            val counter = AtomicInteger(0)
            return ChaosTrigger {
                matches(it) && counter.incrementAndGet() >= n
            }
        }
    }

    object Percentage {
        operator fun invoke(value: Int, random: Random = Random()) = ChaosTrigger { request ->
            require(value in 0..100) { "$value should be between 0 and 100" }
            random.nextInt(100) < value
        }
    }
}

inline val Int.requests get() = match { true }
fun Int.match(predicate: (HttpRequestData) -> Boolean) = RequestMatchingCount(this, predicate)

inline val Int.percent get() = Percentage(this)
fun Int.percent(random: Random = Random()) = Percentage(this@percent, random)