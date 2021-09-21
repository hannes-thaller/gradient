import logging
import pathlib
import re
import shutil
import subprocess
import typing
import zipfile
from concurrent import futures

import attr
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
    logger.info("Pulling protos from maven")
    dir_build.mkdir(exist_ok=True)
    service = Container.build_service()

    version, revision = service.list_gradient_api_version(c.code_artifact.domain, str(c.code_artifact.owner), c.code_artifact.repository)
    if version and revision:
        asset_name = f"gradient-service-api-{version}.jar"

        path_asset = service.download_gradient_service_api_jar(c.code_artifact.domain, str(c.code_artifact.owner), c.code_artifact.repository,
                                                               version, revision, asset_name)

        with zipfile.ZipFile(path_asset) as zipf:
            dir_zip = dir_build.joinpath("protos")

            logger.info(f"Extracting protos into {dir_zip}")
            dir_zip.mkdir(parents=True, exist_ok=True)
            protots = [it for it in zipf.namelist() if it.endswith(".proto")]
            zipf.extractall(dir_zip, protots)

    logger.info(f"Done loading protos")


@task(pre=[protos_load])
def build(c):
    logger.info("Building")

    logger.info("Generating python sources from protos.")
    dir_build.mkdir(exist_ok=True)
    service = Container.build_service()

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


class Container:
    _build_service = None

    @staticmethod
    def build_service() -> "BuildService":
        if Container._build_service is None:
            Container._build_service = BuildService()
        return Container._build_service

    @staticmethod
    def boto_codeartifact():
        import boto3
        return boto3.client("codeartifact")


class BuildService:
    client = attr.ib(factory=Container.boto_codeartifact)

    def list_gradient_api_version(self, domain: str, owner: str, repository: str) -> (typing.Optional[str], typing.Optional[str]):
        logger.info(f"Listing the latest version for the gradient api maven package")

        response = self.client.list_package_versions(
            domain=domain,
            domainOwner=owner,
            repository=repository,
            format="maven",
            namespace="org.sourceflow",
            package="gradient-service-api",
            status="Published",
            sortBy="PUBLISHED_TIME",
            maxResults=1
        )

        result = None, None
        if response:
            versions = response["versions"]
            result = versions[0]["version"], versions[0]["revision"]

        return result

    def download_gradient_service_api_jar(self, domain: str, owner: str, repository: str, version: str, revision: str, asset_name: str) -> pathlib.Path:
        logger.info(f"Downloading the {asset_name}")

        response = self.client.get_package_version_asset(
            domain=domain,
            domainOwner=owner,
            repository=repository,
            format="maven",
            namespace="org.sourceflow",
            package="gradient-service-api",
            packageVersion=version,
            packageVersionRevision=revision,
            asset=asset_name
        )

        stream = response["asset"]
        file_out = dir_build.joinpath(asset_name)
        with file_out.open("wb") as f:
            for it in stream:
                f.write(it)

        return file_out

    def generate_source_from_protos(self, dir_in: pathlib.Path, dir_out: pathlib.Path) -> pathlib.Path:
        assert dir_in.exists()
        logger.info(f"Generating python sources from protos under {dir_in}")

        shutil.rmtree(dir_out, ignore_errors=True)
        dir_out.mkdir(parents=True, exist_ok=True)
        with futures.ThreadPoolExecutor() as pool:
            cmds = [["python",
                     "-m", "grpc.tools.protoc",
                     f"-I{dir_in}",
                     f"--python_out={dir_out}",
                     f"--grpc_python_out={dir_out}",
                     str(it)]
                    for it in dir_in.rglob("*.proto")]
            pool.map(subprocess.run, cmds)

        return self.cleanup_generated_sources(dir_out)

    def cleanup_generated_sources(self, dir_in: pathlib.Path) -> pathlib.Path:
        logger.info(f"Cleaning generated python sources under {dir_in}")

        # delete duplicated files
        for it in dir_in.rglob("*.py"):
            if "entities" in str(it) and it.name.endswith("_grpc.py"):
                it.unlink()
            if "services" in str(it) and it.name.endswith("_pb2.py"):
                it.unlink()

        # fix directory structure
        dir_base = dir_in.joinpath("gradient", "model")
        dir_base.mkdir(parents=True, exist_ok=True)
        shutil.move(str(dir_in.joinpath("org", "sourceflow", "gradient", "entities")),
                    str(dir_base.joinpath("entities")))
        shutil.move(str(dir_in.joinpath("org", "sourceflow", "gradient", "services")),
                    str(dir_base.joinpath("services")))
        shutil.rmtree(dir_in.joinpath("org"))

        # add package files
        dir_base.joinpath("entities", "__init__.py").touch()
        dir_base.joinpath("services", "__init__.py").touch()
        dir_base.joinpath("__init__.py").touch()
        dir_base.parent.joinpath("__init__.py").touch()

        # fix imports
        import_entities = re.compile("from org.sourceflow.gradient.entities import")
        import_services = re.compile("from org.sourceflow.gradient.entities import")
        for path in dir_in.rglob("*.py"):
            lines = path.read_text().splitlines()
            if "entities" in str(path):
                lines = [import_entities.sub("from . import", it) for it in lines]
                lines = [import_services.sub("from .. services", it) for it in lines]
            elif "services" in str(path):
                lines = [import_entities.sub("from ..entities import", it) for it in lines]
                lines = [import_services.sub("from . import", it) for it in lines]

            path.write_text("\n".join(lines))

        return dir_base.parent
