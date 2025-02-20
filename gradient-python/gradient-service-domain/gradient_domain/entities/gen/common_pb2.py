# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: org/sourceflow/gradient/entities/common.proto
"""Generated protocol buffer code."""
from google.protobuf.internal import enum_type_wrapper
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()




DESCRIPTOR = _descriptor.FileDescriptor(
  name='org/sourceflow/gradient/entities/common.proto',
  package='org.sourceflow.gradient.common.entities',
  syntax='proto3',
  serialized_options=b'\n\'org.sourceflow.gradient.common.entitiesB\016CommonEntities',
  create_key=_descriptor._internal_create_key,
  serialized_pb=b'\n-org/sourceflow/gradient/entities/common.proto\x12\'org.sourceflow.gradient.common.entities\";\n\x04UUID\x12\x19\n\x11least_significant\x18\x01 \x01(\x04\x12\x18\n\x10most_significant\x18\x02 \x01(\x04\"n\n\rCanonicalName\x12\x12\n\ncomponents\x18\x01 \x03(\t\x12I\n\x05types\x18\x02 \x03(\x0e\x32:.org.sourceflow.gradient.common.entities.NameComponentType\"\x96\x01\n\x0eProjectContext\x12\x41\n\nproject_id\x18\x01 \x01(\x0b\x32-.org.sourceflow.gradient.common.entities.UUID\x12\x41\n\nsession_id\x18\x02 \x01(\x0b\x32-.org.sourceflow.gradient.common.entities.UUID\"\xe9\x04\n\x05\x44\x61tum\x12\x16\n\x0cstring_datum\x18\x01 \x01(\tH\x00\x12\x17\n\rboolean_datum\x18\x02 \x01(\x08H\x00\x12\x17\n\rinteger_datum\x18\x03 \x01(\x05H\x00\x12\x15\n\x0b\x66loat_datum\x18\x04 \x01(\x02H\x00\x12\x16\n\x0c\x64ouble_datum\x18\x05 \x01(\x01H\x00\x12\x14\n\nlong_datum\x18\x06 \x01(\x03H\x00\x12I\n\rstrings_datum\x18\x07 \x01(\x0b\x32\x30.org.sourceflow.gradient.common.entities.StringsH\x00\x12K\n\x0e\x62ooleans_datum\x18\x08 \x01(\x0b\x32\x31.org.sourceflow.gradient.common.entities.BooleansH\x00\x12K\n\x0eintegers_datum\x18\t \x01(\x0b\x32\x31.org.sourceflow.gradient.common.entities.IntegersH\x00\x12G\n\x0c\x66loats_datum\x18\n \x01(\x0b\x32/.org.sourceflow.gradient.common.entities.FloatsH\x00\x12I\n\rdoubles_datum\x18\x0b \x01(\x0b\x32\x30.org.sourceflow.gradient.common.entities.DoublesH\x00\x12\x45\n\x0blongs_datum\x18\x0c \x01(\x0b\x32..org.sourceflow.gradient.common.entities.LongsH\x00\x42\x11\n\x0f\x64\x61tum_selection\"\x1a\n\x08\x42ooleans\x12\x0e\n\x06values\x18\x01 \x03(\x08\"\x1a\n\x08Integers\x12\x0e\n\x06values\x18\x01 \x03(\x05\"\x17\n\x05Longs\x12\x0e\n\x06values\x18\x01 \x03(\x03\"\x18\n\x06\x46loats\x12\x0e\n\x06values\x18\x01 \x03(\x02\"\x19\n\x07\x44oubles\x12\x0e\n\x06values\x18\x01 \x03(\x01\"\x19\n\x07Strings\x12\x0e\n\x06values\x18\x01 \x03(\t\"\x84\x01\n\rStreamControl\x12\x42\n\x04type\x18\x02 \x01(\x0e\x32\x34.org.sourceflow.gradient.common.entities.ControlType\x12\x15\n\rsend_messages\x18\x03 \x01(\x03\x12\x18\n\x10maximum_messages\x18\x04 \x01(\x03*\x89\x01\n\x11NameComponentType\x12\t\n\x05GROUP\x10\x00\x12\x0c\n\x08\x41RTIFACT\x10\x01\x12\x0b\n\x07VERSION\x10\x02\x12\x0b\n\x07PACKAGE\x10\x03\x12\x08\n\x04TYPE\x10\x04\x12\x0c\n\x08PROPERTY\x10\x05\x12\x0e\n\nEXECUTABLE\x10\x06\x12\r\n\tPARAMETER\x10\x07\x12\n\n\x06RESULT\x10\x08*1\n\x0b\x43ontrolType\x12\x08\n\x04OPEN\x10\x00\x12\r\n\tHEARTBEAT\x10\x01\x12\t\n\x05\x43LOSE\x10\x02\x42\x39\n\'org.sourceflow.gradient.common.entitiesB\x0e\x43ommonEntitiesb\x06proto3'
)

