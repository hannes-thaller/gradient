import logging
import pathlib

from invoke import task

project_name = "gradient-service-model"

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
    logger.info("Building")

    logger.info("Build done")


@task
def test(c):
    logger.info("Testing")

    path_report = dir_build.joinpath("pytest", "reports", "report.xml")
    c.run(f"pytest tests --junitxml={path_report}")

    logger.info("Test done")


@task
def publish(c):
    pass


@task(default=True)
def run(c):
    from gradient.model import bootstrap
    bootstrap.main()
