package br.com.developers.payment

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import javax.validation.Valid

@RestController
@RequestMapping("/api/payment")
class PaymentController(private val paymentService: PaymentService) {

    @PostMapping
    fun save(@Valid @RequestBody paymentRequest: PaymentRequest): ResponseEntity<Any> {
        val payment = paymentRequest.toPayment()
        this.paymentService.save(payment)

        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/api/payment/${payment.pk}"))
            .build()
    }

    @GetMapping
    fun findAll(): ResponseEntity<Any> {
        val payments = this.paymentService.findAll()

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(payments)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable("id") id: String): ResponseEntity<Any> {
        this.paymentService.delete(id)

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}