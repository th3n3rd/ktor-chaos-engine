package io.th3n3rd.ktor.client.chaos

import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.utils.io.*
import io.th3n3rd.ktor.client.chaos.ChaosBehaviours.NoOp
import io.th3n3rd.ktor.client.chaos.ChaosTriggers.Always

class ChaosEngine(
    private val delegate: HttpClientEngine,
    override val config: HttpClientEngineConfig = HttpClientEngineConfig(),
) : HttpClientEngineBase("ktor-chaos-engine") {
    private var stage = defaultBehaviour

    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val behaviour = stage(data)
        return behaviour(data) {
            delegate.execute(it)
        }
    }

    fun misbehave(behaviour: ChaosBehaviour) = misbehave(behaviour.applied(Always))

    fun misbehave(stage: ChaosStage) {
        this.stage = stage
    }

    fun behave() {
        stage = defaultBehaviour
    }

    companion object {
        val defaultBehaviour = NoOp().applied(Always)
    }
}