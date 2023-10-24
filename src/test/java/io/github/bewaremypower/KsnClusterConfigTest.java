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
package io.github.bewaremypower;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertThrows;

import io.github.bewaremypower.testcontainers.KsnClusterConfig;
import org.testng.annotations.Test;

public class KsnClusterConfigTest {

    @Test
    public void test() {
        final KsnClusterConfig config = new KsnClusterConfig();
        assertEquals(config.getImageName(), "streamnative/sn-pulsar:3.1.0.4");
        assertEquals(config.getNumBookies(), 2);
        assertEquals(config.getNumBrokers(), 2);

        assertSame(config.setImageName("streamnative/sn-pulsar:3.1.0.1"), config);
        assertEquals(config.getImageName(), "streamnative/sn-pulsar:3.1.0.1");

        assertSame(config.setNumBookies(3), config);
        assertEquals(config.getNumBookies(), 3);
        assertThrows(IllegalArgumentException.class, () -> config.setNumBookies(1));

        assertSame(config.setNumBrokers(1), config);
        assertEquals(config.getNumBrokers(), 1);
        assertThrows(IllegalArgumentException.class, () -> config.setNumBrokers(0));
    }
}
