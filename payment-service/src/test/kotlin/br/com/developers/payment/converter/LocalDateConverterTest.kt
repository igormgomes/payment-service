package br.com.developers.payment.converter

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("Local date converter test")
class LocalDateConverterTest {

    private lateinit var localDateConverter: LocalDateConverter

    @BeforeEach
    fun before () {
        this.localDateConverter = LocalDateConverter()
    }

    @Test
    fun `Should test the conversion LocalDate to String`() {
        val value = LocalDate.now()

        val convert = this.localDateConverter.convert(value)

        assertThat(value.toString(), `is`(equalTo(convert)))
    }

    @Test
    fun `Should test the conversion String to LocalDate`() {
        val value = LocalDate.now()

        val convert = this.localDateConverter.unconvert(value.toString())

        assertThat(value, `is`(equalTo(convert)))
    }
}