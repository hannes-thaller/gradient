import pathlib

import yaml
from aws_cdk import core, aws_iam, aws_codebuild, aws_codeartifact

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


class PythonServiceDomainStack(core.NestedStack):
    def __init__(self, scope, id: str = "build-sourceflow-gradient-service-domain-python", **kwargs):
        super().__init__(scope, id, **kwargs)

        policy = aws_iam.ManagedPolicy.from_managed_policy_arn(
            self,
            "policy-codeartifact-gradient-service-domain-python",
            "arn:aws:iam::aws:policy/AWSCodeArtifactAdminAccess"
        )

        role = aws_iam.Role(
            scope=self,
            id=f"role-gradient-service-domain-python",
            role_name=f"role-gradient-service-domain-python",
            path="/",
            assumed_by=aws_iam.ServicePrincipal("codebuild.amazonaws.com"),
            managed_policies=[policy]
        )

        path_buildspec = pathlib.Path(__file__).parent.parent.parent.joinpath("resources", "gradient-python", "buildspec-gradient-service-domain.yaml")
        with path_buildspec.open("r") as f:
            buildspec = yaml.safe_load(f)

        project = aws_codebuild.Project(
            self,
            "codebuild-gradient-service-domain-python",
            project_name="sourceflow-gradient-service-domain",
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
                        aws_codebuild.EventAction.PUSH,
                        aws_codebuild.EventAction.PULL_REQUEST_CREATED,
                        aws_codebuild.EventAction.PULL_REQUEST_UPDATED,
                    ).and_file_path_is(f"^gradient-service-domain")
                ]
            ),
            build_spec=aws_codebuild.BuildSpec.from_object(buildspec),
            role=role
        )
