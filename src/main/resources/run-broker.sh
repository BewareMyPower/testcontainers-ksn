#!/bin/bash
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


set -e -x

# The env file is added while the container is already running.
# In order to wait for the port mapping, we need to w
while [ ! -f conf/add_to_env.sh ]; do
  sleep 0.1
done

source conf/add_to_env.sh

bin/apply-config-from-env.py conf/broker.conf
bin/apply-config-from-env.py conf/pulsar_env.sh

exec bin/pulsar broker
