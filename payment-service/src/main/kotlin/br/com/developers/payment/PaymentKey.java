package br.com.developers.payment;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;

public class PaymentKey {
    @DynamoDBHashKey(attributeName = "pk")
    private String pk;
    @DynamoDBRangeKey(attributeName = "sk")
    private String sk;

    public String getPk() {
        return pk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    public String getSk() {
        return sk;
    }

    public void setSk(String sk) {
        this.sk = sk;
    }
}