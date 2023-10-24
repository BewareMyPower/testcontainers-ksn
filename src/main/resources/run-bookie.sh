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

# sets dbStorage_writeCacheMaxSizeMb and dbStorage_readAheadCacheMaxSizeMb if not already defined
export dbStorage_writeCacheMaxSizeMb="${dbStorage_writeCacheMaxSizeMb:-16}"
export dbStorage_readAheadCacheMaxSizeMb="${dbStorage_readAheadCacheMaxSizeMb:-16}"

bin/apply-config-from-env.py conf/bookkeeper.conf
bin/apply-config-from-env.py conf/pulsar_env.sh
bin/apply-config-from-env.py conf/bkenv.sh

exec bin/pulsar bookie
