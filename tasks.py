import os.path
import pathlib

from invoke import task


def _targets(project):
    return [it for it in os.listdir()
            if (not project or project == it) and
            (pathlib.Path(it, "requirements.yaml").exists())]


def run_sub_task(c, project, task):
    try:
        with c.cd(project):
            c.run(f"conda run --live-stream --no-capture-output -n {project} inv {task}")
    except Exception as ex:
        print(f"[gradient-python] Failed {task} for {project}")


@task
def check(c):
    c.run("which conda")


@task
def install(c, project=None):
    print("[gradient-python] Installing")

    for it in _targets(project):
        run_sub_task(c, it, "install")

    print("[gradient-python] Install done")


@task()
def build(c, project=None):
    print("[gradient-python] Building")

    for it in _targets(project):
        run_sub_task(c, it, "build")

    print("[gradient-python] Build done")


@task
def test(c, project=None):
    print("[gradient-python] Testing")

    for it in _targets(project):
        run_sub_task(c, it, "test")

    print("[gradient-python] Test done")


@task
def publish(c, project=None):
    print("[gradient-python] Testing")

    for it in _targets(project):
        run_sub_task(c, it, "publish")

    print("[gradient-python] Test done")
