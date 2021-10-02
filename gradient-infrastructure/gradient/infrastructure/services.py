import logging
import pathlib
import subprocess

import attr
import requests
import yaml
from aws_cdk import core
from . import components

logger = logging.getLogger(__name__)


@attr.s(frozen=True)
class InfrastructureService:
    def create_infrastructure(self, config):
        app = core.App()
        stack_infrastructure = components.InfrastructureStack(app)
        for it in config["repo-stacks"]:
            build_spec = self._load_buildspec(it["code-build"]["build_spec"])
            stacks_build = components.BuildStack(stack_infrastructure, it["id"], it, build_spec)
        app.synth()
        return app

    def _load_buildspec(self, path: str):
        with open(path, "r") as f:
            return yaml.safe_load(f)

    @staticmethod
    def download_conda(url_conda: str) -> pathlib.Path:
        assert url_conda

        logger.info("Downloading conda installer")

        path_installer = pathlib.Path("miniconda.sh")
        response = requests.get(url_conda)
        with path_installer.open("wb") as f:
            f.write(response.content)

        logger.info("Done downloading conda install")

        return path_installer

    @staticmethod
    def install_conda(path_installer: pathlib.Path, dir_install: pathlib.Path):
        assert path_installer.exists()

        logger.info(f"Installing conda from {path_installer} into {dir_install}")

        dir_install = pathlib.Path(dir_install)
        dir_install.mkdir(parents=True, exist_ok=True)

        subprocess.run(["sh", "path_installer", "-b", "-p", dir_install.absolute()])
        subprocess.run(["source", dir_install.joinpath("etc", "profile.d", "conda.sh")])
        subprocess.run(["conda", "config", "--set", "always_yes", "yes", "--set", "changeps1", "no"])
        subprocess.run(["conda", "update", "-q", "conda"])
        subprocess.run(["conda", "info", "-a"])
        subprocess.run(["conda", "install", "-c", "conda-forge", "python=3.7", "invoke=1.5.0"])

        logger.info(f"Done installing conda")
