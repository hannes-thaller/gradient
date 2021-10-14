import logging
import pathlib
import re
import shutil
import subprocess
import typing
from concurrent import futures

import attr

logger = logging.getLogger(__name__)


@attr.s(frozen=True)
class BuildService:
    client = attr.ib()

    def list_gradient_api_version(self, domain: str, owner: str, repository: str) -> typing.List[typing.Tuple[typing.Optional[str], typing.Optional[str]]]:
        from botocore import errorfactory

        logger.info(f"Listing the latest version for the gradient api maven package")

        package = "gradient-service-domain"
        versions = []
        try:
            response = self.client.list_package_versions(
                domain=domain,
                domainOwner=owner,
                repository=repository,
                format="maven",
                namespace="org.sourceflow",
                package="gradient-service-domain",
                status="Published",
                sortBy="PUBLISHED_TIME",
                maxResults=1
            )
            versions = response["versions"]
        except errorfactory.ClientError as ex:
            if ex.__class__.__name__ == "ResourceNotFoundException":
                logger.warning(f"Could not find package {package}.", ex)

        return [(it["version"], it["revision"]) for it in versions]

    def download_gradient_service_api_jar(self, domain: str, owner: str, repository: str, version: str, revision: str, asset_name: str, dir_build: pathlib.Path) -> pathlib.Path:
        logger.info(f"Downloading the {asset_name}")

        response = self.client.get_package_version_asset(
            domain=domain,
            domainOwner=owner,
            repository=repository,
            format="maven",
            namespace="org.sourceflow",
            package="gradient-service-domain",
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
                lines = [import_services.sub("from gradient_domain.services.gen import", it) for it in lines]
            elif "services" in str(path):
                lines = [import_entities.sub("from gradient_domain.entities.gen import", it) for it in lines]
                lines = [import_services.sub("from . import", it) for it in lines]

            path.write_text("\n".join(lines))

        return dir_base.parent
