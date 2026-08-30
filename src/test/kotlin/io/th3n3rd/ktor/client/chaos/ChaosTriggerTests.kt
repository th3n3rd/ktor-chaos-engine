package io.th3n3rd.ktor.client.chaos

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
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
    fun `triggers exactly n times`() {
        with(1.times()) {
            this(anyRequest().build()) shouldBe true
            this(anyRequest().build()) shouldBe false
        }

        with(2.times()) {
            this(anyRequest().build()) shouldBe true
            this(anyRequest().build()) shouldBe true
            this(anyRequest().build()) shouldBe false
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