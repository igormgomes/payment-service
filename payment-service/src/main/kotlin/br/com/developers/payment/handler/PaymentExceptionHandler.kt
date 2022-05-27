package br.com.developers.payment.handler

import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
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
    override fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        log.error("Executing handleMethodArgumentNotValid", ex)

        val errorMessage = ex.bindingResult.fieldErrors.map {
            ErrorMessage(it.field.plus(" ").plus(it.defaultMessage))
        }

        log.error(errorMessage.toString())

        val errorResponse = ErrorResponse(errorMessage)

        return ResponseEntity(errorResponse, HttpHeaders(), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ErrorResponse>{
        log.error("Executing handleException", ex)

        val errorMessage = ErrorMessage(ex.message)
        val errorResponse = ErrorResponse(listOf(errorMessage))

        return ResponseEntity(errorResponse, HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR)
    }
}