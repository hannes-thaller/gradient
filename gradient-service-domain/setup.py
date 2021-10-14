import pathlib
import yaml

from setuptools import setup, find_packages


def load_and_increment_version() -> str:
    path_properties = pathlib.Path("invoke.yaml")

    with path_properties.open("r") as f:
        props = yaml.safe_load(f)

    return props["gradle_gradient_service_domain_version"].replace("-", "+")


setup(
    name="gradient-service-domain",
    version=load_and_increment_version(),
    description="The python API domain entities of Gradient",
    long_description="The python API domain entities of Gradient",
    url="https://bitbucket.org/sourceflow-ai/gradient-python/src/master/",
    license="Copyright Sourceflow",
    packages=find_packages(),
    install_requires=[],
    classifiers=[
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.7",
    ],
)
