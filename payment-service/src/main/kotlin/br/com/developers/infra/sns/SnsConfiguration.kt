package br.com.developers.infra.sns

import com.amazonaws.services.sns.AmazonSNS
import io.awspring.cloud.messaging.core.NotificationMessagingTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class SnsConfiguration {

    @Bean
    @Primary
    fun notificationMessageTemplate(amazonSNS: AmazonSNS): NotificationMessagingTemplate {
        return NotificationMessagingTemplate(amazonSNS)
    }
}