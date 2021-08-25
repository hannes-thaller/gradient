import pathlib
import yaml

from setuptools import setup, find_packages

path_properties = pathlib.Path("project.yaml")


def load_and_increment_version(path_properties: pathlib.Path) -> str:
    if not path_properties.exists():
        path_properties.touch(exist_ok=False)
        with path_properties.open("w") as f:
            yaml.dump(dict(version="0.1.0+0"), f)

    with path_properties.open("r") as f:
        project_properties = yaml.load(f)

    version_old = project_properties["version"]
    version_parts = version_old.split("+")
    project_properties["version"] = f"{version_parts[0]}+{int(version_parts[1]) + 1}"

    with path_properties.open("w") as f:
        yaml.dump(project_properties, f)

    return project_properties["version"]


setup(
    name="gradient-service-api",
    version=load_and_increment_version(path_properties),
    description="The python api entities of Gradient",
    author="Sourceflow",
    license="Copyright Sourceflow",
    packages=find_packages(),
    classifiers=[
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.7",
    ],
)
