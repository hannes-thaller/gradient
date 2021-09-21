import typing

if typing.TYPE_CHECKING:
    from . import services


class Container:
    _build_service = None

    @staticmethod
    def build_service() -> "services.BuildService":
        from gradient.model import services
        if Container._build_service is None:
            Container._build_service = services.BuildService(Container.boto_codeartifact())
        return Container._build_service

    @staticmethod
    def boto_codeartifact():
        import boto3
        return boto3.client("codeartifact")
