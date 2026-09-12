package io.github.th3n3rd.ktor.client.chaos

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.apache.*
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.mock.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.time.withTimeoutOrNull
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.time.measureTimedValue

abstract class ChaosBehaviourContract {

    private val engine = ChaosEngine(delegate())
    private val client = HttpClient(engine)

    abstract fun delegate(): HttpClientEngine

    @Test
    fun `no behaviour applied`() = runTest {
        engine.misbehave(ChaosBehaviours.None)

        val response = sendRequest()

        response.bodyAsText() shouldBe "delegated"
    }

    @Test
    fun `returns specified status`() = runTest {
        engine.misbehave(ChaosBehaviours.ReturnStatus(HttpStatusCode.BadRequest))

        val response = sendRequest()

        response.status shouldBe HttpStatusCode.BadRequest
    }

    @Test
    fun `strips body off of the response`() = runTest {
        engine.misbehave(ChaosBehaviours.StripBody())

        val response = sendRequest()

        response.bodyAsText() shouldBe ""
    }

    @Test
    fun `throws exceptions`() = runTest {
        engine.misbehave(ChaosBehaviours.ThrowException())

        val exception = shouldThrow<RuntimeException> { sendRequest() }

        exception.message shouldBe "something went wrong!"
    }

    @Test
    fun `introduces latency`() = runTest {
        engine.misbehave(ChaosBehaviours.Latency(Duration.ofMillis(500)))

        val (response, duration) = measureTimedValue { sendRequest() }

        response.bodyAsText() shouldBe "delegated"
        duration.inWholeMilliseconds shouldBeGreaterThanOrEqual 500
    }

    @Test
    fun `suspends forever`() = runTest {
        engine.misbehave(ChaosBehaviours.SuspendForever())

        val deferred = async {
            sendRequest()
        }

        val response = withContext(Dispatchers.Default) { // else we will use the context from the runTest scheduler
            withTimeoutOrNull(Duration.ofMillis(100)) {
                deferred.await()
            }
        }

        response shouldBe null
        deferred.cancelAndJoin()
    }

    @Test
    fun `stream body forever`() = runTest {
        engine.misbehave(ChaosBehaviours.StreamBodyForever())

        val deferred = async {
            sendRequest().bodyAsText()
        }

        val body = withContext(Dispatchers.Default) { // else we will use the context from the runTest scheduler
            withTimeoutOrNull(Duration.ofMillis(100)) {
                deferred.await()
            }
        }

        body shouldBe null
        deferred.cancelAndJoin()
    }

    private suspend fun sendRequest(): HttpResponse = client.get(server.url())

    companion object {
        val server = UpstreamServer("delegated")
    }
}

class MockEngineChaosBehaviourTests : ChaosBehaviourContract() {
    override fun delegate() = MockEngine { respondOk("delegated") }
}

class CioEngineChaosBehaviourTests : ChaosBehaviourContract() {
    override fun delegate() = CIO.create()
}

class OkHttpEngineChaosBehaviourTests : ChaosBehaviourContract() {
    override fun delegate() = OkHttp.create()
}

class ApacheEngineChaosBehaviourTests : ChaosBehaviourContract() {
    override fun delegate() = Apache.create()
}