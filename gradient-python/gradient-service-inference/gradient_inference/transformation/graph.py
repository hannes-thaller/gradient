import attr
import uuid

from gradient_inference import entity


@attr.s(frozen=True)
class FactorGraphTransformation:

    @staticmethod
    def transform(graph_structure: entity.StructureGraph) -> entity.FactorGraph:
        assert graph_structure

        elements_atomic = tuple(entity.Variable(id=it.id,
                                                name=it.name,
                                                type_data=entity.DataType[entity.DataTypeDescriptor.Name(it.type_data)])
                                for it in graph_structure.atomic_elements()
                                if FactorGraphTransformation.is_valid_atomic_element(it))

        ids_elements_atomic = set(it.id for it in elements_atomic)

        elements_compositional = tuple(entity.Factor(id=it.id,
                                                     name=it.name,
                                                     variables=tuple(it for it in it.child_relationship_ids()
                                                                     if it in ids_elements_atomic))
                                       for it in graph_structure.compositional_elements()
                                       if FactorGraphTransformation.is_valid_compositional_element(it))

        return entity.FactorGraph(id=uuid.uuid4(),
                                  factors=elements_compositional,
                                  variables=elements_atomic)

    @staticmethod
    def is_valid_atomic_element(element: entity.AtomicElement) -> bool:
        return (element.status != entity.ModelUniverseStatus.EXTERNAL and
                entity.DataTypeDescriptor.NONE.value < element.type_data.descriptor.value < entity.DataTypeDescriptor.REFERENCE.value)

    @staticmethod
    def is_valid_compositional_element(element: entity.CompositionalElement) -> bool:
        return (element.status != entity.ModelUniverseStatus.EXTERNAL and
                (not isinstance(element, entity.Executable) or not element.is_abstract))


@attr.s(frozen=True)
class ClusterGraphTransformation:

    @staticmethod
    def transform(graph_factor: entity.FactorGraph):
        assert graph_factor

        variables = tuple(graph_factor.variables)
        clusters = tuple(
            entity.Cluster(
                id=it.id,
                name=it.name,
                variables=tuple(it.variables),
            )
            for it in graph_factor.factors
        )
        sepsets = tuple(
            entity.Sepset()
        )