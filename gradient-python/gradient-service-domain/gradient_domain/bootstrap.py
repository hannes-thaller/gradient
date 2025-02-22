class Container:
    _build_service = None

    @staticmethod
    def build_service():
        from gradient_domain import services
        if Container._build_service is None:
            Container._build_service = services.BuildService(Container.boto_codeartifact())
        return Container._build_service

    @staticmethod
    def boto_codeartifact():
        import boto3
        return boto3.client("codeartifact")
