import pathlib

import yaml
from aws_cdk import core, aws_iam, aws_codebuild, aws_codeartifact, aws_s3

account = "429689067702"
region = "eu-central-1"


class InfrastructureStack(core.Stack):
    def __init__(self, scope, id="gradient-infrastructure", **kwargs):
        super().__init__(scope, id, **kwargs)

        domain = aws_codeartifact.CfnDomain(scope=self,
                                            id="gradient-codeartifact-domain",
                                            domain_name="sourceflow-gradient")

        repo = aws_codeartifact.CfnRepository(scope=self,
                                              id=f"codeartifact-repository-sourceflow-gradient-jvm",
                                              domain_name=domain.domain_name,
                                              domain_owner=account,
                                              external_connections=["public:maven-central"],
                                              repository_name="sourceflow-gradient-jvm",
                                              description="JVM artifacts related to Gradient")
        repo.add_depends_on(domain)

        repo = aws_codeartifact.CfnRepository(scope=self,
                                              id=f"codeartifact-repository-sourceflow-gradient-python",
                                              domain_name=domain.domain_name,
                                              domain_owner=account,
                                              external_connections=["public:pypi"],
                                              repository_name="sourceflow-gradient-python",
                                              description="Python artifacts related to Gradient")
        repo.add_depends_on(domain)


class PythonServiceStack(core.NestedStack):
    def __init__(self, scope, id: str = "build-sourceflow-gradient-service-python", **kwargs):
        super().__init__(scope, id, **kwargs)

        policy = aws_iam.ManagedPolicy.from_managed_policy_arn(
            self,
            "policy-codeartifact-gradient-service-python",
            "arn:aws:iam::aws:policy/AWSCodeArtifactAdminAccess"
        )

        role = aws_iam.Role(
            scope=self,
            id=f"role-gradient-service-python",
            role_name=f"role-gradient-service-python",
            path="/",
            assumed_by=aws_iam.ServicePrincipal("codebuild.amazonaws.com"),
            managed_policies=[policy]
        )

        path_buildspec = pathlib.Path.cwd().joinpath("resources", "gradient-python", "buildspec-gradient-service.yaml")
        with path_buildspec.open("r") as f:
            buildspec = yaml.safe_load(f)

        cache_bucket = aws_s3.Bucket(self, "s3-code-build-cache")

        project = aws_codebuild.Project(
            self,
            "codebuild-gradient-service-python",
            project_name="sourceflow-gradient-service-python",
            environment=aws_codebuild.BuildEnvironment(
                compute_type=aws_codebuild.ComputeType.SMALL,
                build_image=aws_codebuild.LinuxBuildImage.AMAZON_LINUX_2_3
            ),
            timeout=core.Duration.minutes(10),
            source=aws_codebuild.Source.bit_bucket(
                owner="sourceflow-ai",
                repo="gradient-python",
                webhook=True,
                webhook_filters=[
                    aws_codebuild.FilterGroup.in_event_of(
                        aws_codebuild.EventAction.PULL_REQUEST_CREATED,
                        aws_codebuild.EventAction.PULL_REQUEST_UPDATED,
                    ),
                    aws_codebuild.FilterGroup.in_event_of(
                        aws_codebuild.EventAction.PUSH
                    ).and_branch_is("master")
                ]
            ),
            badge=True,
            build_spec=aws_codebuild.BuildSpec.from_object(buildspec),
            role=role,
            cache=aws_codebuild.Cache.bucket(cache_bucket)
        )


class JVMServiceStack(core.NestedStack):
    def __init__(self, scope, id: str = "build-sourceflow-gradient-service-jvm", **kwargs):
        super().__init__(scope, id, **kwargs)

        policy = aws_iam.ManagedPolicy.from_managed_policy_arn(
            self,
            "policy-codeartifact-gradient-service-jvm",
            "arn:aws:iam::aws:policy/AWSCodeArtifactAdminAccess"
        )

        role = aws_iam.Role(
            scope=self,
            id=f"role-gradient-service-jvm",
            role_name=f"role-gradient-service-jvm",
            path="/",
            assumed_by=aws_iam.ServicePrincipal("codebuild.amazonaws.com"),
            managed_policies=[policy]
        )

        path_buildspec = pathlib.Path.cwd().joinpath("resources", "gradient-jvm", "buildspec-gradient-service.yaml")
        with path_buildspec.open("r") as f:
            buildspec = yaml.safe_load(f)

        cache_bucket = aws_s3.Bucket(self, "s3-code-build-cache")

        project = aws_codebuild.Project(
            self,
            "codebuild-gradient-service-jvm",
            project_name="sourceflow-gradient-service-jvm",
            environment=aws_codebuild.BuildEnvironment(
                compute_type=aws_codebuild.ComputeType.SMALL,
                build_image=aws_codebuild.LinuxBuildImage.AMAZON_LINUX_2_3
            ),
            timeout=core.Duration.minutes(10),
            source=aws_codebuild.Source.bit_bucket(
                owner="sourceflow-ai",
                repo="gradient-jvm",
                webhook=True,
                webhook_filters=[
                    aws_codebuild.FilterGroup.in_event_of(
                        aws_codebuild.EventAction.PULL_REQUEST_CREATED,
                        aws_codebuild.EventAction.PULL_REQUEST_UPDATED,
                    ),
                    aws_codebuild.FilterGroup.in_event_of(
                        aws_codebuild.EventAction.PUSH
                    ).and_branch_is("master")
                ]
            ),
            badge=True,
            build_spec=aws_codebuild.BuildSpec.from_object(buildspec),
            role=role,
            cache=aws_codebuild.Cache.bucket(cache_bucket)
        )
