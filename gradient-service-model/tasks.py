import pathlib
import shutil
import subprocess
from invoke import task

name = "gradient-service-model"
version = "0.1.0"
default_task = "publish"


@task(description="Compiles the protos definitions into tests sources.")
def protos_build(logger):
    dir_in_protos = pathlib.Path("tmp").absolute()
    dir_in_protos.mkdir(exist_ok=True)
    dir_out_protos = pathlib.Path("gradient/model/api").absolute()
    dir_out_protos.mkdir(exist_ok=True, parents=True)
    dir_out_protos.joinpath("__init__.py").touch(exist_ok=True)

    logger.info(f"Pulling protos from API: {dir_in_protos}")
    protos_pull(logger, dir_in_protos)

    logger.info(f"Proto Directory: {dir_in_protos}")
    logger.info(f"Python Output Directory: {dir_out_protos}")
    for it in dir_in_protos.glob("*.proto"):
        cmd = ["tests", "-m", "grpc.tools.protoc", f"-I{dir_in_protos}", f"--python_out={dir_out_protos}",
               f"--grpc_python_out={dir_out_protos}", str(it)]
        logger.info(f"Running: {cmd}")
        subprocess.run(cmd)

    for it in dir_out_protos.glob("*pb2*.py"):
        logger.info(f"Cleaning up {it}")
        cleanup_generated_sources(it)

    logger.info("Cleaning protos dir")
    shutil.rmtree(dir_in_protos)


def cleanup_generated_sources(sourcefile: pathlib.Path):
    to_fix = ["import common_entity_pb2 as common__entity__pb2",
              "import code_entity_pb2 as code__entity__pb2",
              "import model_entity_pb2 as model__entity__pb2",
              "import dataset_entity_pb2 as dataset__entity__pb2"]

    def fix(line: str):
        for it in to_fix:
            if line.startswith(it):
                return "from . " + line
        return line

    if "api" in sourcefile.name and "grpc.py" in sourcefile.name:
        sourcefile.unlink()
    else:
        content = sourcefile.read_text()
        content = "\n".join([fix(it) for it in content.splitlines()])
        sourcefile.write_text(content)


def protos_pull(logger, dir_local_protos):
    dir_api = pathlib.Path("../gradient-service-api")
    dir_protos = [
        dir_api.joinpath("common/src/main/proto"),
        dir_api.joinpath("code/src/main/proto"),
        dir_api.joinpath("dataset/src/main/proto"),
        dir_api.joinpath("model/src/main/proto")
    ]

    if dir_api.exists():
        for it_dir in dir_protos:
            for it in it_dir.glob("**/*.proto"):
                logger.info(f"Pulling {it}")
                shutil.copy(it, dir_local_protos)
    else:
        logger.info("Checkout the api project first")


@init
def initialize(project):
    pathlib.Path(".env").write_text(f"GRADIENT_MODEL_SERVICE={project.version}")
    project.get_property("pytest_extra_args").append("-x")
    project.set_property_if_unset("pytest_coverage_break_build_threshold", 50)
