package br.com.developers.payment

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@RequestMapping("/api/payment")
class PaymentController(private val paymentService: PaymentService) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun save(@Valid @RequestBody paymentRequest: PaymentRequest): ResponseEntity<Any> {
        log.info("Adding payment $paymentRequest")

        val payment = paymentRequest.toPayment()
        this.paymentService.save(payment)

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}