package io.th3n3rd.ktor.client.chaos

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.th3n3rd.ktor.client.chaos.ChaosBehaviours.Latency
import io.th3n3rd.ktor.client.chaos.ChaosBehaviours.None
import io.th3n3rd.ktor.client.chaos.ChaosBehaviours.ReturnStatus
import io.th3n3rd.ktor.client.chaos.ChaosBehaviours.StripBody
import io.th3n3rd.ktor.client.chaos.ChaosBehaviours.SuspendForever
import io.th3n3rd.ktor.client.chaos.ChaosBehaviours.ThrowException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.time.withTimeoutOrNull
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.time.measureTimedValue

class ChaosBehaviourTests {

    private val delegate = MockEngine { respondOk("delegated") }
    private val engine = ChaosEngine(delegate)
    private val client = HttpClient(engine)

    @Test
    fun `no behaviour applied`() = runTest {
        engine.misbehave(None)

        val response = client.request(anyRequest())

        response.bodyAsText() shouldBe "delegated"
    }

    @Test
    fun `returns specified status`() = runTest {
        engine.misbehave(ReturnStatus(BadRequest))

        val response = client.request(anyRequest())

        response.status shouldBe BadRequest
    }

    @Test
    fun `strips body off of the response`() = runTest {
        engine.misbehave(StripBody())

        val response = client.request(anyRequest())

        response.bodyAsText() shouldBe ""
    }

    @Test
    fun `throws exceptions`() = runTest {
        engine.misbehave(ThrowException())

        val exception = shouldThrow<RuntimeException> { client.request(anyRequest()) }

        exception.message shouldBe "something went wrong!"
    }

    @Test
    fun `introduces latency`() = runTest {
        engine.misbehave(Latency(Duration.ofMillis(500)))

        val (response, duration) = measureTimedValue { client.request(anyRequest()) }

        response.bodyAsText() shouldBe "delegated"
        duration.inWholeMilliseconds shouldBeGreaterThanOrEqual 500
    }

    @Test
    fun `suspends forever`() = runTest {
        engine.misbehave(SuspendForever())

        val deferred = async {
            client.request(anyRequest())
        }

        val response = withContext(Dispatchers.Default) { // else we will use the context from the runTest scheduler
            withTimeoutOrNull(Duration.ofMillis(100)) {
                deferred.await()
            }
        }

        response shouldBe null
        deferred.cancelAndJoin()
    }
}

