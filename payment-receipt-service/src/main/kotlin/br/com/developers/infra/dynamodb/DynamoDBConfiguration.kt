package br.com.developers.infra.dynamodb

import br.com.developers.receipt.PaymentReceiptRepository
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig
import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Profile("!local")
@Configuration
@EnableDynamoDBRepositories(basePackageClasses = [PaymentReceiptRepository::class])
class DynamoDBConfiguration(@Value("\${cloud.aws.region.static}") private val awsRegion: String) {

    @Bean
    @Primary
    fun dynamoDbMapperConfig(): DynamoDBMapperConfig {
        return DynamoDBMapperConfig.DEFAULT
    }

    @Bean
    @Primary
    fun dynamoDbMapper(amazonDynamoDB: AmazonDynamoDB, dynamoDBMapperConfig: DynamoDBMapperConfig): DynamoDBMapper {
        return DynamoDBMapper(amazonDynamoDB, dynamoDBMapperConfig)
    }

    @Bean
    @Primary
    fun amazonDynamoDB(): AmazonDynamoDB {
        return AmazonDynamoDBClientBuilder.standard()
            .withCredentials(DefaultAWSCredentialsProviderChain())
            .withRegion(Regions.fromName(this.awsRegion).getName())
            .build()
    }
}