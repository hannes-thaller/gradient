import logging
import os.path
import pathlib
import venv
from invoke import task

project_name = "gradient-python"

logger = logging.getLogger(project_name)
logging.basicConfig(format="%(asctime)s - %(name)s - %(levelname)s - %(message)s", level=logging.INFO)


def _targets(project):
    return [it for it in os.listdir()
            if (not project or project == it) and
            (pathlib.Path(it, "requirements.txt").exists())]


@task
def install(c, project=None):
    logger.info("Installing")

    for project in _targets(project):
        logger.info(f"Installing {project}")

        venv.create(os.path.join(project, ".env"))
        with c.cd(project):
            with c.prefix(f"source .env/bin/activate"):
                c.run("inv install")

    logger.info("Install done")


@task()
def build(c, project=None):
    logger.info("Building")

    for project in _targets(project):
        logger.info(f"Building {project}")

        venv.create(os.path.join(project, ".env"))
        with c.cd(project):
            with c.prefix(f"source .env/bin/activate"):
                c.run("inv build")

    logger.info("Build done")


@task
def test(c, project=None):
    logger.info("Testing")

    for project in _targets(project):
        logger.info(f"Testing {project}")

        venv.create(os.path.join(project, ".env"))
        with c.cd(project):
            with c.prefix(f"source .env/bin/activate"):
                c.run("inv test")

    logger.info("Test done")


@task
def publish(c, project=None):
    logger.info("Publish")

    for project in _targets(project):
        logger.info(f"Publish {project}")

        venv.create(os.path.join(project, ".env"))
        with c.cd(project):
            with c.prefix(f"source .env/bin/activate"):
                c.run("inv publish")

    logger.info("Publish done")
