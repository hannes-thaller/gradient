import enum

import attr
import typing

if typing.TYPE_CHECKING:
    import uuid


@attr.s(frozen=True)
class ProjectContext:
    id_project: "uuid.UUID" = attr.ib()
    id_session: "uuid.UUID" = attr.ib()


class NameComponentType(enum.Enum):
    GROUP = 0
    ARTIFACT = 1
    VERSION = 2
    PACKAGE = 3
    TYPE = 4
    PROPERTY = 5
    EXECUTABLE = 6
    PARAMETER = 7
    RESULT = 8


@attr.s(frozen=True, slots=True)
class CanonicalName:
    components: typing.Tuple[str, ...] = attr.ib()
    types: typing.Tuple[NameComponentType, ...] = attr.ib()
