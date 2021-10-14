import itertools
import typing

import attr
import networkx as nx

if typing.TYPE_CHECKING:
    from gradient.domain.entities import code
    from gradient_inference import persistence

    ProgramRepositoryFactory = typing.Callable[["code.ProgramDetail"], "persistence.ProgramRepository"]


@attr.s(frozen=True, slots=True)
class ModelService:
    dao_model: "persistence.ModelDao" = attr.ib()
    serde_proto: "persistence.ProtobufSerde" = attr.ib()
    factory_program_repository: ProgramRepositoryFactory = attr.ib()

    def build_structure_graph(self, program: "code.ProgramDetail"):
        assert program

        repo = self.factory_program_repository(program)

        graph = nx.MultiGraph()
        graph.add_nodes_from(repo.get_types())
        graph.add_nodes_from(repo.get_properties())
        graph.add_nodes_from(repo.get_executables())
        graph.add_nodes_from(repo.get_parameters())

    @staticmethod
    def build_structure_graph__structural_edges(repo: "persistence.ProgramRepository",
                                                graph: "nx.MultiGraph") -> "nx.MultiGraph":
        assert repo
        assert graph
        graph.add_edges_from(((type, it)
                              for type in repo.get_types()
                              for it in itertools.chain(repo.get_executables_for_type(type),
                                                        repo.get_properties_for_type(type))),
                             type="declares")
        graph.add_edges_from(((executable, it)
                              for executable in repo.get_executables()
                              for it in repo.get_parameters_for_executables(executable)),
                             type="declares")
        return graph

    @staticmethod
    def _build_structure_graph__behavioral_edges(repo: "persistence.ProgramRepository",
                                                 graph: "nx.MultiGraph") -> "nx.MultiGraph":
        assert repo
        assert graph
        graph.add_edges_from(((executable, it)
                              for executable in repo.get_executables()
                              for it in repo.get_parameters_for_executables(executable)),
                             type="receives")
        graph.add_edges_from(((executable, it)
                              for executable in repo.get_executables()
                              for it in repo.get_property_reads_for_executables(executable)),
                             type="reads")
        graph.add_edges_from(((executable, it)
                              for executable in repo.get_executables()
                              for it in repo.get_property_writes_for_executables(executable)),
                             type="writes")
        graph.add_edges_from(((executable, it)
                              for executable in repo.get_executables()),
                             type="writes")

    def build_factor_graph(self, program: "nx.MultiGraph"):
        assert program

    def build_junction_tree(self, program: "nx.MultiGraph"):
        assert program


@attr.s(frozen=True, slots=True)
class InferenceService:
    dao_model: "persistence.ModelDao" = attr.ib()

    def create_program_graph(self, program: "code.ProgramDetail"):
        assert program

    def fit_model(self):
        pass

    def predict(self):
        pass

    def sample(self):
        pass
