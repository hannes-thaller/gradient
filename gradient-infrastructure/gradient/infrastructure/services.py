import attr
from aws_cdk import core

from gradient.infrastructure import components


@attr.s(frozen=True)
class InfrastructureService:
    def create_repository_stack(self, app: core.App):
        stack = components.RepositoriesStack(app, "RepositoryStack")
        return stack


app = core.App()
service = InfrastructureService()
stack  = service.create_repository_stack(app)
app.synth()
