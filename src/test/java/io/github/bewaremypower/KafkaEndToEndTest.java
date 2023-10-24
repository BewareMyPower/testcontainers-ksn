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

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.github.bewaremypower.testcontainers.KsnCluster;
import io.github.bewaremypower.testcontainers.KsnClusterConfig;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class KafkaEndToEndTest {

    private KsnCluster cluster;

    @BeforeClass
    public void setup() throws IOException, InterruptedException {
        cluster = new KsnCluster(new KsnClusterConfig());
    }

    @AfterClass(alwaysRun = true)
    public void cleanup() {
        if (cluster != null) {
            cluster.close();
        }
    }

    @Test
    public void testPartitionedTopic() throws ExecutionException, InterruptedException {
        final Properties adminProps = new Properties();
        adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.getBootstrapServers());
        final AdminClient client = AdminClient.create(adminProps);
        final String topic = "test-partitioned-topic";
        final int numPartitions = 8;
        client.createTopics(Collections.singleton(new NewTopic(topic, 8, (short) 1))).all().get();
        client.close();

        final Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        final int messagesPerPartition = 10;
        final int totalMessages = numPartitions * messagesPerPartition;
        final KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);
        for (int i = 0; i < totalMessages; i++) {
            producer.send(new ProducerRecord<>(topic, i % numPartitions, null, "msg-" + i)).get();
        }
        producer.close();

        final Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.getBootstrapServers());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singleton(topic));
        long start = System.currentTimeMillis();
        int receivedRecords = 0;
        final Map<Integer, List<String>> partitionRecords = new HashMap<>();
        while (receivedRecords < 100 && System.currentTimeMillis() - start < 10000) {
            final ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
            receivedRecords += records.count();
            records.forEach(record -> partitionRecords.computeIfAbsent(record.partition(), __ -> new ArrayList<>())
                    .add(record.value()));
        }
        consumer.close();
        Assert.assertEquals(partitionRecords.keySet(),
                IntStream.range(0, numPartitions).boxed().collect(Collectors.toList()));
        for (int i = 0; i < numPartitions; i++) {
            final int partition = i;
            final List<String> expectedValues = IntStream.range(0, messagesPerPartition)
                    .mapToObj(j -> "msg-" + (j * numPartitions + partition))
                    .collect(Collectors.toList());
            Assert.assertEquals(partitionRecords.get(i), expectedValues);
        }
    }
}
