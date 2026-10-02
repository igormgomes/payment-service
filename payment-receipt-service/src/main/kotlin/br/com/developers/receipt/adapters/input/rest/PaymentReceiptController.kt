package br.com.developers.receipt.adapters.input.rest

import br.com.developers.receipt.application.port.input.FindPaymentReceiptUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/payment-receipt")
class PaymentReceiptController(private val findPaymentReceiptUseCase: FindPaymentReceiptUseCase) {

    @GetMapping("/{id}")
    fun findById(@PathVariable("id") id: String): ResponseEntity<Any> {
        val payment = this.findPaymentReceiptUseCase.findById(id)

        return ResponseEntity.ok(payment)
    }
}
