package io.github.th3n3rd.ktor.client.chaos

import io.github.th3n3rd.ktor.client.chaos.ChaosBehaviours.None
import io.github.th3n3rd.ktor.client.chaos.ChaosTriggers.Always
import io.ktor.client.request.*
import java.util.concurrent.atomic.AtomicBoolean

infix fun ChaosStage.until(trigger: ChaosTrigger) = object : ChaosStage {
    private val active = AtomicBoolean(true)

    override fun invoke(request: HttpRequestData): ChaosBehaviour {
        active.compareAndSet(true, !trigger(request))
        return if (active.get()) this@until(request) else None
    }
}

infix fun ChaosStage.untilAfter(trigger: ChaosTrigger) = object : ChaosStage {
    private val active = AtomicBoolean(true)

    override fun invoke(request: HttpRequestData): ChaosBehaviour {
        if (!active.get()) {
            return None
        }
        if (trigger(request)) {
            active.set(false)
        }
        return this@untilAfter(request)
    }
}

fun ChaosStage.then(next: ChaosStage) = ChaosStage { request ->
    val behaviour = this(request)
    if (behaviour == None) next(request) else behaviour
}

fun ChaosStage.then(next: ChaosBehaviour) = then(next applied Always)

fun interface ChaosStage {
    operator fun invoke(request: HttpRequestData): ChaosBehaviour
}