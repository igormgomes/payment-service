package br.com.developers.receipt

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/payment-receipt")
class PaymentController(private val paymentReceiptService: PaymentReceiptService) {

    @GetMapping("/{id}")
    fun findById(@PathVariable("id") id: String): ResponseEntity<Any> {
        val payment = this.paymentReceiptService.findById(id)

        return ResponseEntity.ok(payment)
    }
}