package io.github.th3n3rd.ktor.client.chaos

import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ChaoticUpstreamTests {

    private val upstream = object : ChaoticUpstream() {
        override fun routing(): Handler = { request ->
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
            override fun routing(): Handler = { request ->
                respondOk("custom-url")
            }
        }

        val client = HttpClient(ReverseProxy(upstream))

        client.get("https://custom-url").bodyAsText() shouldBe "custom-url"
    }
}