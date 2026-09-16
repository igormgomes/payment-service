package br.com.developers.receipt

import java.time.Duration
import java.time.Instant

fun ttlOf60Minutes(): Long = Instant.now().plus(Duration.ofMinutes(60)).epochSecond
