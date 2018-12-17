package com.island.ohara.kafka;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * a fluent-style sender. kafak.ProducerRecord has many fields and most from them are nullable. It
 * makes kafak.ProducerRecord's constructor complicated. This class has fluent-style methods helping
 * user to fill the fields they have.
 *
 * @tparam K key type
 * @tparam V value type
 */
public abstract class Sender<K, V> {
  private Optional<Integer> partition = Optional.empty();
  private List<Header> headers = Collections.emptyList();
  private Optional<K> key = Optional.empty();
  private Optional<V> value = Optional.empty();
  private Optional<Long> timestamp = Optional.empty();

  protected Optional<Integer> partition() {
    return partition;
  }

  protected List<Header> headers() {
    return headers;
  }

  protected Optional<K> key() {
    return key;
  }

  protected Optional<V> value() {
    return value;
  }

  protected Optional<Long> timestamp() {
    return timestamp;
  }

  public Sender<K, V> partition(int partition) {
    this.partition = Optional.of(partition);
    return this;
  }

  public Sender<K, V> header(Header header) {
    this.headers = Collections.singletonList(header);
    return this;
  }

  public Sender<K, V> headers(List<Header> headers) {
    this.headers = headers;
    return this;
  }

  public Sender<K, V> key(K key) {
    this.key = Optional.ofNullable(key);
    return this;
  }

  public Sender<K, V> value(V value) {
    this.value = Optional.ofNullable(value);
    return this;
  }

  public Sender<K, V> timestamp(long timestamp) {
    this.timestamp = Optional.of(timestamp);
    return this;
  }

  /** send the record to brokers with async future */
  public Future<RecordMetadata> send(String topic) {

    CompletableFuture<RecordMetadata> completableFuture = new CompletableFuture<>();
    send(
        topic,
        new Handler<RecordMetadata>() {

          @Override
          public void doException(Exception e) {
            completableFuture.completeExceptionally(e);
          }

          @Override
          public void doHandle(RecordMetadata o) {
            completableFuture.complete(o);
          }
        });

    return completableFuture;
  }

  /**
   * send the record to brokers with callback
   *
   * @param handler invoked after the record is completed or failed
   */
  public void send(String topic, Handler<RecordMetadata> handler) {
    doSend(topic, handler);
  };

  protected abstract void doSend(String topic, Handler<RecordMetadata> handler);

  public static interface Handler<T> {
    void doException(Exception e);

    void doHandle(T t);
  }
}
