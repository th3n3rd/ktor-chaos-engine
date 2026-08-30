package io.th3n3rd.ktor.client.chaos

import io.kotest.matchers.shouldBe
import io.th3n3rd.ktor.client.chaos.ChaosTriggers.Always
import io.th3n3rd.ktor.client.chaos.ChaosTriggers.Never
import org.junit.jupiter.api.Test

class ChaosTriggerTests {

    @Test
    fun `always triggers`() {
        val trigger = Always()

        trigger(anyRequest().build()) shouldBe true
        trigger(anyRequest().build()) shouldBe true
    }

    @Test
    fun `never triggers`() {
        val trigger = Never()

        trigger(anyRequest().build()) shouldBe false
        trigger(anyRequest().build()) shouldBe false
    }
}