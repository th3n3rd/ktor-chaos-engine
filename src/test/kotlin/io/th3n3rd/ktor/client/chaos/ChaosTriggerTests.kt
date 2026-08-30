package io.th3n3rd.ktor.client.chaos

import io.kotest.matchers.shouldBe
import io.th3n3rd.ktor.client.chaos.ChaosTriggers.Always
import io.th3n3rd.ktor.client.chaos.ChaosTriggers.Never
import org.junit.jupiter.api.Test

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
}