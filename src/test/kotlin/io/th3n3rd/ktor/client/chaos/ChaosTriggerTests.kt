package io.th3n3rd.ktor.client.chaos

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Put
import io.th3n3rd.ktor.client.chaos.ChaosTriggers.Always
import io.th3n3rd.ktor.client.chaos.ChaosTriggers.Never
import org.junit.jupiter.api.Test
import java.util.*

class ChaosTriggerTests {

    @Test
    fun `always triggers`() {
        Always(anyRequest().build()) shouldBe true
        Always(anyRequest().build()) shouldBe true
    }

    @Test
    fun `never triggers`() {
        Never(anyRequest().build()) shouldBe false
        Never(anyRequest().build()) shouldBe false
    }

    @Test
    fun `triggers after n requests`() {
        with(1.requests) {
            this(anyRequest().build()) shouldBe false
            this(anyRequest().build()) shouldBe true
            this(anyRequest().build()) shouldBe true
        }

        with(2.requests) {
            this(anyRequest().build()) shouldBe false
            this(anyRequest().build()) shouldBe false
            this(anyRequest().build()) shouldBe true
            this(anyRequest().build()) shouldBe true
        }
    }

    @Test
    fun `triggers after n matching requests`() {
        with(1.matches { it.method == Put }) {
            this(anyRequest().apply { method = Get }.build()) shouldBe false
            this(anyRequest().apply { method = Put }.build()) shouldBe false
            this(anyRequest().apply { method = Put }.build()) shouldBe true
            this(anyRequest().apply { method = Put }.build()) shouldBe true
        }

        with(2.matches { it.method == Put }) {
            this(anyRequest().apply { method = Get }.build()) shouldBe false
            this(anyRequest().apply { method = Put }.build()) shouldBe false
            this(anyRequest().apply { method = Put }.build()) shouldBe false
            this(anyRequest().apply { method = Put }.build()) shouldBe true
            this(anyRequest().apply { method = Put }.build()) shouldBe true
        }
    }

    @Test
    fun `triggers n percent of the time`() {
        val random = Random(0)

        with(0.percent(random)) {
            withClue("0 percent never triggers") {
                repeat(10_000) {
                    this(anyRequest().build()) shouldBe false
                }
            }
        }

        with(100.percent(random)) {
            withClue("100 percent always triggers") {
                repeat(10_000) {
                    this(anyRequest().build()) shouldBe true
                }
            }
        }
    }
}