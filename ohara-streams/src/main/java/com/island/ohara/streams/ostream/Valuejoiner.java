package com.island.ohara.streams.ostream;

public interface Valuejoiner<V1, V2, VR> {
  VR valuejoiner(final V1 value1, final V2 value2);

  class TrueValuejoiner<V1, V2, VR>
      implements org.apache.kafka.streams.kstream.ValueJoiner<V1, V2, VR> {

    private final Valuejoiner<V1, V2, VR> trueValuejoiner;

    TrueValuejoiner(Valuejoiner<V1, V2, VR> valuejoiner) {
      this.trueValuejoiner = valuejoiner;
    }

    @Override
    public VR apply(V1 value1, V2 value2) {
      return this.trueValuejoiner.valuejoiner(value1, value2);
    }
  }
}
