import struct
import uuid

from gradient_domain.entities import common, dataset, code
from gradient_inference import entity


class ProtobufSerde:
    @staticmethod
    def from_uuid(e: common.UUID) -> uuid.UUID:
        assert e is not None and e.bytes
        return uuid.UUID(bytes=struct.pack(">qq", e.most_significant, e.least_significant))

    @staticmethod
    def to_uuid(e: uuid.UUID) -> common.UUID:
        assert e is not None
        msb, lsb = struct.pack(">qq", e.bytes)
        return common.UUID(
            least_significant=lsb,
            most_significant=msb
        )

    @staticmethod
    def from_canonical_name(e: common.CanonicalName) -> entity.CanonicalName:
        assert e is not None
        return entity.CanonicalName(
            components=tuple(e.components),
            types=tuple(entity.NameComponentType[it] for it in e.types)
        )

    @staticmethod
    def to_canonical_name(e: entity.CanonicalName) -> common.CanonicalName:
        assert e is not None
        return common.CanonicalName(
            components=e.components,
            types=[it.value for it in e.types]
        )

    @staticmethod
    def from_data_type(e: code.DataType) -> entity.DataType:
        assert e is not None
        return entity.DataType(
            descriptor=entity.DataTypeDescriptor[e.data_type_descriptor],
            name=ProtobufSerde.from_canonical_name(e.name)
        )

    @staticmethod
    def from_feature(e: dataset.Feature) -> entity.Feature:
        assert e is not None
        return entity.Feature(
            element_id=e.element_id,
            name=ProtobufSerde.from_canonical_name(e.name),
            data_type=ProtobufSerde.from_data_type(e.data_type),
            feature_type=entity.FeatureType[e.feature_type],
            alias_ids=e.alias_ids
        )

    @staticmethod
    def from_feature_description(e: dataset.FeatureDescription) -> entity.FeatureDescription:
        assert e is not None
        return entity.FeatureDescription(
            element_id=e.element_id,
            features=tuple(ProtobufSerde.from_feature(it) for it in e.features)
        )

    @staticmethod
    def from_structure_graph(e: code.StructureGraph) -> entity.StructureGraph:
        assert e
        return entity.StructureGraph(
            types=tuple(ProtobufSerde.from_type(it) for it in e.types),
            properties=tuple(ProtobufSerde.from_property(it) for it in e.properties),
            executables=tuple(ProtobufSerde.from_executable(it) for it in e.executables),
            parameters=tuple(ProtobufSerde.from_parameter(it) for it in e.parameters),
            results=tuple(ProtobufSerde.from_result(it) for it in e.results),
        )

    @staticmethod
    def from_type(e: code.Type) -> entity.Type:
        assert e
        return entity.Type(
            id=e.id,
            name=ProtobufSerde.from_canonical_name(e.name),
            status=entity.ModelUniverseStatus[e.status.name],
            properties=tuple(e.properties),
            executables=tuple(e.executables)
        )

    @staticmethod
    def from_executable(e: code.Executable) -> entity.Executable:
        assert e
        return entity.Executable(
            id=e.id,
            name=ProtobufSerde.from_canonical_name(e.name),
            status=entity.ModelUniverseStatus[e.status.name],
            type_data=ProtobufSerde.from_data_type(e.executable.data_type),
            is_class_member=e.is_class_member,
            is_abstract=e.is_abstract,
            is_constructor=e.is_constructor,
            parameters=tuple(e.parameters),
            invokes=tuple(e.invokes),
            reads=tuple(e.reads),
            writes=tuple(e.writes)
        )

    @staticmethod
    def from_property(e: code.Property) -> entity.Property:
        assert e
        return entity.Property(
            id=e.id,
            name=ProtobufSerde.from_canonical_name(e.name),
            status=entity.ModelUniverseStatus[e.status.name],
            type_data=ProtobufSerde.from_data_type(e.property.data_type),
            is_class_member=e.is_class_member,
            is_immutable=e.is_immutable
        )

    @staticmethod
    def from_parameter(e: code.Parameter) -> entity.Parameter:
        assert e
        return entity.Parameter(
            id=e.id,
            name=ProtobufSerde.from_canonical_name(e.name),
            status=entity.ModelUniverseStatus[e.status.name],
            type_data=ProtobufSerde.from_data_type(e.parameter.data_type),
            index=e.parameter.index
        )

    @staticmethod
    def from_result(e: code.Result) -> entity.Result:
        assert e
        return entity.Result(
            id=e.id,
            name=ProtobufSerde.from_canonical_name(e.name),
            status=entity.ModelUniverseStatus[e.status.name],
            type_data=ProtobufSerde.from_data_type(e.parameter.data_type),
        )

    @staticmethod
    def from_project_context(e: entity.ProjectContext) -> entity.ProjectContext:
        assert e
        return entity.ProjectContext(
            ProtobufSerde.from_uuid(e.id_project),
            ProtobufSerde.from_uuid(e.id_session)
        )


class MongoSerde:
    @staticmethod
    def to_project_context(e: "entity.ProjectContext"):
        assert e
        return {
            "id_project": e.id_project,
            "id_session": e.id_session
        }

    @staticmethod
    def to_factor_graph(e: "entity.FactorGraph"):
        assert e
        return {
            "id": e.id,
            "factors": e.factors,
            "variables": e.variables
        }

    @staticmethod
    def to_cluster_graph(e: "entity.ClusterGraph"):
        assert e
        return {
            "id": e.id,
            "clusters": [MongoSerde.to_cluster(it) for it in e.clusters],
            "variables": [MongoSerde.to_variables(it) for it in e.variables]
        }

    @staticmethod
    def to_cluster(e: "entity.Cluster"):
        assert e
        return {
            "id": e.id,
            "name": e.name,
            "variables": e.variables
        }

    @staticmethod
    def to_factors(e: "entity.Factor"):
        assert e
        return {
            "id": e.id,
            "name": MongoSerde.to_canonical_name(e.name),
            "variables": e.variables
        }

    @staticmethod
    def to_variables(e: "entity.Variable"):
        assert e
        return {
            "id": e.id,
            "name": MongoSerde.to_canonical_name(e.name),
            "type_data": e.type_data.name
        }

    @staticmethod
    def to_canonical_name(e: "entity.CanonicalName"):
        assert e
        return {
            "components": e.components,
            "types": [it.name for it in e.types]
        }
