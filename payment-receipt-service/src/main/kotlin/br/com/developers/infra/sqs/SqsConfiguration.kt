package br.com.developers.infra.sqs

import io.awspring.cloud.messaging.config.QueueMessageHandlerFactory
import io.awspring.cloud.messaging.listener.support.AcknowledgmentHandlerMethodArgumentResolver
import io.awspring.cloud.messaging.listener.support.VisibilityHandlerMethodArgumentResolver
import io.awspring.cloud.messaging.support.NotificationMessageArgumentResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.converter.CompositeMessageConverter
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.converter.SimpleMessageConverter
import org.springframework.messaging.handler.annotation.support.HeadersMethodArgumentResolver
import org.springframework.messaging.handler.annotation.support.PayloadMethodArgumentResolver

@Configuration
class SqsConfiguration {

    @Bean
    fun queueMessageHandlerFactory(jacksonConverter: MappingJackson2MessageConverter): QueueMessageHandlerFactory {
        val queueMessageHandlerFactory = QueueMessageHandlerFactory()
        val compositeMessageConverter = CompositeMessageConverter(listOf(jacksonConverter, SimpleMessageConverter()))

        queueMessageHandlerFactory.setArgumentResolvers(
            listOf(
                HeadersMethodArgumentResolver(),
                AcknowledgmentHandlerMethodArgumentResolver("Acknowledgment"),
                VisibilityHandlerMethodArgumentResolver("Visibility"),
                NotificationMessageArgumentResolver(compositeMessageConverter),
                PayloadMethodArgumentResolver(compositeMessageConverter)
            )
        )
        return queueMessageHandlerFactory
    }
}