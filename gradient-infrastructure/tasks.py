import logging
import pathlib
import shutil
from concurrent import futures

from aws_cdk import aws_iam, core, aws_codebuild
from invoke import task

project_name = "gradient-infrastructure"

logging.basicConfig(format="%(asctime)s - %(name)s - %(levelname)s - %(message)s", level=logging.INFO)
logger = logging.getLogger(project_name)

dir_build = pathlib.Path("build")
dir_project = pathlib.Path(__file__).parent


@task
def install(c):
    logger.info("Installing")

    with c.prefix(f"source .env/bin/activate"):
        c.run(f"pip install -r requirements.txt")

    logger.info("Installing done")


@task
def build(c):
    from gradient.infrastructure import components

    app = core.App()
    stack_infra = components.InfrastructureStack(app)
    components.PythonServiceStack(stack_infra)
    components.JVMServiceStack(stack_infra)
    app.synth()


@task
def test(c):
    logger.info("Testing")

    c.run("cdk doctor")

    logger.info("Test done")


@task
def clean(c, force=False):
    import boto3
    from botocore import exceptions

    shutil.rmtree("cdk.out", ignore_errors=True)

    # delete ECR
    names_repo = [it["ecr"]["name"] for repo in c.config["repo-stacks"]
                  for it in repo["modules"]]

    client = boto3.client("ecr")

    def delete(name):
        try:
            client.delete_repository(repositoryName=name, force=force)
        except exceptions.ClientError as e:
            if not e.response["Error"]["Code"] == "RepositoryNotFoundException":
                raise e

    with futures.ThreadPoolExecutor() as pool:
        list(pool.map(delete, names_repo))


@task
def publish(c):
    logger.info("Publishing")

    logger.info("Done publishing")


@task(pre=[clean, build])
def publish_infrastructure(c):
    logger.info("Publishing Infrastructure")

    c.run(f"cdk deploy --all")

    logger.info("Done publishing")


@task
def run(c):
    pass
