package io.th3n3rd.ktor.client.chaos

import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ChaosEngineTests {

    private val delegate = MockEngine { respondOk("delegated") }
    private val engine = ChaosEngine(delegate = delegate)
    private val client = HttpClient(engine)

    @Test
    fun `delegates by default when behaves normally`() = runTest {
        val response = client.request(anyRequest())

        response.bodyAsText() shouldBe "delegated"
    }

    @Test
    fun `applies given behaviour when misbehaves`() = runTest {
        engine.misbehave { _, _ -> respondOk("misbehaved") }

        val result = client.request(anyRequest())

        result.bodyAsText() shouldBe "misbehaved"
    }
}

