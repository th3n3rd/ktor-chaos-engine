package io.github.th3n3rd.ktor.client.chaos

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotFound
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ReverseProxyTests {

    @Test
    fun `fails with a not found when there are no mappings`() = runTest {
        val client = HttpClient(ReverseProxy())

        val result = client.get("https://no-mappings")

        result.status shouldBe NotFound
        result.bodyAsText() shouldContain """Reverse proxy: no upstream found for "no-mappings""""
    }

    @Test
    fun `routes requests by exact host matching`() = runTest {
        val client = HttpClient(ReverseProxy(
            "app" to { respondOk("app") },
            "app.internal" to { respondOk("app.internal") },
            "api.app.internal" to { respondOk("api.app.internal") }
        ))

        client.get("https://app").bodyAsText() shouldBe "app"
        client.get("https://app.internal/test").bodyAsText() shouldBe "app.internal"
        client.get("https://api.app.internal/test").bodyAsText() shouldBe "api.app.internal"
    }

    @Test
    fun `fails with a not found when no mapping is matching`() = runTest {
        val client = HttpClient(ReverseProxy(
            "matching" to { respondOk("matching") },
        ))

        val result = client.get("https://no-matching")

        result.status shouldBe NotFound
        result.bodyAsText() shouldContain """Reverse proxy: no upstream found for "no-matching""""
    }

    @Test
    fun `maintains requests integrity`() = runTest {
        var captured: HttpRequestData? = null
        val client = HttpClient(ReverseProxy(
            "request-integrity" to {
                captured = it
                respondOk()
            }
        ))

        client.post("https://request-integrity/test") {
            parameter("test-param-name", "test-param-value")
            header("x-test-header-name", "test-header-value")
            setBody("test-payload")
        }

        captured?.method shouldBe HttpMethod.Post
        captured?.url?.encodedPath shouldBe "/test"
        captured?.url?.parameters?.get("test-param-name") shouldBe "test-param-value"
        captured?.headers?.get("x-test-header-name") shouldBe "test-header-value"
    }

    @Test
    fun `maintains responses integrity`() = runTest {
        val client = HttpClient(ReverseProxy(
            "response-integrity" to {
                respond(
                    statusCode = InternalServerError,
                    headers = headersOf("x-test-header-name", "test-header-value"),
                    content = "test-content",
                )
            }
        ))

        val result = client.post("https://response-integrity/test")

        result.status shouldBe InternalServerError
        result.headers["x-test-header-name"] shouldBe "test-header-value"
        result.bodyAsText() shouldBe "test-content"
    }

    @Test
    fun `supports auto-mapping for chaotic upstreams`() = runTest {
        val first = object : ChaoticUpstream() {
            override fun routes(): Handler = {
                respondOk("first")
            }
        }

        val second = object : ChaoticUpstream() {
            override fun routes(): Handler = {
                respondOk("second")
            }
        }

        val client = HttpClient(ReverseProxy(first, second))

        client.get(first.url).bodyAsText() shouldBe "first"
        client.get(second.url).bodyAsText() shouldBe "second"
    }
}

