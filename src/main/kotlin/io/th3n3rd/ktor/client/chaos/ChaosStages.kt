package io.th3n3rd.ktor.client.chaos

import io.ktor.client.request.*
import io.th3n3rd.ktor.client.chaos.ChaosBehaviours.None
import io.th3n3rd.ktor.client.chaos.ChaosTriggers.Always
import java.util.concurrent.atomic.AtomicBoolean

infix fun ChaosStage.until(trigger: ChaosTrigger) = object : ChaosStage {
    private val active = AtomicBoolean(true)

    override fun invoke(request: HttpRequestData): ChaosBehaviour {
        active.compareAndSet(true, !trigger(request))
        return if (active.get()) this@until(request) else None
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