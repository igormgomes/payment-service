package br.com.developers.receipt.converter

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter
import java.time.LocalDate

class LocalDateConverter : DynamoDBTypeConverter<String, LocalDate> {

    override fun convert(localDate: LocalDate): String {
        return localDate.toString()
    }

    override fun unconvert(value: String): LocalDate {
        return LocalDate.parse(value)
    }
}