# testcontainers-ksn

The testcontainers module for Kafka-on-StreamNative (KSN), which is a private version of [KoP](https://github.com/streamnative/kop).

## Get Started

### Install and import this dependency

This project requires JDK 8 or higher.

Currently, this project is not published to Maven Central repository, so you have to install this dependency to your local repository manually:

```bash
mvn clean install -DskipTests
```

Then, you can import this dependency as well as the `testcontainers` dependency:

```xml
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>testcontainers</artifactId>
  <version>1.19.1</version>
</dependency>
<dependency>
  <groupId>io.github.bewaremypower</groupId>
  <artifactId>testcontainers-ksn</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

> **NOTE**:
>
> For simplicity, you can just fork this repository and add tests under [the tests directory](./src/test/java/io/github/bewaremypower).

### Start and stop the KoP cluster

After importing the dependency, you can set up your test like:

```java
// Start the KSN cluster. By default, 1 ZK node, 2 bookie nodes and 2 broker nodes will be deployed.
final KsnCluster cluster = new KsnCluster();
/* TODO: add your tests... */
// Stop the KSN cluster.
cluster.close();
```

You can customize the configs by passing a customized `KsnClusterConfig` object to the `KsnCluster`'s constructor.

```java
final KsnCluster cluster = new KsnCluster(new KsnClusterConfig()
        .setNumBrokers(1)
        .addConfig("kafkaTenant", "my-tenant"));
```

See [`KsnClusterConfig`](./src/main/java/io/github/bewaremypower/testcontainers/KsnClusterConfig.java) for more details.

### Create a Kafka client to connect a `KsnCluster`

`KsnCluster#getBootstrapServers()` returns the bootstrap servers config to create a Kafka client.

Example:

```java
// Create a Kafka admin
final Properties adminProps = new Properties();
adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.getBootstrapServers());
final AdminClient client = AdminClient.create(adminProps);

// Create a Kafka producer
final Properties producerProps = new Properties();
producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.getBootstrapServers());
producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
final KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);

// Create a Kafka consumer
final Properties consumerProps = new Properties();
consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.getBootstrapServers());
consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "group");
consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
```
