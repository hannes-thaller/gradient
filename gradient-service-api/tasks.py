import logging
import pathlib
import shutil
import zipfile

from invoke import task

logging.basicConfig(format="%(asctime)s - %(name)s - %(levelname)s - %(message)s", level=logging.INFO)
logger = logging.getLogger("gradient-service-api")

project_name = "gradient-service-api"

dir_build = pathlib.Path("build")
dir_project = pathlib.Path(__file__).parent


@task
def install(c):
    logger.info("Installing")

    c.run(f"conda env create --force -f requirements.yaml")

    logger.info("Installing done")


@task
def protos_load(c):
    from gradient.model import bootstrap
    logger.info("Pulling protos from maven")
    dir_build.mkdir(exist_ok=True)
    service = bootstrap.Container.build_service()

    version, revision = service.list_gradient_api_version(c.code_artifact.domain, str(c.code_artifact.owner), c.code_artifact.repository)
    if version and revision:
        asset_name = f"gradient-service-api-{version}.jar"

        path_asset = service.download_gradient_service_api_jar(c.code_artifact.domain, str(c.code_artifact.owner), c.code_artifact.repository,
                                                               version, revision, asset_name, dir_build)

        with zipfile.ZipFile(path_asset) as zipf:
            dir_zip = dir_build.joinpath("protos")

            logger.info(f"Extracting protos into {dir_zip}")
            dir_zip.mkdir(parents=True, exist_ok=True)
            protots = [it for it in zipf.namelist() if it.endswith(".proto")]
            zipf.extractall(dir_zip, protots)

    logger.info(f"Done loading protos")


@task(pre=[protos_load])
def build(c):
    from gradient.model import bootstrap
    logger.info("Building")

    logger.info("Generating python sources from protos.")
    dir_build.mkdir(exist_ok=True)
    service = bootstrap.Container.build_service()

    dir_sources = service.generate_source_from_protos(dir_build.joinpath("protos"),
                                                      dir_build.joinpath("protoc"))

    shutil.rmtree(dir_project.joinpath("gradient"), ignore_errors=True)
    shutil.move(str(dir_sources), str(dir_project))

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


@task(pre=[build])
def publish(c):
    logger.info("Publishing")

    shutil.rmtree("dist", ignore_errors=True)

    c.run(f"python setup.py sdist bdist_wheel")
    logger.info(f"Generated the package")

    c.run(f"aws codeartifact login --tool twine --domain sourceflow --repository python")
    c.run(f"conda run --live-stream -n {project_name} twine upload --repository codeartifact dist/*")

    logger.info("Done publishing")


@task
def auth(c):
    logger.info("Authenticating at code artifact")

    c.run(f"aws codeartifact login --tool twine --domain sourceflow --repository python")

    logger.info("Done authenticating")


@task
def run(c):
    pass
