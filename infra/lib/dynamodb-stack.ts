import {RemovalPolicy, Stack, StackProps} from 'aws-cdk-lib';
import {Construct} from 'constructs';
import {AttributeType, BillingMode, StreamViewType, Table} from "aws-cdk-lib/aws-dynamodb";

export class DynamodbStack extends Stack {

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
    }
}