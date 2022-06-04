import {CfnOutput, RemovalPolicy, Stack, StackProps} from 'aws-cdk-lib';
import {Construct} from 'constructs';
import {AttributeType, BillingMode, StreamViewType, Table} from "aws-cdk-lib/aws-dynamodb";

export class PaymentDynamodbStack extends Stack {

    table: Table

    constructor(scope: Construct, id: string, props?: StackProps) {
        super(scope, id, props);

        this.table = new Table(this, id, {
            tableName: 'payment',
            billingMode: BillingMode.PAY_PER_REQUEST,
            partitionKey: {
                name: 'pk',
                type: AttributeType.STRING
            },
            sortKey: {
                name: 'sk',
                type: AttributeType.STRING
            },
            //stream: StreamViewType.NEW_AND_OLD_IMAGES,
            removalPolicy: RemovalPolicy.DESTROY
        })

        new CfnOutput(this, 'payment-dynamodb-arn-cfn-output', {
            value: this.table.tableArn,
            exportName: 'payment-dynamodb-arn',
            description: 'Payment dynamodb arn'
        })
    }
}