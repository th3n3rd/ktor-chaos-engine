package io.github.th3n3rd.ktor.client.chaos

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

        client.get("https://route-matching/tests").bodyAsText() shouldBe "get"
        client.post("https://route-matching/tests").bodyAsText() shouldBe "post"
        client.put("https://route-matching/tests").bodyAsText() shouldBe "put"
        client.delete("https://route-matching/tests").bodyAsText() shouldBe "delete"
        client.head("https://route-matching/tests").bodyAsText() shouldBe "head"
        client.options("https://route-matching/tests").bodyAsText() shouldBe "options"
    }

    @Test
    fun `matches route by method and templated path`() = runTest {
        val upstream = object : ChaoticUpstream(url = Url("https://templated-route-matching")) {
            override fun routes(): Handler = routing {
                get("/tests/{testId}/results") { respondOk("get") }
                post("/tests/{testId}/results") { respondOk("post") }
                put("/tests/{testId}/results") { respondOk("put") }
                delete("/tests/{testId}/results") { respondOk("delete") }
                head("/tests/{testId}/results") { respondOk("head") }
                options("/tests/{testId}/results") { respondOk("options") }
            }
        }

        val client = HttpClient(ReverseProxy(upstream))

        client.get("https://templated-route-matching/tests/foo/results").bodyAsText() shouldBe "get"
        client.post("https://templated-route-matching/tests/foo/results").bodyAsText() shouldBe "post"
        client.put("https://templated-route-matching/tests/foo/results").bodyAsText() shouldBe "put"
        client.delete("https://templated-route-matching/tests/foo/results").bodyAsText() shouldBe "delete"
        client.head("https://templated-route-matching/tests/foo/results").bodyAsText() shouldBe "head"
        client.options("https://templated-route-matching/tests/foo/results").bodyAsText() shouldBe "options"
    }

    @Test
    fun `extract templated segments into the requests attributes`() = runTest {
        val upstream = object : ChaoticUpstream(url = Url("https://templated-route-matching")) {
            override fun routes(): Handler = routing {
                get("/tests/{testId}/results/{resultId}") { respondOk("get - ${it.parameters["testId"]} - ${it.parameters["resultId"]}") }
                post("/tests/{testId}/results/{resultId}") { respondOk("post - ${it.parameters["testId"]} - ${it.parameters["resultId"]}") }
                put("/tests/{testId}/results/{resultId}") { respondOk("put - ${it.parameters["testId"]} - ${it.parameters["resultId"]}") }
                delete("/tests/{testId}/results/{resultId}") { respondOk("delete - ${it.parameters["testId"]} - ${it.parameters["resultId"]}") }
                head("/tests/{testId}/results/{resultId}") { respondOk("head - ${it.parameters["testId"]} - ${it.parameters["resultId"]}") }
                options("/tests/{testId}/results/{resultId}") { respondOk("options - ${it.parameters["testId"]} - ${it.parameters["resultId"]}") }
            }
        }

        val client = HttpClient(ReverseProxy(upstream))

        (randomUUID() to randomUUID()).let { (testId, resultId) -> client.get("https://templated-route-matching/tests/$testId/results/${resultId}").bodyAsText() shouldBe "get - $testId - $resultId" }
        (randomUUID() to randomUUID()).let { (testId, resultId) -> client.post("https://templated-route-matching/tests/$testId/results/${resultId}").bodyAsText() shouldBe "post - $testId - $resultId" }
        (randomUUID() to randomUUID()).let { (testId, resultId) -> client.put("https://templated-route-matching/tests/$testId/results/${resultId}").bodyAsText() shouldBe "put - $testId - $resultId" }
        (randomUUID() to randomUUID()).let { (testId, resultId) -> client.delete("https://templated-route-matching/tests/$testId/results/${resultId}").bodyAsText() shouldBe "delete - $testId - $resultId" }
        (randomUUID() to randomUUID()).let { (testId, resultId) -> client.head("https://templated-route-matching/tests/$testId/results/${resultId}").bodyAsText() shouldBe "head - $testId - $resultId" }
        (randomUUID() to randomUUID()).let { (testId, resultId) -> client.options("https://templated-route-matching/tests/$testId/results/${resultId}").bodyAsText() shouldBe "options - $testId - $resultId" }
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