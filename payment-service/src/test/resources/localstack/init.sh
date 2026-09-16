#!/bin/sh

awslocal dynamodb create-table \
    --table-name payment \
    --attribute-definitions \
        AttributeName=pk,AttributeType=S \
        AttributeName=sk,AttributeType=S \
    --key-schema \
        AttributeName=pk,KeyType=HASH \
        AttributeName=sk,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST

awslocal sns create-topic --name payment-event

echo "Initialized."
