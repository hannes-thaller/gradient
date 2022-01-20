import subprocess
from concurrent import futures

import logging
import pathlib
from invoke import task

project_name = "gradient-admin"

logging.basicConfig(format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
                    level=logging.INFO)
logging.getLogger("gradient").setLevel(logging.DEBUG)
logger = logging.getLogger(project_name)
logger.setLevel(logging.DEBUG)

dir_build = pathlib.Path("build")
dir_project = pathlib.Path(__file__).parent


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
def build(c):
    logger.info("Building")

    logger.info("Build done")


@task
def test(c):
    logger.info("Testing")

    logger.info("Test done")


@task
def publish(c):
    pass


@task
def auth(c):
    logger.info("Authenticating at code artifact")

    domain = c.config["code_artifact"]["domain"]
    repository = c.config["code_artifact"]["repository"]
    owner = c.config["code_artifact"]["owner"]

    c.run(f"aws codeartifact login --tool pip --repository {repository} --domain {domain} --domain-owner {owner}")

    logger.info("Done authenticating")


@task
def clear_databases(c):
    import pymongo
    logger.info("Clearing service databases")

    def clear_database(config):
        logger.info(f"Clearing {config['name']} database")
        client = pymongo.MongoClient(f"localhost:{config['port']}")
        client.drop_database("service")
        client.close()

    with futures.ThreadPoolExecutor() as pool:
        list(pool.map(clear_database, c.config["databases"]))

    logger.info(f"Done clearing databases")


@task
def start_services(c, path="../../gradient-jvm"):
    processes = []
    for service in c.config["services"]:
        logger.info(f"Running gradle services {service['name']}")
        process = subprocess.Popen(["./gradlew", f":services:{service['name']}:run"],
                                   cwd=path, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        processes.append((service['name'], process))

    for name, process in processes:
        code = process.poll()
        logger.info(f"Service {name} is {'running' if code is None else code}")

    for name, process in processes:
        process.wait()


@task
def start_testing_project(c, path="../../gradient-jvm"):
    for service in c.config["testing"]:
        logger.info(f"Running the testing project {service['name']}")
        process = subprocess.Popen(["./gradlew", f":shared:{service['name']}:run"],
                                   cwd=path, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        process.wait()
        print(process.stdout.read().decode())