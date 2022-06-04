#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import {VpcStack} from "../lib/vpc-stack";
import {ClusterStack} from "../lib/cluster-stack";
import {PaymentServiceStack} from "../lib/payment-service-stack";
import {PaymentDynamodbStack} from "../lib/payment-dynamodb-stack";
import {SnsStack} from "../lib/sns-stack";
import {PaymentReceiptServiceStack} from "../lib/payment-receipt-service-stack";
import {PaymentReceiptDynamodbStack} from "../lib/payment-receipt-dynamodb-stack";

const app = new cdk.App();

/* If you don't specify 'env', this stack will be environment-agnostic.
 * Account/Region-dependent features and context lookups will not work,
 * but a single synthesized template can be deployed anywhere. */

/* Uncomment the next line to specialize this stack for the AWS Account
 * and Region that are implied by the current CLI configuration. */
// env: { account: process.env.CDK_DEFAULT_ACCOUNT, region: process.env.CDK_DEFAULT_REGION },

/* Uncomment the next line if you know exactly what Account and Region you
 * want to deploy the stack to. */
// env: { account: '123456789012', region: 'us-east-1' },

/* For more information, see https://docs.aws.amazon.com/cdk/latest/guide/environments.html */

const vpcStack = new VpcStack(app, 'vpc-stack')
const clusterStack = new ClusterStack(app, 'cluster-stack', {}, vpcStack.vpc)
clusterStack.addDependency(vpcStack)

const paymentDynamodbStack = new PaymentDynamodbStack(app, `payment-dynamodb-stack`)
const paymentReceiptDynamodbStack = new PaymentReceiptDynamodbStack(app, `payment-receipt-dynamodb-stack`)

const snsStack = new SnsStack(app, 'sns-stack')

const paymentServiceStack  = new PaymentServiceStack(app, 'payment-service-stack', {}, clusterStack.cluster)
paymentServiceStack.addDependency(clusterStack)
paymentServiceStack.addDependency(paymentDynamodbStack)
paymentServiceStack.addDependency(snsStack)

const paymentReceiptServiceStack  = new PaymentReceiptServiceStack(app, 'payment-receipt-service-stack', {}, clusterStack.cluster)
paymentReceiptServiceStack.addDependency(clusterStack)
paymentReceiptServiceStack.addDependency(paymentReceiptDynamodbStack)
paymentReceiptServiceStack.addDependency(snsStack)