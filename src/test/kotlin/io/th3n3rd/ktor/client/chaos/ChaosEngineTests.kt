package io.th3n3rd.ktor.client.chaos

import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Put
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
        engine.misbehave(returnText("misbehaved"))

        val result = client.request(anyRequest())

        result.bodyAsText() shouldBe "misbehaved"
    }

    @Test
    fun `restore behaviour after misbehaving`() = runTest {
        engine.misbehave(returnText("misbehaved"))
        engine.behave()

        val result = client.request(anyRequest())

        result.bodyAsText() shouldBe "delegated"
    }

    @Test
    fun `applies complex behaviour`() = runTest {
        engine.misbehave(
            (returnText("first") until 3.requests)
                .then(returnText("second") whenever { it.method == Put } until 1.requests)
                .then(returnText("third") applied 100.percent until { it.method == Get })
        )

        repeat(3) { client.get(anyRequest()).bodyAsText() shouldBe "first" }
        client.put(anyRequest()).bodyAsText() shouldBe "second"
        repeat(1000) { client.post(anyRequest()).bodyAsText() shouldBe "third" }
        client.get(anyRequest()).bodyAsText() shouldBe "delegated"
    }

    private fun returnText(text: String): ChaosBehaviour = { _, _ -> respondOk(text) }
}

