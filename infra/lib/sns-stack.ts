import {CfnOutput, Duration, Stack, StackProps} from 'aws-cdk-lib';
import {Construct} from 'constructs';
import {EmailSubscription, SqsSubscription} from "aws-cdk-lib/aws-sns-subscriptions";
import {SubscriptionFilter, Topic} from 'aws-cdk-lib/aws-sns';
import {Queue} from 'aws-cdk-lib/aws-sqs';

export class SnsStack extends Stack {

    constructor(scope: Construct, id: string, props?: StackProps) {
        super(scope, id, props);

        const schedulePaymentQueue = new Queue(this, 'schedule-payment-queue-stack', {
            visibilityTimeout: Duration.seconds(30),
            queueName: `schedule-payment`
        });

        const paymentReceiptQueue = new Queue(this, 'payment-receipt-queue-stack', {
            visibilityTimeout: Duration.seconds(30),
            queueName: `payment-receipt`
        });

        const emailSubscription = new EmailSubscription('igormgomes94@gmail.com', {
            json: true
        })

        const topic = new Topic(this, id, {
            topicName: 'payment-event'
        });
        topic.addSubscription(emailSubscription)
        topic.addSubscription(new SqsSubscription(schedulePaymentQueue, {
            filterPolicy: {
                event_type: SubscriptionFilter.stringFilter({
                    allowlist: ['SCHEDULED_PAYMENT']
                })
            }
        }))
        topic.addSubscription(new SqsSubscription(paymentReceiptQueue))

        new CfnOutput(this, 'payment-event-topic-arn-cfn-output', {
            value: topic.topicArn,
            exportName: 'payment-event-topic-arn',
            description: 'Payment event topic arn'
        })

        new CfnOutput(this, 'payment-receipt-queue-arn-cfn-output', {
            value: paymentReceiptQueue.queueArn,
            exportName: 'payment-receipt-queue-arn',
            description: 'Payment receipt queue arn'
        })

        new CfnOutput(this, 'payment-receipt-queue-name-cfn-output', {
            value: paymentReceiptQueue.queueName,
            exportName: 'payment-receipt-queue-name',
            description: 'Payment receipt queue name'
        })
    }
}