#!/bin/bash
#
# Copyright 2016-2017 Testify Project.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
set -e

if [ "$TRAVIS_PULL_REQUEST" = "false" ]; then
    echo "Decrypting Secrets"
    openssl aes-256-cbc -K $encrypted_faaf279dd1ae_key -iv $encrypted_faaf279dd1ae_iv -in secrets.tar.gz.enc -out secrets.tar.gz -d
    tar --strip-components 1 -xzf secrets.tar.gz
    gpg --fast-import testifybot.asc
    eval "$(ssh-agent -s)"
    ssh-add testifybot_rsa
fi

echo "Installing Docker version $DOCKER_VERSION"
# install the version of docker in the DOCKER_VERSION env var
./docker.sh install_docker
# double-check that the version/config is correct
./docker.sh dump_docker_config
docker version
docker info

echo "Before Install Operations All Done!"