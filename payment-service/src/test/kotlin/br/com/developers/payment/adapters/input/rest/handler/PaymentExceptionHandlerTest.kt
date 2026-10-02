package br.com.developers.payment.adapters.input.rest.handler

import br.com.developers.payment.adapters.input.rest.PaymentRequest
import br.com.developers.payment.domain.PaymentDeletionNotAllowedException
import br.com.developers.payment.domain.PaymentNotFoundException
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.mock.web.MockHttpServletRequest

@DisplayName("Payment exception handler test")
class PaymentExceptionHandlerTest {

    private lateinit var paymentExceptionHandler: PaymentExceptionHandler

    @BeforeEach
    fun before() {
        this.paymentExceptionHandler = PaymentExceptionHandler()
    }

    @Test
    fun `Should return 404 with the error format when the payment is not found`() {
        val response = this.paymentExceptionHandler.handlePaymentNotFoundException(PaymentNotFoundException("Payment 1 not found"))

        assertAll("Assert not found response", {
            assertThat(response.statusCode, `is`(equalTo(HttpStatus.NOT_FOUND)))
            assertThat(response.body, `is`(equalTo(ErrorResponse(listOf(ErrorMessageResponse("Payment 1 not found"))))))
        })
    }

    @Test
    fun `Should return 422 with the error format when the payment deletion is not allowed`() {
        val response = this.paymentExceptionHandler
            .handlePaymentDeletionNotAllowedException(PaymentDeletionNotAllowedException("Payment processed 1 can't be change"))

        assertAll("Assert deletion not allowed response", {
            assertThat(response.statusCode, `is`(equalTo(HttpStatus.UNPROCESSABLE_ENTITY)))
            assertThat(
                response.body,
                `is`(equalTo(ErrorResponse(listOf(ErrorMessageResponse("Payment processed 1 can't be change")))))
            )
        })
    }

    @Test
    fun `Should return 500 with a fixed message when an unexpected exception happens`() {
        val response = this.paymentExceptionHandler.handleException(RuntimeException("boom"))

        assertAll("Assert internal error response", {
            assertThat(response.statusCode, `is`(equalTo(HttpStatus.INTERNAL_SERVER_ERROR)))
            assertThat(response.body, `is`(equalTo(ErrorResponse(listOf(ErrorMessageResponse("Internal server error"))))))
        })
    }

    @Test
    fun `Should return 400 with one message per field error when the request is invalid`() {
        val bindingResult = BeanPropertyBindingResult(PaymentRequest(), "paymentRequest").apply {
            addError(FieldError("paymentRequest", "date", "must not be null"))
            addError(FieldError("paymentRequest", "value", "must not be null"))
        }
        val method = PaymentExceptionHandlerTest::class.java.getDeclaredMethod("target", PaymentRequest::class.java)
        val exception = MethodArgumentNotValidException(MethodParameter(method, 0), bindingResult)

        val response = this.paymentExceptionHandler.handleException(
            exception, ServletWebRequest(MockHttpServletRequest())
        )

        assertAll("Assert bad request response", {
            assertThat(response?.statusCode, `is`(equalTo(HttpStatus.BAD_REQUEST)))
            assertThat(
                response?.body,
                `is`(
                    equalTo<Any>(
                        ErrorResponse(
                            listOf(
                                ErrorMessageResponse("date must not be null"),
                                ErrorMessageResponse("value must not be null")
                            )
                        )
                    )
                )
            )
        })
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    private fun target(paymentRequest: PaymentRequest) = Unit
}
