# ⚡ Ktor Client Chaos Engine

[![Maven Central](https://img.shields.io/maven-central/v/io.github.th3n3rd/ktor-client-chaos.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.th3n3rd/ktor-client-chaos)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-purple.svg)](https://kotlinlang.org)
[![Ktor](https://img.shields.io/badge/Ktor-3.0+-orange.svg)](https://ktor.io)

A lightweight, expressive chaos testing engine for [Ktor](https://ktor.io) HTTP clients.

Inject latency, simulate server faults, test client-side timeouts, throw network errors, and script multi-stage failure
scenarios with an intuitive Kotlin DSL to verify the resilience of your HTTP integrations.

---

## 🎯 Why Chaos Testing for HTTP Clients?

When building distributed systems and microservices, third-party APIs and downstream services *will* fail, slow down,
drop connections, or hang indefinitely.

`ktor-client-chaos` helps you test:

- **Retry Mechanisms**: Does your client back off and retry on `503 Service Unavailable` or transient socket exceptions?
- **Timeouts & Circuit Breakers**: Does your client correctly abort hung connections when downstream calls suspend or
  stream forever?
- **Fallback Logic & Degradation**: Does your application gracefully handle empty payloads or HTTP error codes?
- **Multi-Stage Outages**: Can your service recover after an outage transitions from failures back to healthy responses?
- **Outside-In Testing with Fakes**: Can you test full end-to-end flows using reusable fake upstreams without managing dynamic localhost ports or brittle mocks?

---

## 📦 Installation

Add the dependency to your project's test suite:

### Gradle (Kotlin DSL)

```kotlin
testImplementation("io.github.th3n3rd:ktor-client-chaos:0.1.0")
```

### Gradle (Groovy DSL)

```groovy
testImplementation 'io.github.th3n3rd:ktor-client-chaos:0.1.0'
```

### Maven

```xml

<dependency>
    <groupId>io.github.th3n3rd</groupId>
    <artifactId>ktor-client-chaos</artifactId>
    <version>0.1.0</version>
    <scope>test</scope>
</dependency>
```

---

## 🚀 Quick Start

`ChaosEngine` decorates any Ktor `HttpClientEngine` (e.g. `CIO`, `OkHttp`, `Apache5`, `Java`, or `MockEngine`). By
default, it delegates all calls directly to the underlying engine until you tell it to `misbehave()`.

```kotlin
import io.github.th3n3rd.ktor.client.chaos.*
import io.github.th3n3rd.ktor.client.chaos.ChaosBehaviours.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

// 1. Wrap your chosen engine
val engine = ChaosEngine(delegate = CIO.create())
val client = HttpClient(engine)

// Normal behavior (delegates to CIO)
val healthyResponse = client.get("https://api.example.com/data")

// 2. Inject chaos: Return 500 Internal Server Error
engine.misbehave(ReturnStatus(HttpStatusCode.InternalServerError))

// Now requests will receive HTTP 500
val chaosResponse = client.get("https://api.example.com/data")
assert(chaosResponse.status == HttpStatusCode.InternalServerError)

// 3. Restore normal behavior
engine.behave()
```

---

## 🛠️ Built-in Behaviours

`ChaosBehaviour` defines *how* the engine disrupts requests.

| Behaviour                   | Description                                                                       | Example                                           |
|:----------------------------|:----------------------------------------------------------------------------------|:--------------------------------------------------|
| `ReturnStatus(status)`      | Returns an HTTP response with the specified status code and empty body.           | `ReturnStatus(HttpStatusCode.GatewayTimeout)`     |
| `Latency(duration)`         | Injects artificial delay before executing the request.                            | `Latency(Duration.ofSeconds(2))`                  |
| `ThrowException(throwable)` | Throws an exception during request execution (defaults to `RuntimeException`).    | `ThrowException(IOException("Connection reset"))` |
| `SuspendForever()`          | Suspends the coroutine indefinitely (tests client request timeouts).              | `SuspendForever()`                                |
| `StreamBodyForever()`       | Returns a response whose body channel never completes (tests streaming timeouts). | `StreamBodyForever()`                             |
| `StripBody()`               | Executes the request normally but strips the response body.                       | `StripBody()`                                     |
| `None`                      | Pass-through; delegates normally to the underlying engine.                        | `None`                                            |

### Custom Behaviours

You can define custom behaviours using the `ChaosBehaviour` SAM interface:

```kotlin
val customBehaviour = ChaosBehaviour { request, next ->
    if (request.headers.contains("X-Require-Auth")) {
        next(request)
    } else {
        HttpResponseData(
            statusCode = HttpStatusCode.Unauthorized,
            requestTime = GMTDate(),
            headers = headersOf(),
            version = HttpProtocolVersion.HTTP_1_1,
            body = ByteReadChannel("Unauthorized".toByteArray()),
            callContext = callContext()
        )
    }
}

engine.misbehave(customBehaviour)
```

---

## 🎯 Triggers & Activation Rules

`ChaosTrigger` defines *when* and *for which requests* chaos takes effect.

| Trigger                | DSL / Extension                            | Description                                       |
|:-----------------------|:-------------------------------------------|:--------------------------------------------------|
| `Always`               | Default                                    | Triggers for every request.                       |
| `Never`                | `ChaosTriggers.Never`                      | Never triggers chaos.                             |
| `Percentage`           | `50.percent`                               | Triggers randomly `n%` of the time (0..100).      |
| `RequestMatchingCount` | `3.requests`                               | Triggers on or after the `n`-th request.          |
| `RequestMatchingCount` | `2.match { it.method == HttpMethod.Post }` | Triggers on or after the `n`-th matching request. |
| Custom Trigger         | `ChaosTrigger { request -> ... }`          | Triggers when the predicate returns `true`.       |

---

## 🧩 Composing Complex Stages & Outage Scenarios

With infix operators and sequential chaining (`then`), you can model realistic outage lifecycles (e.g., degraded latency
followed by errors, then partial recovery).

### 1. Applying Conditions (`applied`)

Apply a behaviour conditionally based on a trigger:

```kotlin
// Introduce 500ms latency only on 25% of requests
engine.misbehave(Latency(Duration.ofMillis(500)) applied 25.percent)

// Return 429 Too Many Requests only for POST requests
engine.misbehave(ReturnStatus(HttpStatusCode.TooManyRequests) applied { it.method == HttpMethod.Post })
```

### 2. Lifespan Constraints (`until` and `untilAfter`)

- **`until`**: Chaos is applied **until** the trigger fires. The triggering request will **not** be affected.
- **`untilAfter`**: Chaos is applied **until after** the trigger fires. The triggering request **is** affected, and
  subsequent requests revert to normal.

```kotlin
// Return 503 for the first 3 requests, then automatically recover
engine.misbehave(ReturnStatus(HttpStatusCode.ServiceUnavailable) untilAfter 3.requests)

// Fail until a specific recovery endpoint is called
engine.misbehave(ThrowException() until { it.url.encodedPath == "/health/recover" })
```

### 3. Multi-Stage Pipelines (`then`)

Chain multiple stages together to simulate complex failure cascades:

```kotlin
val failureScenario = (ThrowException() untilAfter 3.requests)
    .then(Latency(Duration.ofSeconds(1)) untilAfter 2.match { it.method == HttpMethod.Put })
    .then(ReturnStatus(HttpStatusCode.BadGateway) applied 50.percent until { it.method == HttpMethod.Delete })
    .then(ReturnStatus(HttpStatusCode.InternalServerError) untilAfter 1.requests)

engine.misbehave(failureScenario)
```

In this scenario:

1. The first 3 requests fail with an exception (testing immediate retries).
2. The next 2 `PUT` requests experience 1-second latency.
3. Subsequent requests experience intermittent `502 Bad Gateway` (50% failure rate) until a `DELETE` request is
   received.
4. The next single request returns `500 Internal Server Error`.
5. Afterwards, all requests delegate normally!

---

## 🧪 Testing Example (JUnit 5 + Kotest)

```kotlin
import io.github.th3n3rd.ktor.client.chaos.*
import io.github.th3n3rd.ktor.client.chaos.ChaosBehaviours.*
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ResilienceServiceTests {

    private val upstream = MockEngine { respondOk("data from server") }
    private val chaosEngine = ChaosEngine(delegate = upstream)
    private val client = HttpClient(chaosEngine)

    @Test
    fun `service recovers when upstream fails for 2 requests`() = runTest {
        // Fail 2 times with 503, then recover
        chaosEngine.misbehave(
            ReturnStatus(HttpStatusCode.ServiceUnavailable) untilAfter 2.requests
        )

        val first = client.get("https://api.example.com/items")
        first.status shouldBe HttpStatusCode.ServiceUnavailable

        val second = client.get("https://api.example.com/items")
        second.status shouldBe HttpStatusCode.ServiceUnavailable

        // Third request delegates to the upstream server
        val third = client.get("https://api.example.com/items")
        third.status shouldBe HttpStatusCode.OK
        third.bodyAsText() shouldBe "data from server"
    }
}
```

---

## 🌐 Fake Upstreams & ReverseProxy (Outside-In Testing)

### The Problem: Brittle Mocks & Localhost Port Sprawl

In integration and end-to-end testing, teams often either:
1. **Configure ad-hoc `MockEngine` instances per test**: This litters test cases with repetitive low-level HTTP mock responses and misses the opportunity to encapsulate realistic domain behaviors.
2. **Spin up local mock servers on dynamic localhost ports**: Replacing production hostnames with dynamic ports (`http://localhost:8080`) complicates application configuration and adds socket binding overhead.

### The Solution: `ChaoticUpstream` + `ReverseProxy`

`ktor-client-chaos` provides first-class primitives for building reusable **Fake Upstream APIs** routed through an in-memory **Reverse Proxy**:

- **`ChaoticUpstream`**: An abstract base class for creating reusable fake server implementations (backed by Ktor's `MockEngine`) with an integrated `ChaosEngine`. Fakes encapsulate contract routing and allow per-upstream fault injection via `misbehave(...)`.
- **`ReverseProxy`**: An `HttpClientEngine` that dispatches incoming requests by `request.url.host` entirely in-memory. It can compose multiple `ChaoticUpstream` instances or host-to-handler mappings into a single engine, enabling tests to target real hostnames (e.g. `https://api.payment.internal`) from a clean outside-in, black-box perspective.

### 1. Defining Reusable `ChaoticUpstream` Services

```kotlin
import io.github.th3n3rd.ktor.client.chaos.ChaoticUpstream
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.client.engine.mock.respondOk
import io.ktor.http.HttpMethod
import io.ktor.http.Url

class FakeBasketApi(
    url: Url = Url("https://api.basket.internal")
) : ChaoticUpstream(url) {

    override fun routing(): Handler = { request ->
        when (request.url.encodedPath) {
            "/v1/basket" -> respondOk(
                """{"items": [{"id": "item_1", "name": "Mechanical Keyboard", "price": 120}], "total": 120}"""
            )
            else -> respondBadRequest()
        }
    }
}

class FakePaymentApi(
    url: Url = Url("https://api.payments.internal")
) : ChaoticUpstream(url) {

    override fun routing(): Handler = { request ->
        when {
            request.method == HttpMethod.Post && request.url.encodedPath == "/v1/charges" -> {
                respondOk("""{"id": "ch_123", "status": "CHARGED"}""")
            }
            request.method == HttpMethod.Get && request.url.encodedPath.startsWith("/v1/charges/") -> {
                respondOk("""{"id": "ch_123", "status": "SETTLED"}""")
            }
            else -> respondBadRequest()
        }
    }
}
```

### 2. Composing Upstreams with `ReverseProxy`

Pass `ChaoticUpstream` instances directly to `ReverseProxy`:

```kotlin
val basket = FakeBasketApi()
val payments = FakePaymentApi()

// Automatically maps basket.url.host and payments.url.host to their respective upstreams
val engine = ReverseProxy(basket, payments)
```

You can also provide explicit host-to-handler pair mappings:

```kotlin
val engine = ReverseProxy(
    "api.orders.internal" to { request -> respondOk("""{"orders": []}""") },
    "api.inventory.internal" to { request -> respondOk("""{"available": true}""") }
)
```

### 3. Outside-In Acceptance Testing with an Application Under Test

```kotlin
import io.github.th3n3rd.ktor.client.chaos.ChaosBehaviours.ReturnStatus
import io.github.th3n3rd.ktor.client.chaos.ReverseProxy
import io.github.th3n3rd.ktor.client.chaos.requests
import io.github.th3n3rd.ktor.client.chaos.untilAfter
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

// 1. Domain models
@Serializable
data class BasketItem(val id: String, val name: String, val price: Int)

@Serializable
data class Basket(val items: List<BasketItem>, val total: Int)

@Serializable
data class CheckoutConfirmation(val transactionId: String)

sealed interface CheckoutResult {
    data class Success(val transactionId: String) : CheckoutResult
    data object PaymentFailed : CheckoutResult
}

// 2. Application module under test
fun Application.checkoutModule(outboundEngine: HttpClientEngine) {
    val outboundClient = HttpClient(outboundEngine) {
        install(ClientContentNegotiation) { json() }
    }

    install(ServerContentNegotiation) { json() }

    routing {
        get("/basket") {
            val response = outboundClient.get("https://api.basket.internal/v1/basket")
            if (response.status == HttpStatusCode.OK) {
                val upstreamBasket = response.body<Basket>()
                call.respond(upstreamBasket)
            } else {
                call.respond(response.status)
            }
        }

        post("/checkout") {
            val response = outboundClient.post("https://api.payments.internal/v1/charges")
            if (response.status == HttpStatusCode.OK) {
                val charge = response.body<CheckoutConfirmation>()
                call.respond(charge)
            } else {
                call.respond(HttpStatusCode.PaymentRequired)
            }
        }
    }
}

// 3. Customer actor interacting with the application over HTTP
class Customer(private val client: HttpClient) {

    suspend fun viewBasket(): Basket? {
        val response = client.get("/basket")
        return if (response.status == HttpStatusCode.OK) {
            response.body<Basket>()
        } else null
    }

    suspend fun checkout(): CheckoutResult {
        val response = client.post("/checkout")
        return if (response.status == HttpStatusCode.OK) {
            val confirmation = response.body<CheckoutConfirmation>()
            CheckoutResult.Success(confirmation.transactionId)
        } else {
            CheckoutResult.PaymentFailed
        }
    }
}

// 4. Acceptance test suite using Ktor's testApplication
class CheckoutAcceptanceTests {

    private val basketApi = FakeBasketApi()
    private val paymentApi = FakePaymentApi()

    @Test
    fun `customer views basket and completes checkout successfully`() = testApplication {
        application {
            checkoutModule(outboundEngine = ReverseProxy(basketApi, paymentApi))
        }
        val customer = Customer(createClient { install(ClientContentNegotiation) { json() } })

        customer.viewBasket() shouldBe Basket(
            items = listOf(BasketItem("item_1", "Mechanical Keyboard", 120)),
            total = 120
        )

        customer.checkout() shouldBe CheckoutResult.Success("ch_123")
    }

    @Test
    fun `customer experiences graceful degradation and recovery during payment gateway outage`() = testApplication {
        application {
            checkoutModule(outboundEngine = ReverseProxy(basketApi, paymentApi))
        }
        val customer = Customer(createClient { install(ClientContentNegotiation) { json() } })

        // Inject chaos ONLY into payment gateway: fail next 2 requests with 503
        paymentApi.misbehave(
            ReturnStatus(HttpStatusCode.ServiceUnavailable) untilAfter 2.requests
        )

        // Checkout attempts fail during outage, but basket remains accessible and healthy
        customer.checkout() shouldBe CheckoutResult.PaymentFailed
        customer.checkout() shouldBe CheckoutResult.PaymentFailed
        customer.viewBasket() shouldBe Basket(
            items = listOf(BasketItem("item_1", "Mechanical Keyboard", 120)),
            total = 120
        )

        // Payment gateway recovers automatically and customer retry succeeds
        customer.checkout() shouldBe CheckoutResult.Success("ch_123")
    }
}
```

---

## 🙏 Acknowledgements & Credits

This library is heavily inspired by the excellent [http4k Chaos](https://www.http4k.org/ecosystem/http4k/reference/chaos/) module and its elegant approach to chaos engineering in Kotlin.

---

## 📄 License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
