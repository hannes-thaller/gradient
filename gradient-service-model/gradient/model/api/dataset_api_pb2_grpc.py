# Generated by the gRPC Python protocol compiler plugin. DO NOT EDIT!
import grpc

from gradient.model.api import dataset_entity_pb2 as dataset__entity__pb2


class DatasetServiceStub(object):
  # missing associated documentation comment in .proto file
  pass

  def __init__(self, channel):
    """Constructor.

    Args:
      channel: A grpc.Channel.
    """
    self.FeatureDescriptionsOfPrograms = channel.unary_unary(
        '/org.sourceflow.gradient.dataset.DatasetService/FeatureDescriptionsOfPrograms',
        request_serializer=dataset__entity__pb2.DatasetMessage.SerializeToString,
        response_deserializer=dataset__entity__pb2.DatasetMessage.FromString,
        )
    self.LoadDataset = channel.unary_unary(
        '/org.sourceflow.gradient.dataset.DatasetService/LoadDataset',
        request_serializer=dataset__entity__pb2.DatasetMessage.SerializeToString,
        response_deserializer=dataset__entity__pb2.DatasetMessage.FromString,
        )


class DatasetServiceServicer(object):
  # missing associated documentation comment in .proto file
  pass

  def FeatureDescriptionsOfPrograms(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def LoadDataset(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')


def add_DatasetServiceServicer_to_server(servicer, server):
  rpc_method_handlers = {
      'FeatureDescriptionsOfPrograms': grpc.unary_unary_rpc_method_handler(
          servicer.FeatureDescriptionsOfPrograms,
          request_deserializer=dataset__entity__pb2.DatasetMessage.FromString,
          response_serializer=dataset__entity__pb2.DatasetMessage.SerializeToString,
      ),
      'LoadDataset': grpc.unary_unary_rpc_method_handler(
          servicer.LoadDataset,
          request_deserializer=dataset__entity__pb2.DatasetMessage.FromString,
          response_serializer=dataset__entity__pb2.DatasetMessage.SerializeToString,
      ),
  }
  generic_handler = grpc.method_handlers_generic_handler(
      'org.sourceflow.gradient.dataset.DatasetService', rpc_method_handlers)
  server.add_generic_rpc_handlers((generic_handler,))