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

        const addPaymentQueue = new Queue(this, 'add-payment-queue-stack', {
            visibilityTimeout: Duration.seconds(30),
            queueName: `add-payment`
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
        topic.addSubscription(new SqsSubscription(addPaymentQueue))

        new CfnOutput(this, 'payment-event-topic-arn-cfn-output', {
            value: topic.topicArn,
            exportName: 'payment-event-topic-arn',
            description: 'Payment event topic arn'
        })
    }
}