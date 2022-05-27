package br.com.developers.infra.sqs

import io.awspring.cloud.messaging.config.QueueMessageHandlerFactory
import io.awspring.cloud.messaging.support.NotificationMessageArgumentResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.converter.MessageConverter
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver

@Configuration
class SqsConfiguration {

    @Bean
    fun queueMessageHandlerFactory(messageConverter: MessageConverter): QueueMessageHandlerFactory {
        val factory = QueueMessageHandlerFactory()
        factory.setArgumentResolvers(
            listOf<HandlerMethodArgumentResolver>(
                NotificationMessageArgumentResolver(
                    messageConverter
                )
            )
        )
        return factory
    }

    @Bean
    protected fun messageConverter(): MessageConverter {
        val converter = MappingJackson2MessageConverter()
        converter.serializedPayloadClass = String::class.java
        converter.isStrictContentTypeMatch = false
        return converter
    }
}