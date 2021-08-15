#!/bin/bash

name=${PWD##*/}

echo "Creating conda environment: ${name}"
eval "$(conda shell.bash hook)"
conda env remove -n "${name}" -y
conda env create -n "${name}" -f requirements.yaml