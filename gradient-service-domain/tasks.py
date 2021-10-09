import logging
import pathlib
import shutil
import zipfile

from botocore import errorfactory
from invoke import task

project_name = "gradient-service-domain"

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
def protos_load(c):
    from gradient.domain import bootstrap

    logger.info("Pulling protos from maven")

    dir_build.mkdir(exist_ok=True)
    service = bootstrap.Container.build_service()

    packages = service.list_gradient_api_version(c.code_artifact.domain, str(c.code_artifact.owner), c.code_artifact.repository)
    packages = [it for it in packages if it[0] == c.config["gradle_gradient_service_domain_version"]]
    assert packages, f"Could not load protos"

    version, revision = packages[0]
    if version and revision:
        logger.info(f"Found configured version {version} and revision {revision}")

        asset_name = f"gradient-service-domain-{version}.jar"

        path_asset = service.download_gradient_service_api_jar(c.code_artifact.domain, str(c.code_artifact.owner), c.code_artifact.repository,
                                                               version, revision, asset_name, dir_build)

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
    from gradient.domain import bootstrap

    logger.info("Building")

    logger.info("Generating python sources from protos.")
    dir_build.mkdir(exist_ok=True)
    service = bootstrap.Container.build_service()

    try:
        dir_sources = service.generate_source_from_protos(dir_build.joinpath("protos"),
                                                          dir_build.joinpath("protoc"))
        shutil.rmtree(dir_project.joinpath("gradient"), ignore_errors=True)
        shutil.move(str(dir_sources), str(dir_project))
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

    with c.prefix(f"source .env/bin/activate"):
        c.run(f"python setup.py sdist bdist_wheel")
        logger.info(f"Generated the package")

        c.run(f"aws codeartifact login --tool twine --domain sourceflow --repository python")
        c.run(f"twine upload --repository codeartifact dist/*")

    logger.info("Done publishing")


@task
def auth(c):
    logger.info("Authenticating at code artifact")

    c.run(f"aws codeartifact login --tool twine --domain sourceflow --repository python")

    logger.info("Done authenticating")


@task
def run(c):
    pass
