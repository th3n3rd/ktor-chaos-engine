package io.th3n3rd.ktor.client.chaos

import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.HttpMethod.Companion.Post
import io.th3n3rd.ktor.client.chaos.ChaosTriggers.Always
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ChaosStageTests {

    private val delegate = MockEngine { respondOk("delegated") }
    private val engine = ChaosEngine(delegate)
    private val client = HttpClient(engine)

    @Test
    fun `applies the given behaviour until trigger fires`() = runTest {
        engine.misbehave(
            returnText("misbehaved")
                applied Always
                until { it.method == Post }
        )

        client.get(anyRequest()).bodyAsText() shouldBe "misbehaved"
        client.post(anyRequest()).bodyAsText() shouldBe "delegated"
        client.get(anyRequest()).bodyAsText() shouldBe "delegated"
    }

    @Test
    fun `applies the given behaviour until after trigger fires`() = runTest {
        engine.misbehave(
            returnText("misbehaved")
                applied Always
                untilAfter { it.method == Post }
        )
        client.get(anyRequest()).bodyAsText() shouldBe "misbehaved"
        client.post(anyRequest()).bodyAsText() shouldBe "misbehaved"
        client.post(anyRequest()).bodyAsText() shouldBe "delegated"
        client.get(anyRequest()).bodyAsText() shouldBe "delegated"
    }

    @Test
    fun `chain behaviours`() = runTest {
        engine.misbehave(
            (returnText("first") untilAfter 1.requests)
                .then(returnText("second") untilAfter 1.requests)
                .then(returnText("third") untilAfter 1.requests)
                .then(returnText("fourth"))
        )

        client.get(anyRequest()).bodyAsText() shouldBe "first"
        client.get(anyRequest()).bodyAsText() shouldBe "second"
        client.get(anyRequest()).bodyAsText() shouldBe "third"
        client.get(anyRequest()).bodyAsText() shouldBe "fourth"
    }
}

