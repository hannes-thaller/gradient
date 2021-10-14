from gradient_domain import bootstrap


def test__bootstrapping__create():
    sut = bootstrap.Container()

    result = sut.boto_codeartifact()

    assert result is not None
