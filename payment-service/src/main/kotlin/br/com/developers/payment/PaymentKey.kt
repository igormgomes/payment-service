package br.com.developers.payment

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey

class PaymentKey {
    @DynamoDBHashKey(attributeName = "pk")
    var pk: String? = null

    @DynamoDBRangeKey(attributeName = "sk")
    var sk: String? = null
}