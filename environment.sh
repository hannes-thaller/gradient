#!/bin/bash

name="${PWD##*/}"

echo "Creating conda environment: ${name}"
eval "$(conda shell.bash hook)"
conda create -n "${name}" -y -c conda-forge python=3.7 invoke=1.5 attrs=20.3.0 pymongo=3.11.0
