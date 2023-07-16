package br.com.developers.payment

import jakarta.validation.Validation
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PaymentRequestTest {

    private lateinit var validator: Validator

    @BeforeEach
    fun before () {
        val factory: ValidatorFactory = Validation.buildDefaultValidatorFactory()
        validator = factory.validator
    }

    @Test
    fun `Should test the payment violations`() {
        val paymentRequest = PaymentRequest()

        val violations = this.validator.validate(paymentRequest)

        assertThat(violations.size, `is`(equalTo(3)))
    }
}