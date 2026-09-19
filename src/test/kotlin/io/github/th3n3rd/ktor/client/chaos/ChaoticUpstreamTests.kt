package io.github.th3n3rd.ktor.client.chaos

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

class ChaoticUpstreamTests {

    private val upstream = object : ChaoticUpstream() {
        override fun routes(): Handler = { request ->
            respondOk("upstream")
        }
    }

    private val client = HttpClient(ReverseProxy(upstream))

    @Test
    fun `pass-through when behaving`() = runTest {
        client.get(upstream.url).bodyAsText() shouldBe "upstream"
    }

    @Test
    fun `engages the chaotic engine when misbehave`() = runTest {
        upstream.misbehave(returnText("misbehaved"))

        client.get(upstream.url).bodyAsText() shouldBe "misbehaved"
    }

    @Test
    fun `accepts an arbitrary url`() = runTest {
        val upstream = object : ChaoticUpstream(url = Url("https://custom-url")) {
            override fun routes(): Handler = { request ->
                respondOk("custom-url")
            }
        }

        val client = HttpClient(ReverseProxy(upstream))

        client.get("https://custom-url").bodyAsText() shouldBe "custom-url"
    }

    @Test
    fun `supports convenient ways of receive typed requests and send typed responses`() = runTest {
        val upstream = object : ChaoticUpstream(url = Url("https://typed")) {
            override fun routes(): Handler = { request ->
                val typedRequest = request.receive<TypedRequest>()
                val typedResponse = TypedResponse(typedRequest.input)
                respondJson(typedResponse)
            }
        }
        val client = HttpClient(ReverseProxy(upstream))

        val response = client.get("https://typed") { setBody("""{ "input": "foo" }""") }

        response.bodyAsText() shouldEqualJson """{ "output": "foo" }"""
        response.contentType() shouldBe ContentType.Application.Json
    }

    @Serializable
    data class TypedRequest(val input: String)

    @Serializable
    data class TypedResponse(val output: String)
}