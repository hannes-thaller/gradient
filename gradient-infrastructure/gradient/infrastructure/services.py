import logging

import attr
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
