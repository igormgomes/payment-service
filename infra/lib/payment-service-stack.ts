import {CfnOutput, Duration, RemovalPolicy, Stack, StackProps} from 'aws-cdk-lib';
import {Construct} from 'constructs';
import {Cluster, ContainerImage, LogDrivers} from "aws-cdk-lib/aws-ecs";
import {ApplicationLoadBalancedFargateService} from "aws-cdk-lib/aws-ecs-patterns";
import {LogGroup} from "aws-cdk-lib/aws-logs";
import {Table} from "aws-cdk-lib/aws-dynamodb";
import {Topic} from "aws-cdk-lib/aws-sns";

export class PaymentServiceStack extends Stack {

    constructor(scope: Construct, id: string, props?: StackProps, cluster?: Cluster, table?: Table, topic?: Topic) {
        super(scope, id, props);

        if (typeof table == 'undefined') {
            throw 'Invalid Table'
        }

        if (typeof topic == 'undefined') {
            throw 'Invalid Topic'
        }

        const paymentService = new ApplicationLoadBalancedFargateService(this, id, {
            cluster: cluster,
            serviceName: 'payment-service',
            cpu: 512,
            desiredCount: 1,
            listenerPort: 8080,
            memoryLimitMiB: 1024,
            taskImageOptions: {
                containerName: 'payment',
                image: ContainerImage.fromRegistry('igormgomes/payment-service:1.0.1'),
                environment: {
                    'PAYMENT_TOPIC_NAME': topic.topicArn,
                },
                containerPort: 8080,
                logDriver: LogDrivers.awsLogs({
                    logGroup: new LogGroup(this, 'log', {
                        logGroupName: 'payment-service',
                        removalPolicy: RemovalPolicy.DESTROY
                    }),
                    streamPrefix: 'payment'
                })
            },
            publicLoadBalancer: true
        })

        paymentService.targetGroup.configureHealthCheck({
            path: '/actuator/health',
            port: '8080',
            healthyHttpCodes: '200'
        })

        const scalableTaskCount = paymentService.service.autoScaleTaskCount({
            minCapacity: 1,
            maxCapacity: 10
        })
        scalableTaskCount.scaleOnCpuUtilization('payment-service-cpu-auto-scaling', {
            targetUtilizationPercent: 60,
            scaleInCooldown: Duration.seconds(120),
            scaleOutCooldown: Duration.seconds(120)
        })
        scalableTaskCount.scaleOnMemoryUtilization('payment-service-memory-auto-scaling', {
            targetUtilizationPercent: 80,
            scaleInCooldown: Duration.seconds(120),
            scaleOutCooldown: Duration.seconds(120)
        })

        new CfnOutput(this, 'payment-service-cfn-output', {
            value: paymentService.loadBalancer.loadBalancerDnsName,
            exportName: 'payment-service-load-balancer',
            description: 'Payment service load balancer dns'
        })


        //paymentService.taskDefinition.taskRole.attachInlinePolicy()

        table.grantFullAccess(paymentService.taskDefinition.taskRole)
        topic.grantPublish(paymentService.taskDefinition.taskRole)
    }
}