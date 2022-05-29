import {CfnOutput, Duration, Stack, StackProps} from 'aws-cdk-lib';
import {Construct} from 'constructs';
import {EmailSubscription, SqsSubscription} from "aws-cdk-lib/aws-sns-subscriptions";
import {Topic} from 'aws-cdk-lib/aws-sns';
import { Queue } from 'aws-cdk-lib/aws-sqs';

export class SnsStack extends Stack {

    topic: Topic

    constructor(scope: Construct, id: string, props?: StackProps) {
        super(scope, id, props);

        const queue = new Queue(this, 'test-queue', {
            visibilityTimeout: Duration.seconds(30),
            queueName: `test-queue`
        });

        const emailSubscription = new EmailSubscription('igormgomes94@gmail.com', {
            json: true
        })

        const topic = new Topic(this, id, {
            topicName: 'payment-event'
        });
        topic.addSubscription(emailSubscription)
        topic.addSubscription(new SqsSubscription(queue))

        this.topic = topic

        new CfnOutput(this, 'payment-event-topic-arn-cfn-output', {
            value: topic.topicArn,
            exportName: 'payment-event-topic-arn',
            description: 'Payment event topic arn'
        })
    }
}