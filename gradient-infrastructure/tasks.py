import logging
import os.path
import pathlib
import shutil
import subprocess
from concurrent import futures

import requests
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
    logger.info("Installing conda")
    dir_conda = pathlib.Path(c.config.buildspec.dir_conda)
    if force or not dir_conda.exists():
        service = BuildPipelineService()
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
def clean(c):
    shutil.rmtree("cdk.out", ignore_errors=True)
    c.run(f"aws ecr delete-repository")


@task
def remove_ecr_repos(c, force=False):
    import boto3
    from botocore import exceptions

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
    logger.info("Publishing")

    c.run(f"python setup.py sdist bdist_wheel")
    logger.info(f"Generated the package")

    logger.info("Done publishing")


@task
def publish_infrastructure(c):
    logger.info("Publishing Infrastructure")

    c.run(f"cdk deploy --all")

    logger.info("Done publishing")


@task
def run(c):
    pass


class BuildPipelineService:
    @staticmethod
    def download_conda(url_conda: str) -> pathlib.Path:
        assert url_conda

        logger.info("Downloading conda installer")

        path_installer = pathlib.Path("miniconda.sh")
        response = requests.get(url_conda)
        with path_installer.open("wb") as f:
            f.write(response.content)

        logger.info("Done downloading conda install")

        return path_installer

    @staticmethod
    def install_conda(path_installer: pathlib.Path, dir_install: pathlib.Path):
        assert os.path.exists(path_installer)

        logger.info(f"Installing conda from {path_installer} into {dir_install}")

        dir_install = pathlib.Path(dir_install)
        dir_install.mkdir(parents=True, exist_ok=True)

        subprocess.run(["sh", "path_installer", "-b", "-p", dir_install.absolute()])
        subprocess.run(["source", dir_install.joinpath("etc", "profile.d", "conda.sh")])
        subprocess.run(["conda", "config", "--set", "always_yes", "yes", "--set", "changeps1", "no"])
        subprocess.run(["conda", "update", "-q", "conda"])
        subprocess.run(["conda", "info", "-a"])
        subprocess.run(["conda", "install", "-c", "conda-forge", "python=3.7", "invoke=1.5.0"])

        logger.info(f"Done installing conda")
