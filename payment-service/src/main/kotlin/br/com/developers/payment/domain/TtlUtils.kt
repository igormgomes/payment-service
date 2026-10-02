package br.com.developers.payment.domain

import java.time.Duration
import java.time.Instant

fun ttlOf60Minutes(): Long = Instant.now().plus(Duration.ofMinutes(60)).epochSecond