_NAMECOMPONENTTYPE = _descriptor.EnumDescriptor(
  name='NameComponentType',
  full_name='org.sourceflow.gradient.common.entities.NameComponentType',
  filename=None,
  file=DESCRIPTOR,
  create_key=_descriptor._internal_create_key,
  values=[
    _descriptor.EnumValueDescriptor(
      name='GROUP', index=0, number=0,
      serialized_options=None,
      type=None,
      create_key=_descriptor._internal_create_key),
    _descriptor.EnumValueDescriptor(
      name='ARTIFACT', index=1, number=1,
      serialized_options=None,
      type=None,
      create_key=_descriptor._internal_create_key),
    _descriptor.EnumValueDescriptor(
      name='VERSION', index=2, number=2,
      serialized_options=None,
      type=None,
      create_key=_descriptor._internal_create_key),
    _descriptor.EnumValueDescriptor(
      name='PACKAGE', index=3, number=3,
      serialized_options=None,
      type=None,
      create_key=_descriptor._internal_create_key),
    _descriptor.EnumValueDescriptor(
      name='TYPE', index=4, number=4,
      serialized_options=None,
      type=None,
      create_key=_descriptor._internal_create_key),
    _descriptor.EnumValueDescriptor(
      name='PROPERTY', index=5, number=5,
      serialized_options=None,
      type=None,
      create_key=_descriptor._internal_create_key),
    _descriptor.EnumValueDescriptor(
      name='EXECUTABLE', index=6, number=6,
      serialized_options=None,
      type=None,
      create_key=_descriptor._internal_create_key),
    _descriptor.EnumValueDescriptor(
      name='PARAMETER', index=7, number=7,
      serialized_options=None,
      type=None,
      create_key=_descriptor._internal_create_key),
    _descriptor.EnumValueDescriptor(
      name='RESULT', index=8, number=8,
      serialized_options=None,
      type=None,
      create_key=_descriptor._internal_create_key),
  ],
  containing_type=None,
  serialized_options=None,
  serialized_start=1333,
  serialized_end=1470,
)
_sym_db.RegisterEnumDescriptor(_NAMECOMPONENTTYPE)

NameComponentType = enum_type_wrapper.EnumTypeWrapper(_NAMECOMPONENTTYPE)
_CONTROLTYPE = _descriptor.EnumDescriptor(
  name='ControlType',
  full_name='org.sourceflow.gradient.common.entities.ControlType',
  filename=None,
  file=DESCRIPTOR,
  create_key=_descriptor._internal_create_key,
  values=[
    _descriptor.EnumValueDescriptor(
      name='OPEN', index=0, number=0,
      serialized_options=None,
      type=None,
      create_key=_descriptor._internal_create_key),
    _descriptor.EnumValueDescriptor(
      name='HEARTBEAT', index=1, number=1,
      serialized_options=None,
      type=None,
      create_key=_descriptor._internal_create_key),
    _descriptor.EnumValueDescriptor(
      name='CLOSE', index=2, number=2,
      serialized_options=None,
      type=None,
      create_key=_descriptor._internal_create_key),
  ],
  containing_type=None,
  serialized_options=None,
  serialized_start=1472,
  serialized_end=1521,
)
_sym_db.RegisterEnumDescriptor(_CONTROLTYPE)

ControlType = enum_type_wrapper.EnumTypeWrapper(_CONTROLTYPE)
GROUP = 0
ARTIFACT = 1
VERSION = 2
PACKAGE = 3
TYPE = 4
PROPERTY = 5
EXECUTABLE = 6
PARAMETER = 7
RESULT = 8
OPEN = 0
HEARTBEAT = 1
CLOSE = 2



