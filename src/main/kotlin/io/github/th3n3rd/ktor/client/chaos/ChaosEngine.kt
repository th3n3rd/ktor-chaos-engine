package io.github.th3n3rd.ktor.client.chaos

import io.github.th3n3rd.ktor.client.chaos.ChaosBehaviours.None
import io.github.th3n3rd.ktor.client.chaos.ChaosTriggers.Always
import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.utils.io.*

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

    fun misbehave(behaviour: ChaosBehaviour) = misbehave(behaviour applied Always)

    fun misbehave(stage: ChaosStage) {
        this.stage = stage
    }

    fun behave() {
        stage = defaultBehaviour
    }

    companion object {
        val defaultBehaviour = None applied Always
    }
}