package br.com.developers.payment.adapters.input.rest

import br.com.developers.payment.application.port.input.DeletePaymentUseCase
import br.com.developers.payment.application.port.input.FindPaymentUseCase
import br.com.developers.payment.application.port.input.SavePaymentUseCase
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
class PaymentController(
    private val savePaymentUseCase: SavePaymentUseCase,
    private val findPaymentUseCase: FindPaymentUseCase,
    private val deletePaymentUseCase: DeletePaymentUseCase
) {

    @PostMapping
    fun save(@Valid @RequestBody paymentRequest: PaymentRequest): ResponseEntity<PaymentResponse> {
        val payment = paymentRequest.toPayment()
        this.savePaymentUseCase.save(payment)

        return ResponseEntity.created(URI.create("/api/payment/${payment.pk}"))
            .body(payment.toResponse())
    }

    @GetMapping("/{id}")
    fun findById(@PathVariable("id") id: String): ResponseEntity<PaymentResponse> {
        val payment = this.findPaymentUseCase.findById(id)

        return ResponseEntity.ok(payment.toResponse())
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable("id") id: String): ResponseEntity<Void> {
        this.deletePaymentUseCase.delete(id)

        return ResponseEntity.noContent().build()
    }
}
