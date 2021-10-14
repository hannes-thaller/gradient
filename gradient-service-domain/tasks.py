import logging
import pathlib
import re
import shutil
import zipfile

from invoke import task

project_name = "gradient-service-domain"

logging.basicConfig(format="%(asctime)s - %(name)s - %(levelname)s - %(message)s", level=logging.INFO)
logger = logging.getLogger(project_name)

dir_build = pathlib.Path("build")
dir_project = pathlib.Path(__file__).parent


def extract_version(version: str):
    pattern = re.compile(r"(?P<major>\d)\.(?P<minor>\d)\.(?P<patch>\d)([-+])(?P<build>\d)")
    result = pattern.search(version)
    return (
        result.group("major"),
        result.group("minor"),
        result.group("patch"),
        result.group("build"),
    )


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
def protos_load(c):
    from gradient_domain import bootstrap

    logger.info("Pulling protos from maven")

    dir_build.mkdir(exist_ok=True)
    service = bootstrap.Container.build_service()

    packages = service.list_gradient_api_version(c.code_artifact.domain, str(c.code_artifact.owner), c.code_artifact.repository_jvm)
    assert packages, f"Could not load protos"
    version_str, revision_current = packages[0]

    if version_str and c.config["gradle_gradient_service_domain_version"]:
        logger.info(f"Found configured version {version_str}")

        asset_name = f"gradient-service-domain-{version_str}.jar"

        path_asset = service.download_gradient_service_api_jar(c.code_artifact.domain, str(c.code_artifact.owner), c.code_artifact.repository_jvm,
                                                               version_str, revision_current, asset_name, dir_build)

        with zipfile.ZipFile(path_asset) as zipf:
            dir_zip = dir_build.joinpath("protos")

            logger.info(f"Extracting protos into {dir_zip}")
            dir_zip.mkdir(parents=True, exist_ok=True)
            protots = [it for it in zipf.namelist() if it.endswith(".proto")]
            zipf.extractall(dir_zip, protots)
    else:
        logger.warning(f"Could not find the configured version {c.config['gradle_gradient_service_domain_version']}")

    logger.info(f"Done loading protos")


@task(pre=[protos_load])
def build(c):
    from gradient_domain import bootstrap
    from botocore import errorfactory

    logger.info("Building")

    logger.info("Generating python sources from protos.")
    dir_build.mkdir(exist_ok=True)
    service = bootstrap.Container.build_service()

    try:
        dir_sources = service.generate_source_from_protos(dir_build.joinpath("protos"),
                                                          dir_build.joinpath("protoc"))

        dir_entities_in = dir_sources.joinpath("model", "entities")
        dir_services_in = dir_sources.joinpath("model", "services")
        dir_gen_entities_out = dir_project.joinpath("gradient_domain", "entities", "gen")
        dir_gen_services_out = dir_project.joinpath("gradient_domain", "services", "gen")

        shutil.rmtree(dir_gen_entities_out, ignore_errors=True)
        shutil.rmtree(dir_gen_services_out, ignore_errors=True)

        shutil.move(str(dir_entities_in), str(dir_gen_entities_out))
        shutil.move(str(dir_services_in), str(dir_gen_services_out))
    except errorfactory.ClientError as ex:
        logger.warning(f"Could not pull the most recent proto definitions.", ex)

    logger.info("Build done")


@task
def test(c):
    logger.info("Testing")

    path_report = dir_build.joinpath("pytest", "reports", "report.xml")
    c.run(f"pytest tests --junitxml={path_report}")

    logger.info("Test done")


@task
def clean(c):
    shutil.rmtree("build", ignore_errors=True)
    shutil.rmtree("dist", ignore_errors=True)
    shutil.rmtree("gradient_service_domain.egg-info", ignore_errors=True)
    shutil.rmtree(".pytest_cache", ignore_errors=True)


@task(pre=[clean, build])
def publish(c):
    logger.info("Publishing")

    c.run(f"python setup.py sdist bdist_wheel")
    logger.info(f"Generated the package")

    domain = c.config["code_artifact"]["domain"]
    repository = c.config["code_artifact"]["repository_python"]

    c.run(f"aws codeartifact login --tool twine --domain {domain} --repository {repository}")
    try:
        c.run(f"twine upload --repository codeartifact dist/*")
    except:
        logger.error(f"Could not upload the artifact")

    logger.info("Done publishing")


@task
def auth(c):
    logger.info("Authenticating at code artifact")

    domain = c.config["code_artifact"]["domain"]
    repository = c.config["code_artifact"]["repository_python"]

    c.run(f"aws codeartifact login --tool twine --domain {domain} --repository {repository}")

    logger.info("Done authenticating")


@task
def run(c):
    pass
