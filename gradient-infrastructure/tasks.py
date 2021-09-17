import logging
import pathlib
import shutil

from aws_cdk import core
from invoke import task

from gradient.infrastructure import components

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
def build(c):
    logger.info("Building")

    app = core.App()
    stack = components.BuildStack(app, "python-build-pipeline", "gradient-pipeline-python")
    app.synth()

    logger.info("Build done")


@task
def test(c):
    logger.info("Testing")

    c.run(f"conda run --live-stream -n {project_name} python -m pytest tests")

    logger.info("Test done")


@task
def clean(c):
    shutil.rmtree("build", ignore_errors=True)
    shutil.rmtree("dist", ignore_errors=True)
    shutil.rmtree("gradient_service_api.egg-info", ignore_errors=True)
    shutil.rmtree(".pytest_cache", ignore_errors=True)


@task(pre=[clean, build])
def publish(c):
    logger.info("Publishing")

    c.run(f"python setup.py sdist bdist_wheel")
    logger.info(f"Generated the package")

    c.run(f"aws codeartifact login --tool twine --domain sourceflow --repository python")
    c.run(f"conda run --live-stream -n {project_name} twine upload --repository codeartifact dist/*")

    logger.info("Done publishing")


@task
def run(c):
    pass
