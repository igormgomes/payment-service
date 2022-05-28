package br.com.developers.payment.converter

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("Big decimal converter test")
class BigDecimalConverterTest {

    private lateinit var bigDecimalConverter: BigDecimalConverter

    @BeforeEach
    fun before () {
        this.bigDecimalConverter = BigDecimalConverter()
    }

    @Test
    fun `Should test the conversion BigDecimal to String`() {
        val value = BigDecimal.ONE

        val convert = this.bigDecimalConverter.convert(value)

        assertThat(value.toString(), `is`(equalTo(convert)))
    }

    @Test
    fun `Should test the conversion String to BigDecimal`() {
        val value = BigDecimal.ONE

        val convert = this.bigDecimalConverter.unconvert(value.toString())

        assertThat(value, `is`(equalTo(convert)))
    }
}