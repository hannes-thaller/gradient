import typing
import uuid

import attr
import networkx as nx

from src.main.python.gradient.model import entity

if typing.TYPE_CHECKING:
    from src.main.python.gradient.model import persistence
    from src.main.python.gradient.model.api import code_entity_pb2


@attr.s(frozen=True, slots=True)
class FeatureDescriptionBuilder:
    id_project: uuid.UUID = attr.ib()
    program: "code_entity_pb2.ProgramDetail" = attr.ib()

    def __attrs_post_init__(self):
        object.__setattr__(self, "dao_program", persistence.ProgramRepository(self.program))

    def create_feature_description(self, executable: code_entity_pb2.CodeElement) -> entity.FeatureDescription:
        assert executable


@attr.s(frozen=True, slots=True)
class ProgramGraphTransformation:
    repository_program: "persistence.ProgramRepository" = attr.ib()
    in_universe_status: typing.Set["code_entity_pb2.ModelingUniverseStatus"] = attr.ib(
        default={code_entity_pb2.INTERNAL_MODEL, code_entity_pb2.BOUNDARY_MODEL},
        init=False
    )

    def build(self, program: "code_entity_pb2.ProgramDetail") -> nx.MultiGraph:
        assert program

        graph = nx.MultiGraph()
        graph.add_nodes_from(self.repository_program.get_types())
        graph.add_nodes_from(self.repository_program.get_properties())
        graph.add_nodes_from(self.repository_program.get_executables())
        graph.add_nodes_from(self.repository_program.get_parameters())

        graph.add_edges_from((source, target)
                             for source in self.repository_program.get_executables()
                             for target in self.repository_program.get_invocations_for_executables(source))
        graph.add_edges_from((source, target)
                             for source in self.repository_program.get_executables()
                             for target in self.repository_program.get_parameters_for_executables(source))
        graph.add_edges_from((source, target)
                             for source in self.repository_program.get_executables()
                             for target in self.repository_program.get_property_reads_for_executables(source))
        graph.add_edges_from((source, target)
                             for source in self.repository_program.get_executables()
                             for target in self.repository_program.get_property_writes_for_executables(source))

        return graph

    def _update_modeling_status(self, graph: nx.MultiGraph):
        assert graph

        for type in self.repository_program.get_types():
            if type.status in self.in_universe_status:
                pass
