package br.com.developers.payment

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/payment")
class PaymentController(private val paymentService: PaymentService) {

    @PostMapping
    fun save(@Valid @RequestBody paymentRequest: PaymentRequest): ResponseEntity<Payment> {
        val payment = paymentRequest.toPayment()
        this.paymentService.save(payment)

        return ResponseEntity.created(URI.create("/api/payment/${payment.pk}"))
            .body(payment)
    }

    @GetMapping("/{id}")
    fun findById(@PathVariable("id") id: String): ResponseEntity<Payment> {
        val payment = this.paymentService.findById(id)

        return ResponseEntity.ok(payment)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable("id") id: String): ResponseEntity<Payment> {
        this.paymentService.delete(id)

        return ResponseEntity.noContent().build()
    }
}