import pathlib
import shutil

from invoke import task
import pymongo


@task(default=True)
def check(c):
    print("Checking system for dependencies...")
    if shutil.which("docker"):
        print("docker ... OK")
    else:
        print("Could not find docker")

    if shutil.which("docker-compose"):
        print("docker-compose ... OK")
    else:
        print("Could not find docker-compose")

    if shutil.which("git"):
        print("git ... OK")
    else:
        print("Could not find git")


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
        if "service" in it["tags"] and dir_project.exists():
            with c.cd(str(dir_project)):
                if len(list(dir_project.glob("docker-compose.y*ml"))) > 0:
                    if restart:
                        c.run("docker-compose stop")
                    c.run("docker-compose up -d")


@task
def stop(c):
    for it in c.config["repositories"]:
        dir_project = pathlib.Path(it["path"])
        if "service" in it["tags"] and dir_project.exists():
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


@task
def clear_datastores(c):
    for it in c.config["repositories"]:
        if "mongo-port" in it:
            client = pymongo.MongoClient("localhost", it["mongo-port"])
            client.drop_database("service")
            client.close()



@task
def generate_proto(c):
    dir_project = pathlib.Path(__file__).parent.joinpath("gradient", "model")
    dir_api = pathlib.Path(next(it for it in c.config["repositories"])["path"])
    dir_api = "/home/hannes/Projects/Sourceflow/gradient-service-api"
    cmd = f"protoc " \
          f"-I=/home/hannes/Projects/Sourceflow/gradient-service-api " \
          f"--python_out=/home/hannes/Projects/Sourceflow/gradient-service/gradient/model " \
          f"--proto_path=/home/hannes/Projects/Sourceflow/gradient-service-api/introspect/src/main/proto/ " \
          f"/home/hannes/Projects/Sourceflow/gradient-service-api/introspect/src/main/proto/*.proto"
    c.run(cmd)


@task
def checkout_projects(c):
    for it in c.config["repositories"]:
        dir_project = pathlib.Path(it["path"])
        shutil.rmtree(dir_project, ignore_errors=True)

        c.run(f"git clone {it['repository']} {it['path']}")
