package br.com.developers.infra.sqs

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory
import io.awspring.cloud.sqs.listener.SqsContainerOptionsBuilder
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementOrdering
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.sqs.SqsAsyncClient

@Configuration
class SqsConfiguration {

    @Bean
    fun defaultSqsListenerContainerFactory(sqsAsyncClient: SqsAsyncClient): SqsMessageListenerContainerFactory<Any> {
        return SqsMessageListenerContainerFactory
            .builder<Any>()
            .configure { options: SqsContainerOptionsBuilder ->
                options
                    .acknowledgementMode(AcknowledgementMode.MANUAL)
                    .acknowledgementOrdering(AcknowledgementOrdering.ORDERED)
            }
            .sqsAsyncClient(sqsAsyncClient)
            .build()
    }
}