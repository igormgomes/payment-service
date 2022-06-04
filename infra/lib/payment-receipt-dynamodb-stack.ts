import {CfnOutput, RemovalPolicy, Stack, StackProps} from 'aws-cdk-lib';
import {Construct} from 'constructs';
import {AttributeType, BillingMode, Table} from "aws-cdk-lib/aws-dynamodb";

export class PaymentReceiptDynamodbStack extends Stack {

    table: Table

    constructor(scope: Construct, id: string, props?: StackProps) {
        super(scope, id, props);

        this.table = new Table(this, id, {
            tableName: 'payment-receipt',
            billingMode: BillingMode.PAY_PER_REQUEST,
            partitionKey: {
                name: 'pk',
                type: AttributeType.STRING
            },
            removalPolicy: RemovalPolicy.DESTROY
        })

        new CfnOutput(this, 'payment-receipt-dynamodb-arn-cfn-output', {
            value: this.table.tableArn,
            exportName: 'payment-receipt-dynamodb-arn',
            description: 'Payment receipt dynamodb arn'
        })
    }
}