import attr
from aws_cdk import aws_codepipeline, core


@attr.s(frozen=True)
class InfrastructureService:
    def create_stacks(self):
        pass

    def create_roles(self):
        pass

    def create(self):
        pass


@attr.s(frozen=True)
class BuildService:
    def create_repositories(self):
        pass

    def create_roles(self):
        pass

    def create_domains(self):
        pass

    def create_codebuild(self):
        pipeline = aws_codepipeline.Pipeline("gradient-python", cross_account_keys=False)

    def create_all(self):
        self.create_codebuild()


class BuildpipelinePythonStack(core.Stack):
    def __init__(self, scope: core.Construct, id: str, **kwargs) -> None:
        super().__init__(scope, id, **kwargs)
        self.create_buckets()

    def create_buckets(self):
        aws_codepipeline.Pipeline(self, "gradient-pipeline-python")
