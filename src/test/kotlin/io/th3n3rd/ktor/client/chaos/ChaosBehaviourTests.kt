package io.th3n3rd.ktor.client.chaos

import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.th3n3rd.ktor.client.chaos.ChaosBehaviours.NoOp
import io.th3n3rd.ktor.client.chaos.ChaosBehaviours.ReturnStatus
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ChaosBehaviourTests {

    @Test
    fun `no behaviour applied`() = runTest {
        val delegate = MockEngine { respondOk("delegated") }
        val client = HttpClient(
            ChaosEngine(delegate).apply {
                misbehave(NoOp())
            }
        )

        val response = client.request(anyRequest())

        response.bodyAsText() shouldBe "delegated"
    }

    @Test
    fun `returns specified status`() = runTest {
        val delegate = MockEngine { respondOk("delegated") }
        val client = HttpClient(
            ChaosEngine(delegate = delegate).apply {
                misbehave(ReturnStatus(BadRequest))
            }
        )

        val response = client.request(anyRequest())

        response.status shouldBe BadRequest
    }
}

