package io.th3n3rd.ktor.client.chaos

import io.ktor.client.request.*
import io.th3n3rd.ktor.client.chaos.ChaosBehaviours.NoOp
import java.util.concurrent.atomic.AtomicBoolean

fun ChaosBehaviour.applied(trigger: ChaosTrigger) = ChaosStage { request ->
    if (trigger(request)) this else NoOp()
}

fun ChaosStage.until(trigger: ChaosTrigger) = object : ChaosStage {
    private val active = AtomicBoolean(true)

    override fun invoke(request: HttpRequestData): ChaosBehaviour {
        if (active.get()) active.set(!trigger(request))
        return if (active.get()) this@until(request) else NoOp()
    }
}

fun interface ChaosStage {
    operator fun invoke(request: HttpRequestData): ChaosBehaviour
}