_UUID = _descriptor.Descriptor(
  name='UUID',
  full_name='org.sourceflow.gradient.common.entities.UUID',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  create_key=_descriptor._internal_create_key,
  fields=[
    _descriptor.FieldDescriptor(
      name='least_significant', full_name='org.sourceflow.gradient.common.entities.UUID.least_significant', index=0,
      number=1, type=4, cpp_type=4, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='most_significant', full_name='org.sourceflow.gradient.common.entities.UUID.most_significant', index=1,
      number=2, type=4, cpp_type=4, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=90,
  serialized_end=149,
)


_CANONICALNAME = _descriptor.Descriptor(
  name='CanonicalName',
  full_name='org.sourceflow.gradient.common.entities.CanonicalName',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  create_key=_descriptor._internal_create_key,
  fields=[
    _descriptor.FieldDescriptor(
      name='components', full_name='org.sourceflow.gradient.common.entities.CanonicalName.components', index=0,
      number=1, type=9, cpp_type=9, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='types', full_name='org.sourceflow.gradient.common.entities.CanonicalName.types', index=1,
      number=2, type=14, cpp_type=8, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=151,
  serialized_end=261,
)


_PROJECTCONTEXT = _descriptor.Descriptor(
  name='ProjectContext',
  full_name='org.sourceflow.gradient.common.entities.ProjectContext',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  create_key=_descriptor._internal_create_key,
  fields=[
    _descriptor.FieldDescriptor(
      name='project_id', full_name='org.sourceflow.gradient.common.entities.ProjectContext.project_id', index=0,
      number=1, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='session_id', full_name='org.sourceflow.gradient.common.entities.ProjectContext.session_id', index=1,
      number=2, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=264,
  serialized_end=414,
)


_DATUM = _descriptor.Descriptor(
  name='Datum',
  full_name='org.sourceflow.gradient.common.entities.Datum',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  create_key=_descriptor._internal_create_key,
  fields=[
    _descriptor.FieldDescriptor(
      name='string_datum', full_name='org.sourceflow.gradient.common.entities.Datum.string_datum', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='boolean_datum', full_name='org.sourceflow.gradient.common.entities.Datum.boolean_datum', index=1,
      number=2, type=8, cpp_type=7, label=1,
      has_default_value=False, default_value=False,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='integer_datum', full_name='org.sourceflow.gradient.common.entities.Datum.integer_datum', index=2,
      number=3, type=5, cpp_type=1, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='float_datum', full_name='org.sourceflow.gradient.common.entities.Datum.float_datum', index=3,
      number=4, type=2, cpp_type=6, label=1,
      has_default_value=False, default_value=float(0),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='double_datum', full_name='org.sourceflow.gradient.common.entities.Datum.double_datum', index=4,
      number=5, type=1, cpp_type=5, label=1,
      has_default_value=False, default_value=float(0),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='long_datum', full_name='org.sourceflow.gradient.common.entities.Datum.long_datum', index=5,
      number=6, type=3, cpp_type=2, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='strings_datum', full_name='org.sourceflow.gradient.common.entities.Datum.strings_datum', index=6,
      number=7, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='booleans_datum', full_name='org.sourceflow.gradient.common.entities.Datum.booleans_datum', index=7,
      number=8, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='integers_datum', full_name='org.sourceflow.gradient.common.entities.Datum.integers_datum', index=8,
      number=9, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='floats_datum', full_name='org.sourceflow.gradient.common.entities.Datum.floats_datum', index=9,
      number=10, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='doubles_datum', full_name='org.sourceflow.gradient.common.entities.Datum.doubles_datum', index=10,
      number=11, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='longs_datum', full_name='org.sourceflow.gradient.common.entities.Datum.longs_datum', index=11,
      number=12, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
    _descriptor.OneofDescriptor(
      name='datum_selection', full_name='org.sourceflow.gradient.common.entities.Datum.datum_selection',
      index=0, containing_type=None,
      create_key=_descriptor._internal_create_key,
    fields=[]),
  ],
  serialized_start=417,
  serialized_end=1034,
)


_BOOLEANS = _descriptor.Descriptor(
  name='Booleans',
  full_name='org.sourceflow.gradient.common.entities.Booleans',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  create_key=_descriptor._internal_create_key,
  fields=[
    _descriptor.FieldDescriptor(
      name='values', full_name='org.sourceflow.gradient.common.entities.Booleans.values', index=0,
      number=1, type=8, cpp_type=7, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=1036,
  serialized_end=1062,
)


_INTEGERS = _descriptor.Descriptor(
  name='Integers',
  full_name='org.sourceflow.gradient.common.entities.Integers',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  create_key=_descriptor._internal_create_key,
  fields=[
    _descriptor.FieldDescriptor(
      name='values', full_name='org.sourceflow.gradient.common.entities.Integers.values', index=0,
      number=1, type=5, cpp_type=1, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=1064,
  serialized_end=1090,
)


_LONGS = _descriptor.Descriptor(
  name='Longs',
  full_name='org.sourceflow.gradient.common.entities.Longs',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  create_key=_descriptor._internal_create_key,
  fields=[
    _descriptor.FieldDescriptor(
      name='values', full_name='org.sourceflow.gradient.common.entities.Longs.values', index=0,
      number=1, type=3, cpp_type=2, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=1092,
  serialized_end=1115,
)


_FLOATS = _descriptor.Descriptor(
  name='Floats',
  full_name='org.sourceflow.gradient.common.entities.Floats',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  create_key=_descriptor._internal_create_key,
  fields=[
    _descriptor.FieldDescriptor(
      name='values', full_name='org.sourceflow.gradient.common.entities.Floats.values', index=0,
      number=1, type=2, cpp_type=6, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=1117,
  serialized_end=1141,
)


_DOUBLES = _descriptor.Descriptor(
  name='Doubles',
  full_name='org.sourceflow.gradient.common.entities.Doubles',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  create_key=_descriptor._internal_create_key,
  fields=[
    _descriptor.FieldDescriptor(
      name='values', full_name='org.sourceflow.gradient.common.entities.Doubles.values', index=0,
      number=1, type=1, cpp_type=5, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=1143,
  serialized_end=1168,
)


_STRINGS = _descriptor.Descriptor(
  name='Strings',
  full_name='org.sourceflow.gradient.common.entities.Strings',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  create_key=_descriptor._internal_create_key,
  fields=[
    _descriptor.FieldDescriptor(
      name='values', full_name='org.sourceflow.gradient.common.entities.Strings.values', index=0,
      number=1, type=9, cpp_type=9, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=1170,
  serialized_end=1195,
)


_STREAMCONTROL = _descriptor.Descriptor(
  name='StreamControl',
  full_name='org.sourceflow.gradient.common.entities.StreamControl',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  create_key=_descriptor._internal_create_key,
  fields=[
    _descriptor.FieldDescriptor(
      name='type', full_name='org.sourceflow.gradient.common.entities.StreamControl.type', index=0,
      number=2, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='send_messages', full_name='org.sourceflow.gradient.common.entities.StreamControl.send_messages', index=1,
      number=3, type=3, cpp_type=2, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
    _descriptor.FieldDescriptor(
      name='maximum_messages', full_name='org.sourceflow.gradient.common.entities.StreamControl.maximum_messages', index=2,
      number=4, type=3, cpp_type=2, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=1198,
  serialized_end=1330,
)

_CANONICALNAME.fields_by_name['types'].enum_type = _NAMECOMPONENTTYPE
_PROJECTCONTEXT.fields_by_name['project_id'].message_type = _UUID
_PROJECTCONTEXT.fields_by_name['session_id'].message_type = _UUID
_DATUM.fields_by_name['strings_datum'].message_type = _STRINGS
_DATUM.fields_by_name['booleans_datum'].message_type = _BOOLEANS
_DATUM.fields_by_name['integers_datum'].message_type = _INTEGERS
_DATUM.fields_by_name['floats_datum'].message_type = _FLOATS
_DATUM.fields_by_name['doubles_datum'].message_type = _DOUBLES
_DATUM.fields_by_name['longs_datum'].message_type = _LONGS
_DATUM.oneofs_by_name['datum_selection'].fields.append(
  _DATUM.fields_by_name['string_datum'])
_DATUM.fields_by_name['string_datum'].containing_oneof = _DATUM.oneofs_by_name['datum_selection']
_DATUM.oneofs_by_name['datum_selection'].fields.append(
  _DATUM.fields_by_name['boolean_datum'])
_DATUM.fields_by_name['boolean_datum'].containing_oneof = _DATUM.oneofs_by_name['datum_selection']
_DATUM.oneofs_by_name['datum_selection'].fields.append(
  _DATUM.fields_by_name['integer_datum'])
_DATUM.fields_by_name['integer_datum'].containing_oneof = _DATUM.oneofs_by_name['datum_selection']
_DATUM.oneofs_by_name['datum_selection'].fields.append(
  _DATUM.fields_by_name['float_datum'])
_DATUM.fields_by_name['float_datum'].containing_oneof = _DATUM.oneofs_by_name['datum_selection']
_DATUM.oneofs_by_name['datum_selection'].fields.append(
  _DATUM.fields_by_name['double_datum'])
_DATUM.fields_by_name['double_datum'].containing_oneof = _DATUM.oneofs_by_name['datum_selection']
_DATUM.oneofs_by_name['datum_selection'].fields.append(
  _DATUM.fields_by_name['long_datum'])
_DATUM.fields_by_name['long_datum'].containing_oneof = _DATUM.oneofs_by_name['datum_selection']
_DATUM.oneofs_by_name['datum_selection'].fields.append(
  _DATUM.fields_by_name['strings_datum'])
_DATUM.fields_by_name['strings_datum'].containing_oneof = _DATUM.oneofs_by_name['datum_selection']
_DATUM.oneofs_by_name['datum_selection'].fields.append(
  _DATUM.fields_by_name['booleans_datum'])
_DATUM.fields_by_name['booleans_datum'].containing_oneof = _DATUM.oneofs_by_name['datum_selection']
_DATUM.oneofs_by_name['datum_selection'].fields.append(
  _DATUM.fields_by_name['integers_datum'])
_DATUM.fields_by_name['integers_datum'].containing_oneof = _DATUM.oneofs_by_name['datum_selection']
_DATUM.oneofs_by_name['datum_selection'].fields.append(
  _DATUM.fields_by_name['floats_datum'])
_DATUM.fields_by_name['floats_datum'].containing_oneof = _DATUM.oneofs_by_name['datum_selection']
_DATUM.oneofs_by_name['datum_selection'].fields.append(
  _DATUM.fields_by_name['doubles_datum'])
_DATUM.fields_by_name['doubles_datum'].containing_oneof = _DATUM.oneofs_by_name['datum_selection']
_DATUM.oneofs_by_name['datum_selection'].fields.append(
  _DATUM.fields_by_name['longs_datum'])
_DATUM.fields_by_name['longs_datum'].containing_oneof = _DATUM.oneofs_by_name['datum_selection']
_STREAMCONTROL.fields_by_name['type'].enum_type = _CONTROLTYPE
DESCRIPTOR.message_types_by_name['UUID'] = _UUID
DESCRIPTOR.message_types_by_name['CanonicalName'] = _CANONICALNAME
DESCRIPTOR.message_types_by_name['ProjectContext'] = _PROJECTCONTEXT
DESCRIPTOR.message_types_by_name['Datum'] = _DATUM
DESCRIPTOR.message_types_by_name['Booleans'] = _BOOLEANS
DESCRIPTOR.message_types_by_name['Integers'] = _INTEGERS
DESCRIPTOR.message_types_by_name['Longs'] = _LONGS
DESCRIPTOR.message_types_by_name['Floats'] = _FLOATS
DESCRIPTOR.message_types_by_name['Doubles'] = _DOUBLES
DESCRIPTOR.message_types_by_name['Strings'] = _STRINGS
DESCRIPTOR.message_types_by_name['StreamControl'] = _STREAMCONTROL
DESCRIPTOR.enum_types_by_name['NameComponentType'] = _NAMECOMPONENTTYPE
DESCRIPTOR.enum_types_by_name['ControlType'] = _CONTROLTYPE
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

UUID = _reflection.GeneratedProtocolMessageType('UUID', (_message.Message,), {
  'DESCRIPTOR' : _UUID,
  '__module__' : 'org.sourceflow.gradient.entities.common_pb2'
  # @@protoc_insertion_point(class_scope:org.sourceflow.gradient.common.entities.UUID)
  })
_sym_db.RegisterMessage(UUID)

CanonicalName = _reflection.GeneratedProtocolMessageType('CanonicalName', (_message.Message,), {
  'DESCRIPTOR' : _CANONICALNAME,
  '__module__' : 'org.sourceflow.gradient.entities.common_pb2'
  # @@protoc_insertion_point(class_scope:org.sourceflow.gradient.common.entities.CanonicalName)
  })
_sym_db.RegisterMessage(CanonicalName)

ProjectContext = _reflection.GeneratedProtocolMessageType('ProjectContext', (_message.Message,), {
  'DESCRIPTOR' : _PROJECTCONTEXT,
  '__module__' : 'org.sourceflow.gradient.entities.common_pb2'
  # @@protoc_insertion_point(class_scope:org.sourceflow.gradient.common.entities.ProjectContext)
  })
_sym_db.RegisterMessage(ProjectContext)

Datum = _reflection.GeneratedProtocolMessageType('Datum', (_message.Message,), {
  'DESCRIPTOR' : _DATUM,
  '__module__' : 'org.sourceflow.gradient.entities.common_pb2'
  # @@protoc_insertion_point(class_scope:org.sourceflow.gradient.common.entities.Datum)
  })
_sym_db.RegisterMessage(Datum)

Booleans = _reflection.GeneratedProtocolMessageType('Booleans', (_message.Message,), {
  'DESCRIPTOR' : _BOOLEANS,
  '__module__' : 'org.sourceflow.gradient.entities.common_pb2'
  # @@protoc_insertion_point(class_scope:org.sourceflow.gradient.common.entities.Booleans)
  })
_sym_db.RegisterMessage(Booleans)

Integers = _reflection.GeneratedProtocolMessageType('Integers', (_message.Message,), {
  'DESCRIPTOR' : _INTEGERS,
  '__module__' : 'org.sourceflow.gradient.entities.common_pb2'
  # @@protoc_insertion_point(class_scope:org.sourceflow.gradient.common.entities.Integers)
  })
_sym_db.RegisterMessage(Integers)

Longs = _reflection.GeneratedProtocolMessageType('Longs', (_message.Message,), {
  'DESCRIPTOR' : _LONGS,
  '__module__' : 'org.sourceflow.gradient.entities.common_pb2'
  # @@protoc_insertion_point(class_scope:org.sourceflow.gradient.common.entities.Longs)
  })
_sym_db.RegisterMessage(Longs)

Floats = _reflection.GeneratedProtocolMessageType('Floats', (_message.Message,), {
  'DESCRIPTOR' : _FLOATS,
  '__module__' : 'org.sourceflow.gradient.entities.common_pb2'
  # @@protoc_insertion_point(class_scope:org.sourceflow.gradient.common.entities.Floats)
  })
_sym_db.RegisterMessage(Floats)

Doubles = _reflection.GeneratedProtocolMessageType('Doubles', (_message.Message,), {
  'DESCRIPTOR' : _DOUBLES,
  '__module__' : 'org.sourceflow.gradient.entities.common_pb2'
  # @@protoc_insertion_point(class_scope:org.sourceflow.gradient.common.entities.Doubles)
  })
_sym_db.RegisterMessage(Doubles)

Strings = _reflection.GeneratedProtocolMessageType('Strings', (_message.Message,), {
  'DESCRIPTOR' : _STRINGS,
  '__module__' : 'org.sourceflow.gradient.entities.common_pb2'
  # @@protoc_insertion_point(class_scope:org.sourceflow.gradient.common.entities.Strings)
  })
_sym_db.RegisterMessage(Strings)

StreamControl = _reflection.GeneratedProtocolMessageType('StreamControl', (_message.Message,), {
  'DESCRIPTOR' : _STREAMCONTROL,
  '__module__' : 'org.sourceflow.gradient.entities.common_pb2'
  # @@protoc_insertion_point(class_scope:org.sourceflow.gradient.common.entities.StreamControl)
  })
_sym_db.RegisterMessage(StreamControl)


DESCRIPTOR._options = None
# @@protoc_insertion_point(module_scope)