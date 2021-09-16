import typing


class Container:
    _instance: typing.Optional["Container"] = None

    @staticmethod
    def instance() -> "Container":
        if Container._instance is None:
            Container._instance = Container()

        return Container._instance

    def build_service(self):
        from gradient.infrastructure import build
        return build.BuildService()
