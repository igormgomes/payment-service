package br.com.developers.payment

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.validation.Validation
import javax.validation.Validator;
import javax.validation.ValidatorFactory

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