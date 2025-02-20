import logging
import subprocess

from invoke import task

project_name = "gradient-python"

logger = logging.getLogger(project_name)
logging.basicConfig(format="%(asctime)s - %(name)s - %(levelname)s - %(message)s", level=logging.INFO)


@task
def install(c, module=None):
    logger.info("Installing")

    modules = [it for it in c.config["modules"]
               if module is None or module == it]

    for module in modules:
        logger.info(f"Installing {module}")

        subprocess.run(["conda", "run", "inv", "install"],
                       cwd=module,
                       stdout=subprocess.PIPE)

    logger.info("Install done")


@task()
def build(c, module=None):
    logger.info("Building")

    modules = [it for it in c.config["modules"]
               if module is None or module == it]

    for module in modules:
        logger.info(f"Building {module}")

        c.run(f"conda run --live-stream -n {module} inv install")

    logger.info("Build done")


@task
def test(c, module=None):
    logger.info("Testing")

    modules = [it for it in c.config["modules"]
               if module is None or module == it]

    for module in modules:
        logger.info(f"Building {module}")

        c.run(f"conda run --live-stream -n {module} inv test")

    logger.info("Test done")


@task
def publish(c, module=None):
    logger.info("Publish")

    modules = [it for it in c.config["modules"]
               if module is None or module == it]

    for module in modules:
        logger.info(f"Building {module}")

        c.run(f"conda run --live-stream -n {module} inv publish")

    logger.info("Publish done")


@task
def auth(c):
    logger.info("Authenticating at code artifact")

    domain = c.config["code_artifact"]["domain"]
    repository = c.config["code_artifact"]["repository"]
    owner = c.config["code_artifact"]["owner"]

    c.run(f"aws codeartifact login --tool pip --repository {repository} --domain {domain} --domain-owner {owner}")

    logger.info("Done authenticating")
