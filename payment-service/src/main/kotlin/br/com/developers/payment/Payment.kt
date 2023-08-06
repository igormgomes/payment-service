package br.com.developers.payment;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@DynamoDbBean
public class Payment {

    private UUID pk;
    private String sk;
    private LocalDate date;
    private BigDecimal value;
    private String description;
    private String pixKeyCredit;
    private Long ttl = Instant.now().plus(Duration.ofMinutes(60)).getEpochSecond();

    @DynamoDbPartitionKey
    public UUID getPk() {
        return pk;
    }

    public void setPk(UUID pk) {
        this.pk = pk;
    }

    @DynamoDbSortKey
    public String getSk() {
        return sk;
    }

    public void setSk(String sk) {
        this.sk = sk;
    }

    @DynamoDbAttribute(value = "date")
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    @DynamoDbAttribute(value = "value")
    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    @DynamoDbAttribute(value = "description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @DynamoDbAttribute(value = "pix_key_credit")
    public String getPixKeyCredit() {
        return pixKeyCredit;
    }

    public void setPixKeyCredit(String pixKeyCredit) {
        this.pixKeyCredit = pixKeyCredit;
    }

    @DynamoDbAttribute(value = "ttl")
    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }

    @Override
    public String toString() {
        return "Payment{" +
                "pk=" + pk +
                ", sk='" + sk + '\'' +
                ", date=" + date +
                ", value=" + value +
                ", description='" + description + '\'' +
                ", pixKeyCredit='" + pixKeyCredit + '\'' +
                ", ttl=" + ttl +
                '}';
    }
}
