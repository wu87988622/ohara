package com.island.ohara.streams;

import com.island.ohara.OStreams;
import com.island.ohara.StreamsBuilder;
import com.island.ohara.Topology;
import com.island.ohara.integration.With3Brokers;
import com.island.ohara.kafka.KafkaClient;
import com.island.ohara.ostreams.Serdes;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.island.ohara.streams.AirlineDataImporter.createKafkaConsumer;
import static com.island.ohara.streams.AirlineDataImporter.createKafkaProducer;

public class TestOStreams extends With3Brokers {

    private final String appid = "test-app";
    private final String fromTopic = "stream-in";
    private final String toTopic = "stream-out";

    private final KafkaClient client = KafkaClient.of(testUtil().brokersConnProps());
    private final String simple_string = "this is a test sentence.";
    private final KafkaProducer<String, String> producer = createKafkaProducer(client.brokers());
    private final KafkaConsumer<String, String> consumer = createKafkaConsumer(client.brokers());

    @Before
    public void tearUp() {
        int partitions = 3;
        short replications = 1;
        try {
            client.topicCreator().numberOfPartitions(partitions).numberOfReplications(replications).create(fromTopic);
            client.topicCreator().numberOfPartitions(partitions).numberOfReplications(replications).create(toTopic);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    @Test
    public void testSimpleApplication() {
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(fromTopic, simple_string);
        Future<RecordMetadata> f = producer.send(producerRecord);
        try {
            f.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

        consumer.subscribe(Collections.singletonList(fromTopic));
        ConsumerRecords<String, String> m = consumer.poll(10000);
        consumer.commitAsync();
        Assert.assertEquals(1, m.count());
        m.forEach(record -> {
            Assert.assertEquals(simple_string, record.value());
        });

        OStreams<String, String> oStreams = StreamsBuilder.of(Serdes.STRING, Serdes.STRING)
                .appid(appid).bootstrapServers(client.brokers())
                .fromTopic(fromTopic)
                .toTopic(toTopic)
                .cleanStart()
                .build();

        oStreams.filter((key, value) -> value != null)
                .mapValues(String::toUpperCase);

        try (Topology topology = oStreams.construct()) {
            System.out.println(topology.describe());
            topology.start();
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        consumer.unsubscribe();
        consumer.subscribe(Collections.singletonList(toTopic));
        try {
            ConsumerRecords<String, String> messages = consumer.poll(10000);
            consumer.commitAsync();
            Assert.assertEquals(1, messages.count());
            messages.forEach(record -> {
                Assert.assertEquals(simple_string.toUpperCase(), record.value());
            });
        } finally {
            consumer.close();
        }
    }

    @After
    public void tearDown() {
        client.close();
        producer.close();
    }
}
