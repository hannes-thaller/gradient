# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: model_api.proto

import sys

_b = sys.version_info[0] < 3 and (lambda x: x) or (lambda x: x.encode('latin1'))
from google.protobuf import descriptor as _descriptor
from google.protobuf import symbol_database as _symbol_database

# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()

from src.main.python.gradient.model.api import model_entity_pb2 as model__entity__pb2

DESCRIPTOR = _descriptor.FileDescriptor(
    name='model_api.proto',
    package='org.sourceflow.gradient.model',
    syntax='proto3',
    serialized_options=None,
    serialized_pb=_b(
        '\n\x0fmodel_api.proto\x12\x1dorg.sourceflow.gradient.model\x1a\x12model_entity.proto2u\n\x0cModelService\x12\x65\n\tLoadModel\x12+.org.sourceflow.gradient.model.ModelMessage\x1a+.org.sourceflow.gradient.model.ModelMessageb\x06proto3')
    ,
    dependencies=[model__entity__pb2.DESCRIPTOR, ])

_sym_db.RegisterFileDescriptor(DESCRIPTOR)

_MODELSERVICE = _descriptor.ServiceDescriptor(
    name='ModelService',
    full_name='org.sourceflow.gradient.model.ModelService',
    file=DESCRIPTOR,
    index=0,
    serialized_options=None,
    serialized_start=70,
    serialized_end=187,
    methods=[
        _descriptor.MethodDescriptor(
            name='LoadModel',
            full_name='org.sourceflow.gradient.model.ModelService.LoadModel',
            index=0,
            containing_service=None,
            input_type=model__entity__pb2._MODELMESSAGE,
            output_type=model__entity__pb2._MODELMESSAGE,
            serialized_options=None,
        ),
    ])
_sym_db.RegisterServiceDescriptor(_MODELSERVICE)

DESCRIPTOR.services_by_name['ModelService'] = _MODELSERVICE

# @@protoc_insertion_point(module_scope)
