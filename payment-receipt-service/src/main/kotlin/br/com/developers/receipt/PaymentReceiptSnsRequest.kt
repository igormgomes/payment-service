package br.com.developers.receipt

import com.fasterxml.jackson.annotation.JsonProperty

data class PaymentReceiptSnsRequest(
    @JsonProperty("Message")
    val message: String?,
    @JsonProperty("Type")
    val type: String?,
    @JsonProperty("TopicArn")
    val topicArn: String?,
    @JsonProperty("Timestamp")
    val timestamp: String?,
    @JsonProperty("MessageId")
    val messageId: String?
)