package io.th3n3rd.ktor.client.chaos

import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ChaosEngineTests {

    @Test
    fun `delegates by default when behaves normally`() = runTest {
        val delegate = MockEngine { respondOk("delegated") }
        val engine = ChaosEngine(delegate = delegate)
        val client = HttpClient(engine)

        val response = client.request(anyRequest())

        response.bodyAsText() shouldBe "delegated"
    }

    @Test
    fun `applies given behaviour when misbehaves`() = runTest {
        val delegate = MockEngine { respondOk("delegated") }
        val engine = ChaosEngine(delegate = delegate).apply {
            misbehave { _, _ -> respondOk("misbehaved") }
        }
        val client = HttpClient(engine)

        val result = client.request(anyRequest())

        result.bodyAsText() shouldBe "misbehaved"
    }
}

