package br.com.developers.receipt

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey

data class PaymentReceiptKey(
    @DynamoDBHashKey(attributeName = "pk")
    var pk: String? = null
)