import pathlib
import yaml

from setuptools import setup, find_packages


def load_and_increment_version() -> str:
    path_properties = pathlib.Path(__file__).parent.parent.joinpath("invoke.yaml")

    with path_properties.open("r") as f:
        props = yaml.safe_load(f)

    return props["gradient_service_version"].replace("-", "+")


setup(
    name="gradient-service-inference",
    version=load_and_increment_version(),
    description="The Gradient model registry and inference service.",
    long_description="The Gradient model registry and inference service.",
    url="https://bitbucket.org/sourceflow-ai/gradient-python/src/master/",
    license="Copyright Sourceflow",
    packages=find_packages(),
    install_requires=[],
    classifiers=[
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.8",
    ],
)
