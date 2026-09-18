package io.github.th3n3rd.ktor.client.chaos

import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.NotFound
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ChaoticUpstreamRoutingTests {
    @Test
    fun `matches route by method and exact path`() = runTest {
        val upstream = object : ChaoticUpstream(url = Url("https://route-matching")) {
            override fun routes(): Handler = routing {
                get("/tests") { respondOk("get") }
                post("/tests") { respondOk("post") }
                put("/tests") { respondOk("put") }
                delete("/tests") { respondOk("delete") }
                head("/tests") { respondOk("head") }
                options("/tests") { respondOk("options") }
            }
        }

        val client = HttpClient(ReverseProxy(upstream))

        client.get("https://route-matching/tests").bodyAsText() shouldBe "get"
        client.post("https://route-matching/tests").bodyAsText() shouldBe "post"
        client.put("https://route-matching/tests").bodyAsText() shouldBe "put"
        client.delete("https://route-matching/tests").bodyAsText() shouldBe "delete"
        client.head("https://route-matching/tests").bodyAsText() shouldBe "head"
        client.options("https://route-matching/tests").bodyAsText() shouldBe "options"
    }

    @Test
    fun `no matching route results into a not found by default`() = runTest {
        val upstream = object : ChaoticUpstream(url = Url("https://route-not-matching")) {
            override fun routes(): Handler = routing {
                get("/tests") { respondOk("get") }
                post("/tests") { respondOk("post") }
                put("/tests") { respondOk("put") }
                delete("/tests") { respondOk("delete") }
                head("/tests") { respondOk("head") }
                options("/tests") { respondOk("options") }
            }
        }

        val client = HttpClient(ReverseProxy(upstream))

        client.get("https://route-not-matching/does-not-exist").status shouldBe NotFound
        client.post("https://route-not-matching/does-not-exist").status shouldBe NotFound
        client.put("https://route-not-matching/does-not-exist").status shouldBe NotFound
        client.delete("https://route-not-matching/does-not-exist").status shouldBe NotFound
        client.head("https://route-not-matching/does-not-exist").status shouldBe NotFound
        client.options("https://route-not-matching/does-not-exist").status shouldBe NotFound
    }

    @Test
    fun `matches route or else`() = runTest {
        val upstream = object : ChaoticUpstream(url = Url("https://route-not-matching")) {
            override fun routes(): Handler = routing {
                get("/tests") { respondOk("get") }
                post("/tests") { respondOk("post") }
                put("/tests") { respondOk("put") }
                delete("/tests") { respondOk("delete") }
                head("/tests") { respondOk("head") }
                options("/tests") { respondOk("options") }
                orElse { respondOk("else") }
            }
        }

        val client = HttpClient(ReverseProxy(upstream))

        client.get("https://route-not-matching/does-not-exist").bodyAsText() shouldBe "else"
        client.post("https://route-not-matching/does-not-exist").bodyAsText() shouldBe "else"
        client.put("https://route-not-matching/does-not-exist").bodyAsText() shouldBe "else"
        client.delete("https://route-not-matching/does-not-exist").bodyAsText() shouldBe "else"
        client.head("https://route-not-matching/does-not-exist").bodyAsText() shouldBe "else"
        client.options("https://route-not-matching/does-not-exist").bodyAsText() shouldBe "else"
    }
}