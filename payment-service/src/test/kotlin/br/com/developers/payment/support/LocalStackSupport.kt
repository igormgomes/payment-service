package br.com.developers.payment.support

import org.testcontainers.containers.localstack.LocalStackContainer

fun LocalStackContainer.execAwsLocal(vararg command: String) {
    var lastError: String? = null
    repeat(15) {
        val result = execInContainer(*(arrayOf("awslocal") + command))
        if (result.exitCode == 0) return
        lastError = result.stderr
        Thread.sleep(1000)
    }
    error("awslocal ${command.joinToString(" ")} failed after retries: $lastError")
}
