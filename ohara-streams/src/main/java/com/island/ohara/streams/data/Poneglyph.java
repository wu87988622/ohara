package com.island.ohara.streams.data;

import java.io.Serializable;
import java.util.HashSet;

public final class Poneglyph implements Serializable {
  private final HashSet<Stele> steles = new HashSet<>();

  public void addStele(Stele another) {
    this.steles.add(another);
  }

  public HashSet<Stele> getSteles() {
    return this.steles;
  }

  @Override
  public String toString() {
    return String.format("steles : %s" + getSteles());
  }
}
