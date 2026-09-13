package io.github.th3n3rd.ktor.client.chaos

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import java.util.*

abstract class ChaoticUpstream(
    val url: Url = Url("https://upstream-${UUID.randomUUID()}.internal")
) {
    private val engine = ChaosEngine(MockEngine(routing()))

    @OptIn(InternalAPI::class)
    suspend operator fun invoke(request: HttpRequestData): HttpResponseData = engine.execute(request)

    fun misbehave(behaviour: ChaosBehaviour) = engine.misbehave(behaviour)

    fun misbehave(stage: ChaosStage) = engine.misbehave(stage)

    abstract fun routing(): Handler

    typealias Handler = suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
}