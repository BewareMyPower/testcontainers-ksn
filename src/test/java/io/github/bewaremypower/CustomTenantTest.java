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

import io.github.bewaremypower.testcontainers.KsnCluster;
import io.github.bewaremypower.testcontainers.KsnClusterConfig;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class CustomTenantTest {

    private static final String TENANT = "my-tenant";
    private KsnCluster cluster;

    @BeforeClass
    public void setup() throws IOException, InterruptedException {
        cluster = new KsnCluster(new KsnClusterConfig().setNumBrokers(1).addConfig("kafkaTenant", TENANT));
    }

    @AfterClass(alwaysRun = true)
    public void cleanup() {
        if (cluster != null) {
            cluster.close();
        }
    }

    @Test
    public void test() throws Exception {
        final Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // These two topics represent the same topic
        final String shortTopic = "my-topic";
        final String longTopic = TENANT + "/default/my-topic";

        final KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);
        final RecordMetadata metadata = producer.send(new ProducerRecord<>(shortTopic, "value")).get();
        Assert.assertEquals(metadata.topic(), "my-topic");
        Assert.assertEquals(metadata.partition(), 0);
        Assert.assertEquals(metadata.offset(), 0);
        producer.close();


        final Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.getBootstrapServers());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singleton(longTopic));
        final List<String> values = new ArrayList<>();
        final long start = System.currentTimeMillis();
        while (values.isEmpty() && System.currentTimeMillis() - start < 10000) {
            consumer.poll(Duration.ofSeconds(1)).forEach(r -> values.add(r.value()));
        }
        Assert.assertEquals(values, Collections.singleton("value"));
    }
}
