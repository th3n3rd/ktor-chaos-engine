package io.th3n3rd.ktor.client.chaos

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.utils.io.InternalAPI

class ChaosEngine(
    override val config: HttpClientEngineConfig = HttpClientEngineConfig(),
    private val delegate: HttpClientEngine,
) : HttpClientEngineBase("ktor-chaos-engine") {

    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        return delegate.execute(data)
    }
}