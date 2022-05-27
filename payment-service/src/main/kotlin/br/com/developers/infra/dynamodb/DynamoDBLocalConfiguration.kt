package br.com.developers.infra.dynamodb

import br.com.developers.payment.PaymentRepository
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig
import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Profile("local")
@Configuration
@EnableDynamoDBRepositories(basePackageClasses = [PaymentRepository::class])
class DynamoDBLocalConfiguration {

    final val ENDPOINT: String = "http://localhost:4566"

    @Bean
    fun endpointConfiguration(): EndpointConfiguration {
        return EndpointConfiguration(ENDPOINT, Regions.US_EAST_1.getName())
    }

    @Bean
    @Primary
    fun dynamoDbMapperConfig(): DynamoDBMapperConfig {
        return DynamoDBMapperConfig.DEFAULT
    }

    @Bean
    @Primary
    fun amazonDynamoDB(endpointConfiguration: EndpointConfiguration): AmazonDynamoDB {
        return AmazonDynamoDBClient.builder()
            .withEndpointConfiguration(endpointConfiguration)
            .build()
    }

    @Bean
    @Primary
    fun dynamoDbMapper(amazonDynamoDB: AmazonDynamoDB, dynamoDBMapperConfig: DynamoDBMapperConfig): DynamoDBMapper {
        return DynamoDBMapper(amazonDynamoDB, dynamoDBMapperConfig)
    }
}