import {CfnOutput, Duration, Fn, RemovalPolicy, Stack, StackProps} from 'aws-cdk-lib';
import {Construct} from 'constructs';
import {Cluster, ContainerImage, LogDrivers} from "aws-cdk-lib/aws-ecs";
import {ApplicationLoadBalancedFargateService} from "aws-cdk-lib/aws-ecs-patterns";
import {LogGroup} from "aws-cdk-lib/aws-logs";
import {Policy, PolicyStatement} from 'aws-cdk-lib/aws-iam';

export class PaymentReceiptServiceStack extends Stack {

    constructor(scope: Construct, id: string, props?: StackProps, cluster?: Cluster) {
        super(scope, id, props);

        const paymentReceiptQueueName = Fn.importValue('payment-receipt-queue-name');
        const paymentReceiptQueueArn = Fn.importValue('payment-receipt-queue-arn');
        const paymentReceiptDynamoDbArn = Fn.importValue('payment-receipt-dynamodb-arn');

        const paymentReceiptService = new ApplicationLoadBalancedFargateService(this, id, {
            cluster: cluster,
            serviceName: 'payment-receipt-service',
            cpu: 512,
            desiredCount: 1,
            listenerPort: 8080,
            memoryLimitMiB: 1024,
            taskImageOptions: {
                containerName: 'payment-receipt-service',
                image: ContainerImage.fromRegistry('igormgomes/payment-receipt-service:latest'),
                environment: {
                    'PAYMENT_RECEIPT_QUEUE_NAME': paymentReceiptQueueName,
                },
                containerPort: 8080,
                logDriver: LogDrivers.awsLogs({
                    logGroup: new LogGroup(this, 'log', {
                        logGroupName: 'payment-receipt-service',
                        removalPolicy: RemovalPolicy.DESTROY
                    }),
                    streamPrefix: 'aws/logs/developers'
                })
            },
            publicLoadBalancer: true
        })

        paymentReceiptService.targetGroup.configureHealthCheck({
            path: '/actuator/health',
            port: '8080',
            healthyHttpCodes: '200'
        })

        const scalableTaskCount = paymentReceiptService.service.autoScaleTaskCount({
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
            value: paymentReceiptService.loadBalancer.loadBalancerDnsName,
            exportName: 'payment-receipt-service-load-balancer',
            description: 'Payment receipt service load balancer dns'
        })

/*        const role = new Role(this, 'Role', {
            assumedBy:  new ServicePrincipal('lambda.amazonaws.com'),
            roleName: ''
        });

        role.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName('AmazonSQSFullAccess'))*/

        const sqsPolicyStatement = new PolicyStatement({
            actions: [
                'sqs:*'
            ],
            resources: [
                paymentReceiptQueueArn
            ],
        });
        const dynamodbPolicyStatement = new PolicyStatement({
            actions: [
                'dynamodb:*'
            ],
            resources: [
                paymentReceiptDynamoDbArn
            ],
        });
        const paymentReceiptPolicy = new Policy(this, 'payment-service-policy');
        paymentReceiptPolicy.addStatements(sqsPolicyStatement)
        paymentReceiptPolicy.addStatements(dynamodbPolicyStatement)
        paymentReceiptService.taskDefinition.taskRole.attachInlinePolicy(paymentReceiptPolicy)
    }
}