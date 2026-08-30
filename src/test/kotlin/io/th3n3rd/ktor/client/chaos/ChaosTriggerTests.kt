package io.th3n3rd.ktor.client.chaos

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ChaosTriggerTests {

    @Test
    fun `always triggers`() {
        val trigger = ChaosTriggers.Always()

        trigger(anyRequest().build()) shouldBe true
        trigger(anyRequest().build()) shouldBe true
    }

    @Test
    fun `never triggers`() {
        val trigger = ChaosTriggers.Never()

        trigger(anyRequest().build()) shouldBe false
        trigger(anyRequest().build()) shouldBe false
    }
}