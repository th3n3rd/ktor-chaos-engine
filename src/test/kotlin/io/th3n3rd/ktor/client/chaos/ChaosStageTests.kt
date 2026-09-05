package io.th3n3rd.ktor.client.chaos

import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpStatusCode.Companion.BadGateway
import io.ktor.http.HttpStatusCode.Companion.GatewayTimeout
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.ServiceUnavailable
import io.th3n3rd.ktor.client.chaos.ChaosBehaviours.ReturnStatus
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
            ReturnStatus(ServiceUnavailable)
                .applied(Always)
                .until { request -> request.method == Post }
        )

        client.get(anyRequest()).status shouldBe ServiceUnavailable
        client.post(anyRequest()).bodyAsText() shouldBe "delegated"
        client.get(anyRequest()).bodyAsText() shouldBe "delegated"
    }

    @Test
    fun `chain behaviours`() = runTest {
        engine.misbehave(
            ReturnStatus(InternalServerError).until(1.requests)
                .then(ReturnStatus(BadGateway).until(1.requests))
                .then(ReturnStatus(ServiceUnavailable).until(1.requests))
                .then(ReturnStatus(GatewayTimeout))
        )

        client.get(anyRequest()).status shouldBe InternalServerError
        client.get(anyRequest()).status shouldBe BadGateway
        client.get(anyRequest()).status shouldBe ServiceUnavailable
        client.get(anyRequest()).status shouldBe GatewayTimeout
    }
}

