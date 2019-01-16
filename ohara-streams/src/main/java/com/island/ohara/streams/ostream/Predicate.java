package com.island.ohara.streams.ostream;

public interface Predicate<K, V> {

  boolean predicate(final K key, final V value);

  final class TruePredicate<K, V> implements org.apache.kafka.streams.kstream.Predicate<K, V> {
    final Predicate<K, V> truePredicate;

    TruePredicate(Predicate<K, V> predicate) {
      this.truePredicate = predicate;
    }

    @Override
    public boolean test(final K key, final V value) {
      return this.truePredicate.predicate(key, value);
    }
  }
}
