#!/bin/sh

awslocal sqs create-queue --queue-name payment-receipt

echo "Initialized."
echo "######################.###########.###########.###########.###########.###########.###########.###########.###########.."