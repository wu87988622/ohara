package com.island.ohara.ostreams;

public class Consumed {

//    protected Serde<K> keySerde;
//    protected Serde<V> valueSerde;
//
//    private Consumed(final Serde<K> keySerde, final Serde<V> valueSerde){
//        this.keySerde = keySerde;
//        this.valueSerde = valueSerde;
//    }

    public static <K,V> org.apache.kafka.streams.Consumed<K,V> with(final Serde<K> keySerde, final Serde<V> valueSerde){

        return org.apache.kafka.streams.Consumed.with(keySerde, valueSerde);
    }
}
