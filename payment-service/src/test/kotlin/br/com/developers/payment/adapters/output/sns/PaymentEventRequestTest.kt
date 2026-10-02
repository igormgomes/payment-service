package br.com.developers.payment.adapters.output.sns

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

@DisplayName("Payment event request test")
class PaymentEventRequestTest {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `Should serialize the payment event request with snake case fields`() {
        val request = PaymentEventRequest("1", "SCHEDULED_PAYMENT", "2026-10-01", "a@b.com")

        val json: JsonNode = this.objectMapper.readTree(this.objectMapper.writeValueAsString(request))

        assertAll("Assert serialized event", {
            assertThat(json.fieldNames().asSequence().toSet(), `is`(equalTo(setOf("id", "event_type", "date", "pix_key_credit"))))
            assertThat(json["id"].asText(), `is`(equalTo("1")))
            assertThat(json["event_type"].asText(), `is`(equalTo("SCHEDULED_PAYMENT")))
            assertThat(json["date"].asText(), `is`(equalTo("2026-10-01")))
            assertThat(json["pix_key_credit"].asText(), `is`(equalTo("a@b.com")))
        })
    }
}
