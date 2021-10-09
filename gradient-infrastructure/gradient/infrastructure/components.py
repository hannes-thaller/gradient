import typing

import constructs
from aws_cdk import core, aws_codebuild, aws_ecr, aws_s3, aws_codeartifact, aws_iam


class InfrastructureStack(core.Stack):
    def __init__(self, scope: constructs.Construct, config: dict, id: str = "gradient-infrastructure", **kwargs):
        super().__init__(scope, id, **kwargs)

        domain = self.create_codeartifact_domain(config["app"]["code-artifact"]["name_domain"])

        for config_tech in config["app"]["tech-stacks"]:
            config_ca = config_tech["code-artifact"]
            repo = self.create_codeartifact_repo(
                name=config_ca["name"],
                description=config_ca["description"],
                domain=domain,
                account=config["app"]["account"],
                external_connections=config_ca["external_connections"]
            )

    def create_codeartifact_domain(self, name):
        return aws_codeartifact.CfnDomain(scope=self,
                                          id="gradient-codeartifact-domain",
                                          domain_name=name)

    def create_codeartifact_repo(self, name, description, domain, account, external_connections):
        repo = aws_codeartifact.CfnRepository(scope=self,
                                              id=f"codeartifact-repository-{name}",
                                              domain_name=domain.domain_name,
                                              domain_owner=account,
                                              external_connections=external_connections,
                                              repository_name=name,
                                              description=description)
        repo.add_depends_on(domain)
        return repo


class BuildStack(core.NestedStack):
    def __init__(self, scope: constructs.Construct, id: str, config: dict, build_spec, **kwargs):
        super().__init__(scope, id, **kwargs)

        config_cb = config["code-build"]

        env = self.create_build_env(config_cb["build_image"])
        filters = self.create_filter_groups(config_cb["location_module"])
        source = self.create_source(config_cb["owner"], config_cb["repo"], filters)
        cache = self.create_cache_bucket()
        role = self.create_code_build_role(config_cb["name"])
        project = self.create_code_build_project(config_cb["name"], config_cb["description"], env, source, build_spec, cache)

        for config_module in config["modules"]:
            config_ecr = config_module["ecr"]
            self.create_repository(config_ecr["name"], config_ecr["max_image_count"], project)

    @staticmethod
    def create_build_env(build_image: typing.Optional[str]):

        if build_image:
            build_image = aws_codebuild.LinuxBuildImage.from_docker_registry(build_image)
        else:
            build_image = aws_codebuild.LinuxBuildImage.AMAZON_LINUX_2_3

        return aws_codebuild.BuildEnvironment(
            compute_type=aws_codebuild.ComputeType.SMALL,
            build_image=build_image
        )

    @staticmethod
    def create_filter_groups(name_project: str):
        return [
            aws_codebuild.FilterGroup.in_event_of(
                aws_codebuild.EventAction.PUSH,
                aws_codebuild.EventAction.PULL_REQUEST_CREATED,
                aws_codebuild.EventAction.PULL_REQUEST_UPDATED,
            ).and_file_path_is(f"^{name_project}")
        ]

    @staticmethod
    def create_source(owner, repo, filters):
        return aws_codebuild.Source.bit_bucket(
            owner=owner,
            repo=repo,
            webhook=True,
            webhook_filters=filters
        )

    def create_code_build_role(self, name):
        statement = aws_iam.PolicyStatement(
            effect=aws_iam.Effect.ALLOW,
            actions=[
                "ecr:BatchCheckLayerAvailability",
                "ecr:CompleteLayerUpload",
                "ecr:DescribeImages",
                "ecr:GetAuthorizationToken",
                "ecr:InitiateLayerUpload",
                "ecr:ListImages",
                "ecr:PutImage",
                "ecr:UploadLayerPart",
                "logs:*",
            ],
            resources=["*"]
        )

        document = aws_iam.PolicyDocument(statements=[statement])

        return aws_iam.Role(
            scope=self,
            id=f"codebuild-role",
            role_name=f"codebuild-role-{name}",
            path="/",
            assumed_by=aws_iam.ServicePrincipal("codebuild.amazonaws.com"),
            inline_policies={
                "CodeBuildAccess": document
            }
        )

    def create_code_build_project(self, name, description, env, source, buildspec, cache):

        project = aws_codebuild.Project(
            self,
            "code-build",
            project_name=name,
            description=description,
            badge=True,
            environment=env,
            timeout=core.Duration.minutes(10),
            source=source,
            build_spec=aws_codebuild.BuildSpec.from_object(buildspec),
            cache=aws_codebuild.Cache.bucket(cache),

        )
        cache.grant_read_write(project)
        return project

    def create_repository(self, name, max_image_count, code_build):
        repo = aws_ecr.Repository(self, f"ecr-{name}", repository_name=name)
        repo.add_lifecycle_rule(max_image_count=max_image_count)
        repo.grant_pull_push(code_build)
        return repo

    def create_cache_bucket(self):
        bucket = aws_s3.Bucket(self, "s3-code-build-cache")
        return bucket
