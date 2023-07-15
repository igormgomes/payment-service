package br.com.developers.receipt.handler


import br.com.developers.receipt.PaymentReceiptNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class PaymentReceiptExceptionHandler: ResponseEntityExceptionHandler() {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(PaymentReceiptNotFoundException::class)
    fun handlePaymentNotFoundException(ex: PaymentReceiptNotFoundException): ResponseEntity<ErrorResponse>{
        log.error("Executing handlePaymentNotFoundException", ex)

        val errorMessageResponse = ErrorMessageResponse(ex.message)
        val errorResponse = ErrorResponse(listOf(errorMessageResponse))

        return ResponseEntity(errorResponse, HttpHeaders(), HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ErrorResponse>{
        log.error("Executing handleException", ex)

        val errorMessageResponse = ErrorMessageResponse(ex.message)
        val errorResponse = ErrorResponse(listOf(errorMessageResponse))

        return ResponseEntity(errorResponse, HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR)
    }
}