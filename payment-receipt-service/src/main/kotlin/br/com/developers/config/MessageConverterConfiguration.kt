package br.com.developers.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
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

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        return ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerModule(KotlinModule.Builder()
                .build())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
    }
}