import logging
import pathlib

from invoke import task

project_name = "gradient-service-inference"

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


@task
def auth(c):
    logger.info("Authenticating at code artifact")

    domain = c.config["code_artifact"]["domain"]
    repository = c.config["code_artifact"]["repository"]
    owner = c.config["code_artifact"]["owner"]

    c.run(f"aws codeartifact login --tool pip --repository {repository} --domain {domain} --domain-owner {owner}")

    logger.info("Done authenticating")


@task(default=True)
def run(c):
    from gradient_inference import __main__
    __main__.main()
