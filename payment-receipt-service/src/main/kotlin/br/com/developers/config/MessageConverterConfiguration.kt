package br.com.developers.config

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.messaging.converter.JacksonJsonMessageConverter
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

@Configuration
class MessageConverterConfiguration {

    @Bean
    @Primary
    @ConditionalOnMissingBean
    fun jacksonConverter(objectMapper: JsonMapper): JacksonJsonMessageConverter {
        val jacksonJsonMessageConverter = JacksonJsonMessageConverter(objectMapper)
        jacksonJsonMessageConverter.setSerializedPayloadClass(String::class.java)
        jacksonJsonMessageConverter.setStrictContentTypeMatch(false)
        return jacksonJsonMessageConverter
    }

    @Bean
    @Primary
    fun objectMapper(): JsonMapper {
        return JsonMapper.builder()
            .addModule(KotlinModule.Builder().build())
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .changeDefaultPropertyInclusion { JsonInclude.Value.ALL_NON_NULL }
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build()
    }
}
