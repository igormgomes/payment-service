package br.com.developers

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PaymentReceiptServiceApplication

fun main(args: Array<String>) {
	runApplication<PaymentReceiptServiceApplication>(*args)
}