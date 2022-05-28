package br.com.developers.payment.converter

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter
import java.math.BigDecimal

class BigDecimalConverter : DynamoDBTypeConverter<String, BigDecimal> {
    override fun convert(value: BigDecimal): String {
        return value.toString()
    }

    override fun unconvert(`object`: String): BigDecimal {
        return BigDecimal(`object`)
    }
}