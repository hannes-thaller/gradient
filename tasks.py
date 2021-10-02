import logging
import os.path
import pathlib
import subprocess

import requests
from invoke import task

logger = logging.getLogger("gradient-python")
logging.basicConfig(format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")


def _targets(project):
    return [it for it in os.listdir()
            if (not project or project == it) and
            (pathlib.Path(it, "requirements.yaml").exists())]


def run_sub_task(c, project, task, with_env=True):
    logger.info(f"Invoking {task} for project {project} with environment={with_env}")
    try:
        with c.cd(project):
            if with_env:
                c.run(f"conda run --live-stream --no-capture-output -n {project} inv {task}")
            else:
                c.run(f"conda run --live-stream --no-capture-output inv {task}")
    except Exception as ex:
        logger.error(f"Failed {task} for {project}")


def download_conda(url_conda: str) -> pathlib.Path:
    assert url_conda

    logger.info("Downloading conda installer")

    path_installer = pathlib.Path("miniconda.sh")
    response = requests.get(url_conda)
    with path_installer.open("wb") as f:
        f.write(response.content)

    logger.info("Done downloading conda install")

    return path_installer


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


@task
def check(c):
    c.run("which conda")


@task
def install(c, project=None):
    logger.info("Installing")

    for it in _targets(project):
        run_sub_task(c, it, "install", with_env=False)

    logger.info("Install done")


@task
def install_conda(c, force=False):
    logger.info("Installing conda")

    dir_conda = pathlib.Path(c.config.buildspec.dir_conda)
    if force or not dir_conda.exists():
        path_installer = download_conda(c.config.buildspec.url_conda)
        install_conda(path_installer, dir_conda)

    logger.info("Installing done")


@task()
def build(c, project=None):
    logger.info("Building")

    for it in _targets(project):
        run_sub_task(c, it, "build")

    logger.info("Build done")


@task
def test(c, project=None):
    logger.info("Testing")

    for it in _targets(project):
        run_sub_task(c, it, "test")

    logger.info("Test done")


@task
def publish(c, project=None):
    print("[gradient-python] Testing")

    for it in _targets(project):
        run_sub_task(c, it, "publish")

    print("[gradient-python] Test done")
