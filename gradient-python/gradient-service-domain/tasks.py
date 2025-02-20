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


def load_parent_config() -> dict:
    import yaml
    with dir_project.parent.joinpath("invoke.yaml").open() as f:
        return yaml.safe_load(f)


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
    else:
        c.run(f"conda env create --force -f requirements/requirements.yaml")

    logger.info("Installing done")


@task
def protos_load(c, dir_local=None):
    if dir_local:
        _load_protos_local(dir_local)
    else:
        _load_protos_aws(c)

    logger.info(f"Done loading protos")


def _load_protos_local(dir_local):
    logger.info(f"Getting protos from {dir_local}")

    dir_local = pathlib.Path(dir_local).joinpath("gradient-service-domain.jar")
    if dir_local.exists():
        _extract_protos_zip(dir_local)
    else:
        logger.warning(f"Could not find the configured version {c.config['gradle_gradient_service_domain_version']}")


def _load_protos_aws(c):
    from gradient_domain import bootstrap
    from botocore import errorfactory

    logger.info("Pulling protos from maven")

    dir_build.mkdir(exist_ok=True)
    service = bootstrap.Container.build_service()

    packages = service.list_gradient_api_version(c.code_artifact.domain, str(c.code_artifact.owner), c.code_artifact.repository_jvm)
    assert packages, f"Could not load protos"
    version_str, revision_current = packages[0]

    config_parent = load_parent_config()
    if version_str and config_parent["gradient_service_version"]:
        logger.info(f"Found configured version {version_str}")

        asset_name = f"gradient-service-domain-{version_str}.jar"

        try:
            path_asset = service.download_gradient_service_api_jar(c.code_artifact.domain, str(c.code_artifact.owner), c.code_artifact.repository_jvm,
                                                                   version_str, revision_current, asset_name, dir_build)

            _extract_protos_zip(path_asset)
        except errorfactory.ClientError as ex:
            logger.warning(f"Could not pull the most recent proto definitions.", ex)
    else:
        logger.warning(f"Could not find the configured version {c.config['gradle_gradient_service_domain_version']}")


def _extract_protos_zip(path_asset: pathlib.Path):
    with zipfile.ZipFile(path_asset) as zipf:
        dir_zip = dir_build.joinpath("protos")

        logger.info(f"Extracting protos into {dir_zip}")
        dir_zip.mkdir(parents=True, exist_ok=True)
        protots = [it for it in zipf.namelist() if it.endswith(".proto")]
        zipf.extractall(dir_zip, protots)


@task
def assemble(c):
    from gradient_domain import bootstrap

    logger.info("Assembling")

    logger.info("Generating python sources from protos.")
    dir_build.mkdir(exist_ok=True)
    service = bootstrap.Container.build_service()

    dir_sources = service.generate_source_from_protos(dir_build.joinpath("protos"),
                                                      dir_build.joinpath("protoc"))

    dir_entities_in = dir_sources.joinpath("model", "entities")
    dir_gen_entities_out = dir_project.joinpath("gradient_domain", "entities", "gen")

    shutil.rmtree(dir_gen_entities_out, ignore_errors=True)

    shutil.move(str(dir_entities_in), str(dir_gen_entities_out))

    logger.info("Assemble done")


@task(pre=[protos_load])
def build(c):
    logger.info("Building")

    assemble(c)

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


@task(pre=[build])
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
