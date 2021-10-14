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
    def from_type(e: code.Type) -> entity.Type:
        assert e
        return entity.Type(
            id=e.id,
            name=ProtobufSerde.from_canonical_name(e.name),
            status=entity.ModelUniverseStatus[e.status.name]
        )

    @staticmethod
    def from_executable(e: code.Executable) -> entity.Executable:
        assert e
        return entity.Executable(
            id=e.id,
            name=ProtobufSerde.from_canonical_name(e.name),
            status=entity.ModelUniverseStatus[e.status.name],
            type_data=ProtobufSerde.from_data_type(e.executable.data_type)
        )

    @staticmethod
    def from_property(e: code.Property) -> entity.Property:
        assert e
        return entity.Property(
            id=e.id,
            name=ProtobufSerde.from_canonical_name(e.name),
            status=entity.ModelUniverseStatus[e.status.name],
            type_data=ProtobufSerde.from_data_type(e.property.data_type)
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


class MongoSerde:
    @staticmethod
    def to_feature_description(e: "dataset.FeatureDescription"):
        pass

    @staticmethod
    def from_feature_description():
        pass

    @staticmethod
    def to_model():
        pass

    @staticmethod
    def from_model():
        pass
