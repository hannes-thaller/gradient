import logging
import pathlib
import shutil
from concurrent import futures

from invoke import task

project_name = "gradient-infrastructure"

logging.basicConfig(format="%(asctime)s - %(name)s - %(levelname)s - %(message)s", level=logging.INFO)
logger = logging.getLogger(project_name)

dir_build = pathlib.Path("build")
dir_project = pathlib.Path(__file__).parent


@task
def install(c, distilled=False):
    logger.info("Installing")

    if distilled:
        c.run(f"conda env create --force -f requirements.yaml")
        c.run(f"conda run -n {project_name} conda env export > requirements/requirements.yaml")
        c.run(f"conda run -n {project_name} pip list --format=freeze > requirements/requirements.txt")
    else:
        c.run(f"conda env create --force -f requirements/requirements.yaml")

    logger.info("Installing done")


@task
def build(c):
    from aws_cdk import core
    from gradient_infrastructure import components

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
def clean(c):
    logger.info("Clean")

    logger.info("Cleaning done")


@task
def publish(c):
    logger.info("Publishing")

    logger.info("Done publishing")


@task
def clean_infrastructure(c):
    logger.info("Destroying Infrastructure")

    c.run("cdk destroy -y")

    logger.info("Done destroying")



@task(pre=[clean, build])
def publish_infrastructure(c):
    logger.info("Publishing Infrastructure")

    c.run(f"cdk deploy --all")

    logger.info("Done publishing")


@task
def run(c):
    pass
