package io.github.th3n3rd.ktor.client.chaos

import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.NotFound
import kotlinx.coroutines.test.runTest
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
    fun `matches routes by method and exact path`() = runTest {
        val upstream = object : ChaoticUpstream(url = Url("https://route-matching")) {
            override fun routes(): Handler = routing {
                get("/tests") {
                    respondOk("get")
                }
                post("/tests") {
                    respondOk("post")
                }
                put("/tests") {
                    respondOk("put")
                }
                delete("/tests") {
                    respondOk("delete")
                }
                head("/tests") {
                    respondOk("head")
                }
                options("/tests") {
                    respondOk("options")
                }
            }
        }

        val client = HttpClient(ReverseProxy(upstream))

        client.get("https://route-matching/tests").bodyAsText() shouldBe "get"
        client.post("https://route-matching/tests").bodyAsText() shouldBe "post"
        client.put("https://route-matching/tests").bodyAsText() shouldBe "put"
        client.delete("https://route-matching/tests").bodyAsText() shouldBe "delete"
        client.head("https://route-matching/tests").bodyAsText() shouldBe "head"
        client.options("https://route-matching/tests").bodyAsText() shouldBe "options"

        client.get("https://route-matching/does-not-exist").status shouldBe NotFound
        client.post("https://route-matching/does-not-exist").status shouldBe NotFound
        client.put("https://route-matching/does-not-exist").status shouldBe NotFound
        client.delete("https://route-matching/does-not-exist").status shouldBe NotFound
        client.head("https://route-matching/does-not-exist").status shouldBe NotFound
        client.options("https://route-matching/does-not-exist").status shouldBe NotFound
    }
}