import {Stack, StackProps} from 'aws-cdk-lib';
import {Construct} from 'constructs';
import {Vpc} from "aws-cdk-lib/aws-ec2";
import {Cluster} from "aws-cdk-lib/aws-ecs";

export class ClusterStack extends Stack {

    cluster: Cluster

    constructor(scope: Construct, id: string, props?: StackProps, vpc?: Vpc) {
        super(scope, id, props);

        this.cluster = new Cluster(this, id, {
            clusterName: 'payment-cluster',
            vpc: vpc
        })
    }
}