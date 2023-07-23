package br.com.developers.receipt;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@DynamoDbBean
public class PaymentReceipt {

    private UUID pk;
    private String status;
    private LocalDate inclusionDate;
    private LocalDate paymentDate;
    private String pixKeyCredit;
    private Long ttl = Instant.now().plus(Duration.ofMinutes(60)).getEpochSecond();

    @DynamoDbPartitionKey
    public UUID getPk() {
        return pk;
    }

    public void setPk(UUID pk) {
        this.pk = pk;
    }

    @DynamoDbAttribute(value = "status")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @DynamoDbAttribute(value = "inclusion_date")
    public LocalDate getInclusionDate() {
        return inclusionDate;
    }

    public void setInclusionDate(LocalDate inclusionDate) {
        this.inclusionDate = inclusionDate;
    }

    @DynamoDbAttribute(value = "payment_date")
    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
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
        return "PaymentReceipt{" +
                "pk=" + pk +
                ", status='" + status + '\'' +
                ", inclusionDate=" + inclusionDate +
                ", paymentDate=" + paymentDate +
                ", pixKeyCredit='" + pixKeyCredit + '\'' +
                ", ttl=" + ttl +
                '}';
    }
}
