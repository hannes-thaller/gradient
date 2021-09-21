import constructs
from aws_cdk import core, aws_codebuild, aws_ecr, aws_s3


class InfrastructureStack(core.Stack):
    def __init__(self, scope: constructs.Construct, id: str = "gradient-infrastructure", **kwargs):
        super().__init__(scope, id, **kwargs)


class BuildStack(core.NestedStack):
    def __init__(self, scope: constructs.Construct, id: str, config: dict, build_spec, **kwargs):
        super().__init__(scope, id, **kwargs)

        config_cb = config["code-build"]

        env = self.create_build_env()
        filters = self.create_filter_groups()
        source = self.create_source(config_cb["owner"], config_cb["repo"], filters)
        cache = self.create_cache_bucket()
        project = self.create_code_build_project(config_cb["name"], config_cb["description"], env, source, build_spec, cache)

        for config_module in config["modules"]:
            config_ecr = config_module["ecr"]
            self.create_repository(config_ecr["name"], config_ecr["max_image_count"], project)

    @staticmethod
    def create_build_env():
        return aws_codebuild.BuildEnvironment(
            compute_type=aws_codebuild.ComputeType.SMALL,
            build_image=aws_codebuild.LinuxBuildImage.AMAZON_LINUX_2_3
        )

    @staticmethod
    def create_filter_groups():
        return [
            aws_codebuild.FilterGroup.in_event_of(
                aws_codebuild.EventAction.PUSH,
                aws_codebuild.EventAction.PULL_REQUEST_CREATED,
                aws_codebuild.EventAction.PULL_REQUEST_UPDATED
            )
        ]

    @staticmethod
    def create_source(owner, repo, filters):
        return aws_codebuild.Source.bit_bucket(
            owner=owner,
            repo=repo,
            webhook=True,
            webhook_filters=filters
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
            cache=aws_codebuild.Cache.bucket(cache)
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
