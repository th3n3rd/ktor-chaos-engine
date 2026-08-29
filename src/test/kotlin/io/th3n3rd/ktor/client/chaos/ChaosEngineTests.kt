package io.th3n3rd.ktor.client.chaos

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
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

