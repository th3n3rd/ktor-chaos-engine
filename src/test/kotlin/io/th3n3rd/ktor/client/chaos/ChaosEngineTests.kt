package io.th3n3rd.ktor.client.chaos

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ChaosEngineTests {

    @Test
    fun `delegates by default when behaves normally`() = runTest {
        val delegate = MockEngine { respondOk() }
        val engine = ChaosEngine(delegate = delegate)
        val client = HttpClient(engine)

        val result = client.get("https://example.com")

        result.status shouldBe HttpStatusCode.OK
    }
}

class ChaosEngine(
    override val config: HttpClientEngineConfig = HttpClientEngineConfig(),
    private val delegate: HttpClientEngine,
) : HttpClientEngineBase("ktor-chaos-engine") {

    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        return delegate.execute(data)
    }
}