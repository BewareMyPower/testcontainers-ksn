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

import com.github.dockerjava.api.command.InspectContainerResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

class BrokerContainer extends GenericContainer<BrokerContainer> {

    static final String HOST_PREFIX = "broker-";
    private static final AtomicInteger COUNT = new AtomicInteger(0);
    private static final Map<String, String> DEFAULT_CONFIGS = new HashMap<>();
    private static final String STARTUP_SCRIPT_PATH = "/pulsar/bin/run-broker.sh";
    private static final String INTERNAL_LISTENER = "kafka_internal";
    private static final String EXTERNAL_LISTENER = "kafka_external";
    private static final int INTERNAL_PORT = 9092;

    static {
        DEFAULT_CONFIGS.put("messagingProtocols", "kafka");
        DEFAULT_CONFIGS.put("brokerEntryMetadataInterceptors",
                "org.apache.pulsar.common.intercept.AppendIndexMetadataInterceptor");
        DEFAULT_CONFIGS.put("brokerDeleteInactiveTopicsEnabled", "false");
        DEFAULT_CONFIGS.put("kafkaTransactionCoordinatorEnabled", "true");
        DEFAULT_CONFIGS.put("brokerDeduplicationEnabled", "true");
        DEFAULT_CONFIGS.put("defaultRetentionTimeInMinutes", "-1");
        DEFAULT_CONFIGS.put("defaultRetentionSizeInMB", "-1");
        DEFAULT_CONFIGS.put("ttlDurationDefaultInSeconds", "259200");
        DEFAULT_CONFIGS.put("kafkaProtocolMap",
                String.format("%s:PLAINTEXT,%s:PLAINTEXT", INTERNAL_LISTENER, EXTERNAL_LISTENER));
    }

    private final String host;
    private final int port;

    BrokerContainer(final DockerImageName imageName, final Network network, final String metadataStoreUrl,
                    final String cluster) {
        super(imageName);
        int id = COUNT.getAndIncrement();
        this.host = HOST_PREFIX + id;
        this.port = 19092 + id;
        this.setPortBindings(Collections.singletonList(port + ":" + port));
        this.withNetwork(network).withNetworkAliases(host)
                .withCopyFileToContainer(MountableFile.forClasspathResource("run-broker.sh"),
                        STARTUP_SCRIPT_PATH)
                .withCreateContainerCmdModifier(cmd -> cmd.withHostName(host))
                .withEnv("PULSAR_MEM", "-Xmx512M")
                .addBrokerConfig("metadataStoreUrl", metadataStoreUrl)
                .addBrokerConfig("configurationMetadataStoreUrl", metadataStoreUrl)
                .addBrokerConfig("clusterName", cluster)
                .withLogConsumer(outputFrame ->
                        System.out.println("[" + host + "] " + outputFrame.getUtf8StringWithoutLineEnding()))
                .withExposedPorts(6650, 8080, INTERNAL_PORT)
                .withCommand("bash " + STARTUP_SCRIPT_PATH);
        DEFAULT_CONFIGS.forEach(this::addBrokerConfig);
    }

    int getPort() {
        return port;
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        super.containerIsStarting(containerInfo);
        final String kafkaListeners = INTERNAL_LISTENER + "://0.0.0.0:" + INTERNAL_PORT
                + "," + EXTERNAL_LISTENER + "://0.0.0.0:" + port;
        final String kafkaAdvertisedListeners = INTERNAL_LISTENER + "://" + host + ":" + INTERNAL_PORT
                + "," + EXTERNAL_LISTENER + "://127.0.0.1:" + port;
        final String pulsarListeners = "pulsar:pulsar://" + host + ":6650";
        final String prefix = "export PULSAR_PREFIX_";
        final String env = prefix + "kafkaListeners=" + kafkaListeners + "\n"
                + prefix + "kafkaAdvertisedListeners=" + kafkaAdvertisedListeners + "\n"
                + prefix + "advertisedListeners=" + pulsarListeners + "\n";
        // This script is used in run-broker.sh
        copyFileToContainer(Transferable.of(env, 0755), "/pulsar/conf/add_to_env.sh");
    }

    private BrokerContainer addBrokerConfig(final String key, final String value) {
        return this.withEnv("PULSAR_PREFIX_" + key, value);
    }
}
