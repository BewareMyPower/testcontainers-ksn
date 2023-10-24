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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class KsnCluster implements AutoCloseable {

    private static final String ZK_HOST = "zookeeper";
    private static final int ZK_PORT = 2181;
    private static final String CLUSTER = "ksn-cluster";

    private final Network network = Network.newNetwork();
    private final GenericContainer<?> zkContainer;
    private final List<GenericContainer<?>> bookies;
    private final List<BrokerContainer> brokers;
    private final String bootstrapServers;

    public KsnCluster(final KsnClusterConfig config) throws IOException, InterruptedException {
        final DockerImageName imageName = DockerImageName.parse(config.imageName);
        zkContainer = new GenericContainer<>(imageName).withNetwork(network)
                .withNetworkAliases(ZK_HOST)
                .withEnv("PULSAR_MEM", "-Xmx256M")
                .withCommand("bin/pulsar zookeeper")
                .withExposedPorts(ZK_PORT);
        zkContainer.start();
        final String metadataStoreUrl = "zk:" + ZK_HOST + ":" + ZK_PORT;
        final Container.ExecResult result = zkContainer.execInContainer(
                "/pulsar/bin/pulsar", "initialize-cluster-metadata",
                "--cluster", CLUSTER,
                "--metadata-store", metadataStoreUrl,
                "--configuration-metadata-store", metadataStoreUrl,
                "--web-service-url", "http://" + BrokerContainer.HOST_PREFIX + ":8080",
                "--broker-service-url", "pulsar://" + BrokerContainer.HOST_PREFIX + ":6650");
        if (result.getExitCode() != 0) {
            throw new IOException("Failed to initialize metadata");
        }
        this.bookies = new ArrayList<>(config.numBookies);
        for (int i = 0; i < config.numBookies; i++) {
            final String host = "bookie-" + i;
            final GenericContainer<?> bookie = new GenericContainer<>(imageName)
                    .withCopyFileToContainer(MountableFile.forClasspathResource("run-bookie.sh"),
                            "/pulsar/bin/run-bookie.sh")
                    .withNetwork(network)
                    .withNetworkAliases(host)
                    .withCreateContainerCmdModifier(cmd -> cmd.withHostName(host))
                    .withEnv("BOOKIE_MEM", "-Xmx512M")
                    .withEnv("useHostNameAsBookieID", "true")
                    .withEnv("zkServers", "zookeeper:2181")
                    .withEnv("autoRecoveryDaemonEnabled", "false")
                    .withCommand("bash bin/run-bookie.sh")
                    .withExposedPorts(3181);
            bookie.start();
            bookies.add(bookie);
        }
        this.brokers = new ArrayList<>(config.numBrokers);
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < config.numBrokers; i++) {
            final BrokerContainer broker = new BrokerContainer(imageName, network, metadataStoreUrl, CLUSTER);
            broker.start();
            brokers.add(broker);
            if (i > 0) {
                builder.append(",");
            }
            builder.append(broker.getHost()).append(":").append(broker.getPort());
        }
        this.bootstrapServers = builder.toString();
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }


    @Override
    public void close() {
        if (brokers != null) {
            brokers.forEach(Startable::close);
        }
        if (bookies != null) {
            bookies.forEach(Startable::close);
        }
        if (zkContainer != null) {
            zkContainer.close();
        }
        network.close();
    }
}
