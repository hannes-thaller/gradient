import logging

import attr
import yaml
from aws_cdk import core, aws_iam

from . import components

logger = logging.getLogger(__name__)


@attr.s(frozen=True)
class InfrastructureService:
    def create_infrastructure(self, config):
        app = core.App()

        stack_infrastructure = components.InfrastructureStack(app, config)
        self.create_module_stacks(stack_infrastructure, config)

        app.synth()
        return app

    def create_module_stacks(self, stack_parent, config):
        for it in config["repo-stacks"]:
            build_spec = self._load_buildspec(it["code-build"]["build_spec"])
            stacks_build = components.BuildStack(scope=stack_parent,
                                                 id=it["id"],
                                                 config=it,
                                                 build_spec=build_spec)

    def _load_buildspec(self, path: str):
        with open(path, "r") as f:
            return yaml.safe_load(f)
