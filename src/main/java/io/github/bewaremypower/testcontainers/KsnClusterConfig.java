/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.bewaremypower.testcontainers;

import java.util.HashMap;
import java.util.Map;

public class KsnClusterConfig {

    private String imageName = "streamnative/sn-pulsar:3.1.0.4";
    private int numBookies = 2;
    private int numBrokers = 2;
    final Map<String, String> configs = new HashMap<>();

    /**
     * Set the image name.
     *
     * @param imageName the Docker image name. You can use "streamnative/sn-pulsar" directly or build your own image
     *                  by uploading the KoP NAR package into the `/pulsar/protocols` directory in "apachepulsar:pulsar"
     *                  image.
     * @return this
     */
    public KsnClusterConfig setImageName(final String imageName) {
        this.imageName = imageName;
        return this;
    }

    public String getImageName() {
        return imageName;
    }

    /**
     * Set the number of bookie nodes.
     *
     * @param numBookies the number of bookie nodes
     * @return this
     * @throws IllegalArgumentException if `numBookies` is less than 2 because the default quorum config is 2
     */
    public KsnClusterConfig setNumBookies(final int numBookies) {
        if (numBookies < 2) {
            throw new IllegalArgumentException("numBookies must be greater than 1");
        }
        this.numBookies = numBookies;
        return this;
    }

    public int getNumBookies() {
        return numBookies;
    }

    /**
     * Set the number of broker nodes.
     *
     * @param numBrokers the number of broker nodes.
     * @return this
     * @throws IllegalArgumentException if `numBrokers` is non-positive
     */
    public KsnClusterConfig setNumBrokers(final int numBrokers) {
        if (numBrokers < 1) {
            throw new IllegalArgumentException("numBrokers must be positive");
        }
        this.numBrokers = numBrokers;
        return this;
    }

    public int getNumBrokers() {
        return numBrokers;
    }

    /**
     * Add a customized config to the broker, including the KSN.
     */
    public KsnClusterConfig addConfig(final String key, final String value) {
        configs.put(key, value);
        return this;
    }
}
