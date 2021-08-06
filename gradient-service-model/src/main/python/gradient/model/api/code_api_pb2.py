# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: code_api.proto

import sys

_b = sys.version_info[0] < 3 and (lambda x: x) or (lambda x: x.encode('latin1'))
from google.protobuf import descriptor as _descriptor
from google.protobuf import symbol_database as _symbol_database

# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()

from src.main.python.gradient.model.api import code_entity_pb2 as code__entity__pb2

DESCRIPTOR = _descriptor.FileDescriptor(
    name='code_api.proto',
    package='org.sourceflow.gradient.code',
    syntax='proto3',
    serialized_options=None,
    serialized_pb=_b(
        '\n\x0e\x63ode_api.proto\x12\x1corg.sourceflow.gradient.code\x1a\x11\x63ode_entity.proto2u\n\x0b\x43odeService\x12\x66\n\x0e\x41nalyzeProgram\x12).org.sourceflow.gradient.code.CodeMessage\x1a).org.sourceflow.gradient.code.CodeMessageb\x06proto3')
    ,
    dependencies=[code__entity__pb2.DESCRIPTOR, ])

_sym_db.RegisterFileDescriptor(DESCRIPTOR)

_CODESERVICE = _descriptor.ServiceDescriptor(
    name='CodeService',
    full_name='org.sourceflow.gradient.code.CodeService',
    file=DESCRIPTOR,
    index=0,
    serialized_options=None,
    serialized_start=67,
    serialized_end=184,
    methods=[
        _descriptor.MethodDescriptor(
            name='AnalyzeProgram',
            full_name='org.sourceflow.gradient.code.CodeService.AnalyzeProgram',
            index=0,
            containing_service=None,
            input_type=code__entity__pb2._CODEMESSAGE,
            output_type=code__entity__pb2._CODEMESSAGE,
            serialized_options=None,
        ),
    ])
_sym_db.RegisterServiceDescriptor(_CODESERVICE)

DESCRIPTOR.services_by_name['CodeService'] = _CODESERVICE

# @@protoc_insertion_point(module_scope)
