import logging
import pathlib
import shutil
from concurrent import futures

from invoke import task

logging.basicConfig(format="%(asctime)s - %(name)s - %(levelname)s - %(message)s", level=logging.INFO)
logger = logging.getLogger("gradient-infrastructure")

project_name = "gradient-infrastructure"

dir_build = pathlib.Path("build")
dir_project = pathlib.Path(__file__).parent


@task
def install(c):
    logger.info("Installing")

    c.run(f"conda env create --force -f requirements.yaml")

    logger.info("Installing done")


@task
def install_conda(c, force=False):
    from gradient.infrastructure import services

    logger.info("Installing conda")

    dir_conda = pathlib.Path(c.config.buildspec.dir_conda)
    if force or not dir_conda.exists():
        service = services.InfrastructureService()
        path_installer = service.download_conda(c.config.buildspec.url_conda)
        service.install_conda(path_installer, dir_conda)

    logger.info("Installing done")


@task
def build(c):
    from gradient.infrastructure import services

    logger.info("Building")

    service = services.InfrastructureService()
    service.create_infrastructure(c.config)

    logger.info("Build done")


@task
def test(c):
    logger.info("Testing")

    c.run(f"conda run --live-stream -n {project_name} python -m pytest tests")

    logger.info("Test done")


@task
def clean(c, force=False):
    import boto3
    from botocore import exceptions

    shutil.rmtree("cdk.out", ignore_errors=True)

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


@task(pre=[clean, build])
def publish(c):
    logger.info("Publishing Infrastructure")

    c.run(f"cdk deploy --all")

    logger.info("Done publishing")


@task
def run(c):
    pass
