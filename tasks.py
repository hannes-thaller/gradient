import pathlib
import shutil

from invoke import task


@task(default=True)
def check(c):
    print("Checking system for dependencies...")
    if not shutil.which("docker"):
        print("Could not find docker")
    if not shutil.which("docker-compose"):
        print("Could not find docker-compose")


@task(pre=[check])
def build(c):
    for it in c.config["repositories"]:
        dir_project = pathlib.Path(it["path"])
        if dir_project.exists():
            with c.cd(str(dir_project)):
                if dir_project.joinpath("build.gradle.kts").exists():
                    c.run("./gradlew clean build")
                if len(list(dir_project.glob("docker-compose.y*ml"))) > 0:
                    c.run("docker-compose build")
        else:
            print(f"Skipping missing project: {it['name']}")


@task
def up(c, restart=False):
    for it in c.config["repositories"]:
        dir_project = pathlib.Path(it["path"])
        if dir_project.exists():
            with c.cd(str(dir_project)):
                if len(list(dir_project.glob("docker-compose.y*ml"))) > 0:
                    if restart:
                        c.run("docker-compose stop")
                    c.run("docker-compose up -d")


@task
def stop(c):
    for it in c.config["repositories"]:
        dir_project = pathlib.Path(it["path"])
        if dir_project.exists():
            with c.cd(str(dir_project)):
                if len(list(dir_project.glob("docker-compose.y*ml"))) > 0:
                    c.run("docker-compose stop")


@task
def publish_local(c):
    for it in c.config["repositories"]:
        dir_project = pathlib.Path(it["path"])
        if dir_project.exists():
            with c.cd(str(dir_project)):
                if dir_project.joinpath("build.gradle.kts").exists():
                    c.run("./gradlew clean assemble publishToMavenLocal")
        else:
            print(f"Skipping missing project: {it['name']}")
