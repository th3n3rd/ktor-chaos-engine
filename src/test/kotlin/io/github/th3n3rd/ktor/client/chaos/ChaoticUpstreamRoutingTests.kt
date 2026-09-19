package io.github.th3n3rd.ktor.client.chaos

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.NotFound
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.UUID.randomUUID

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

        methods.forEach { method ->
            withClue(method.value) {
                client.request(method, "https://route-matching/tests").bodyAsText() shouldBe method.value.lowercase()
            }
        }
    }

    @Test
    fun `matches route by method and templated path`() = runTest {
        val upstream = object : ChaoticUpstream(url = Url("https://templated-route-matching")) {
            override fun routes(): Handler = routing {
                methods.forEach { method ->
                    route(method, "/tests/{testId}/results") { respondOk(it.method.value.lowercase()) }
                }
            }
        }

        val client = HttpClient(ReverseProxy(upstream))

        methods.forEach { method ->
            withClue(method.value) {
                client.request(method, "https://templated-route-matching/tests/foo/results").bodyAsText() shouldBe method.value.lowercase()
            }
        }
    }

    @Test
    fun `extract templated segments into the requests attributes`() = runTest {
        val upstream = object : ChaoticUpstream(url = Url("https://extract-templated-segments")) {
            override fun routes(): Handler = routing {
                methods.forEach { method ->
                    route(method, "/tests/{testId}/results/{resultId}") { respondOk("${it.method.value.lowercase()} - ${it.parameters["testId"]} - ${it.parameters["resultId"]}") }
                }
            }
        }

        val client = HttpClient(ReverseProxy(upstream))

        methods.forEach { method ->
            withClue(method.value) {
                val testId = randomUUID()
                val resultId = randomUUID()
                client.request(method, "https://extract-templated-segments/tests/$testId/results/${resultId}").bodyAsText() shouldBe "${method.value.lowercase()} - $testId - $resultId"
            }
        }
    }

    @Test
    fun `no matching route results into a not found by default`() = runTest {
        val upstream = object : ChaoticUpstream(url = Url("https://route-not-found")) {
            override fun routes(): Handler = routing {
                methods.forEach { method ->
                    route(method, "/tests") { respondOk(it.method.value.lowercase()) }
                }
            }
        }

        val client = HttpClient(ReverseProxy(upstream))

        methods.forEach { method ->
            withClue(method.value) {
                client.request(method, "https://route-not-found/does-not-exist").status shouldBe NotFound
            }
        }
    }

    @Test
    fun `matches route or else`() = runTest {
        val upstream = object : ChaoticUpstream(url = Url("https://route-or-else")) {
            override fun routes(): Handler = routing {
                methods.forEach { method ->
                    route(method, "/tests") { respondOk(it.method.value.lowercase()) }
                }
                orElse { respondOk("else") }
            }
        }

        val client = HttpClient(ReverseProxy(upstream))

        methods.forEach { method ->
            withClue(method.value) {
                client.request(method, "https://route-or-else/does-not-exist").bodyAsText() shouldBe "else"
            }
        }
    }

    private suspend fun HttpClient.request(method: HttpMethod, url: String) = request(url) { this.method = method }

    companion object {
        private val methods = listOf(
            HttpMethod.Get,
            HttpMethod.Post,
            HttpMethod.Put,
            HttpMethod.Delete,
            HttpMethod.Head,
            HttpMethod.Options,
        )
    }
}