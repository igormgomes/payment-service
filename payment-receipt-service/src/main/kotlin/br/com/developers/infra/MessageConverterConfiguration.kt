package br.com.developers.infra

import com.fasterxml.jackson.databind.*
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.messaging.converter.MappingJackson2MessageConverter

@Configuration
class MessageConverterConfiguration {

    @Bean
    @Primary
    @ConditionalOnMissingBean
    fun jacksonConverter(objectMapper: ObjectMapper): MappingJackson2MessageConverter? {
        val mappingJackson2MessageConverter = MappingJackson2MessageConverter()
        mappingJackson2MessageConverter.objectMapper = objectMapper
        mappingJackson2MessageConverter.serializedPayloadClass = String::class.java
        mappingJackson2MessageConverter.isStrictContentTypeMatch = false
        return mappingJackson2MessageConverter
    }
}