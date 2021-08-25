import typing

import attr

if typing.TYPE_CHECKING:
    from ..api import code_entity_pb2
    from .. import entity, persistence


@attr.s(frozen=True, slots=True)
class ProgramRepository:
    program: "code_entity_pb2.ProgramDetail" = attr.ib()
    serde: "persistence.ProtobufSerde" = attr.ib()
    _index_type: "typing.Dict[int, code_entity_pb2.CodeElement]" = attr.ib(init=False)
    _index_property: "typing.Dict[int, code_entity_pb2.CodeElement]" = attr.ib(init=False)
    _index_executable: "typing.Dict[int, code_entity_pb2.CodeElement]" = attr.ib(init=False)
    _index_parameter: "typing.Dict[int, code_entity_pb2.CodeElement]" = attr.ib(init=False)

    def __attrs_post_init__(self):
        object.__setattr__(self, "_index_type", self.create_property_index(self.program))
        object.__setattr__(self, "_index_property", self.create_property_index(self.program))
        object.__setattr__(self, "_index_executable", self.create_executable_index(self.program))
        object.__setattr__(self, "_index_parameter", self.create_parameter_index(self.program))

    @staticmethod
    def create_type_index(
            program: "code_entity_pb2.ProgramDetail"
    ) -> typing.Dict[int, code_entity_pb2.CodeElement]:
        assert program
        return {it.id: it
                for it in program.properties
                if it.HasField("type")}

    @staticmethod
    def create_property_index(
            program: "code_entity_pb2.ProgramDetail"
    ) -> typing.Dict[int, code_entity_pb2.CodeElement]:
        assert program
        return {it.id: it
                for it in program.properties
                if it.HasField("property")}

    @staticmethod
    def create_executable_index(
            program: "code_entity_pb2.ProgramDetail"
    ) -> typing.Dict[int, code_entity_pb2.CodeElement]:
        assert program
        return {it.id: it
                for it in program.properties
                if it.HasField("executable")}

    @staticmethod
    def create_parameter_index(
            program: "code_entity_pb2.ProgramDetail"
    ) -> typing.Dict[int, code_entity_pb2.CodeElement]:
        assert program
        return {it.id: it
                for it in program.properties
                if it.HasField("parameter")}

    def get_types(self) -> typing.List["entity.Type"]:
        return [self.serde.from_type(it)
                for it in self._index_type.values()]

    def get_executables(self) -> typing.List["entity.Executable"]:
        return [self.serde.from_executable(it)
                for it in self._index_executable.values()]

    def get_results(self) ->typing.List["entity.Result"]:
        return

    def get_properties(self) -> typing.List["entity.Property"]:
        return [self.serde.from_property(it)
                for it in self._index_property.values()]

    def get_parameters(self) -> typing.List["entity.Parameter"]:
        return [self.serde.from_parameter(it)
                for it in self._index_property.values()]

    def get_properties_for_type(self, e: "entity.Type") -> typing.List["entity.Property"]:
        assert e
        return [self.serde.from_property(self._index_property[it])
                for it in self._index_type[e.id].properties]

    def get_executables_for_type(self, e: "entity.Type") -> typing.List["entity.Executable"]:
        assert e
        return [self.serde.from_executable(self._index_executable[it])
                for it in self._index_type[e.id].executables]

    def get_parameters_for_executables(self, e: "entity.Executable") -> typing.List["entity.Parameter"]:
        assert e
        return [self.serde.from_parameter(self._index_parameter[it])
                for it in self._index_executable[e.id]]

    def get_property_reads_for_executables(self, e: "entity.Executable") -> typing.List["entity.Property"]:
        assert e
        return [self.serde.from_property(self._index_property[it])
                for it in self._index_executable[e.id].reads]

    def get_property_writes_for_executables(self, e: "entity.Executable") -> typing.List["entity.Property"]:
        assert e
        return [self.serde.from_property(self._index_property[it])
                for it in self._index_executable[e.id].writes]

    def get_invocations_for_executables(self, e: "entity.Executable") -> typing.List["entity.Executable"]:
        assert e
        return [self.serde.from_executable(self._index_executable[it])
                for it in self._index_executable[e.id].invokes]
