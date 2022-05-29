import {Stack, StackProps} from 'aws-cdk-lib';
import {Construct} from 'constructs';
import {EmailSubscription} from "aws-cdk-lib/aws-sns-subscriptions";
import { Topic } from 'aws-cdk-lib/aws-sns';

export class SnsStack extends Stack {

    topic: Topic

    constructor(scope: Construct, id: string, props?: StackProps) {
        super(scope, id, props);

        const emailSubscription = new EmailSubscription('igormgomes94@gmail.com', {
            json: true
        })

        const topic = new Topic(this, id, {
            topicName: 'payment-event'
        });
        topic.addSubscription(emailSubscription)

        this.topic = topic
    }
}