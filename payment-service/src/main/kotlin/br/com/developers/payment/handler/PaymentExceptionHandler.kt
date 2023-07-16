package br.com.developers.payment.handler

import br.com.developers.payment.PaymentDeletionNotAllowedException
import br.com.developers.payment.PaymentNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class PaymentExceptionHandler: ResponseEntityExceptionHandler() {

    private val log = LoggerFactory.getLogger(javaClass)

    @Override
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        log.error("Executing handleMethodArgumentNotValid", ex)

        val errorMessageResponse = ex.bindingResult.fieldErrors.map {
            ErrorMessageResponse(it.field.plus(" ").plus(it.defaultMessage))
        }
        val errorResponse = ErrorResponse(errorMessageResponse)

        return ResponseEntity(errorResponse, HttpHeaders(), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(PaymentNotFoundException::class)
    fun handlePaymentNotFoundException(ex: PaymentNotFoundException): ResponseEntity<ErrorResponse>{
        log.error("Executing handlePaymentNotFoundException", ex)

        val errorMessageResponse = ErrorMessageResponse(ex.message)
        val errorResponse = ErrorResponse(listOf(errorMessageResponse))

        return ResponseEntity(errorResponse, HttpHeaders(), HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(PaymentDeletionNotAllowedException::class)
    fun handlePaymentDeletionNotAllowedException(ex: PaymentDeletionNotAllowedException): ResponseEntity<ErrorResponse>{
        log.error("Executing handlePaymentDeletionNotAllowedException", ex)

        val errorMessageResponse = ErrorMessageResponse(ex.message)
        val errorResponse = ErrorResponse(listOf(errorMessageResponse))

        return ResponseEntity(errorResponse, HttpHeaders(), HttpStatus.UNPROCESSABLE_ENTITY)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ErrorResponse>{
        log.error("Executing handleException", ex)

        val errorMessageResponse = ErrorMessageResponse(ex.message)
        val errorResponse = ErrorResponse(listOf(errorMessageResponse))

        return ResponseEntity(errorResponse, HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR)
    }
}