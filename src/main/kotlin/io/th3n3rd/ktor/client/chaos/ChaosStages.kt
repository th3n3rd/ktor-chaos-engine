package io.th3n3rd.ktor.client.chaos

import io.ktor.client.request.*

fun ChaosBehaviour.applied(trigger: ChaosTrigger) = ChaosStage { request ->
    if (trigger(request)) this else ChaosBehaviours.NoOp()
}

fun interface ChaosStage {
    operator fun invoke(request: HttpRequestData): ChaosBehaviour
}