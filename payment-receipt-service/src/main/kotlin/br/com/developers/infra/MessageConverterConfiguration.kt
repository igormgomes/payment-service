package br.com.developers.infra

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.beans.factory.ObjectProvider
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
    fun jacksonConverter(objectMapper: ObjectProvider<ObjectMapper?>): MappingJackson2MessageConverter? {
        val mappingJackson2MessageConverter = MappingJackson2MessageConverter()
        mappingJackson2MessageConverter.objectMapper = objectMapper.getIfAvailable { objectMapper() }
        mappingJackson2MessageConverter.serializedPayloadClass = String::class.java
        mappingJackson2MessageConverter.isStrictContentTypeMatch = false
        return mappingJackson2MessageConverter
    }

    private fun objectMapper(): ObjectMapper? {
        return ObjectMapper().registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
    }
}