import constructs
from aws_cdk import core, aws_s3


class BuildStack(core.Stack):
    def __init__(self, scope: constructs.Construct, id: str, namespace: str):
        super().__init__(scope, id)

        self.bucket = self.create_source_bucket(namespace)

    def create_source_bucket(self, namespace: str):
        return aws_s3.Bucket(
            self, "SourceBucket",
            bucket_name=f"{namespace}-{core.Aws.ACCOUNT_ID}",
            versioned=True,
            removal_policy=core.RemovalPolicy.DESTROY
        )
