#!/bin/bash

name=${PWD##*/}

echo "Creating conda environment: ${name}"
eval "$(conda shell.bash hook)"
conda create -y -n "${name}"
conda activate "${name}"
conda install -y -c conda-forge -c pytorch \
  python=3.7 invoke=1.5.0 attrs=20.3.0 toolz=0.11.1 bidict=0.21.2 networkx=2.5 \
  cloudpickle=1.6.0 protobuf=3.11 grpcio=1.23 grpcio-tools=1.16 pymongo=3.11.0 \
  numpy=1.19.5 pandas=1.2.0 pytorch=1.7.1 cudatoolkit=11.0.221 \
  hypothesis=6.0.0 pytest=6.2.1 pytest-cov=2.10.1
pip install pulsar-client==2.8.0
pip install .