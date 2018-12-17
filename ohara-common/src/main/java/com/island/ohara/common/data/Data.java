package com.island.ohara.common.data;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * This is a helper class which auto gernerates basic methods If your Data is very special , like
 * blow
 *
 * <pre>
 * "Field:  Object o1; Object o2;"
 * "Unnormally , you compare o1.equal(o2) && o2.equal(o1)"
 * </pre>
 *
 * <p>You need to custom the equals by self !!!!!!!!!!!!!!!!!!
 */
public abstract class Data {

  static {
    // default byte array -> [97, 97, 117]
    ToStringBuilder.setDefaultStyle(ToStringStyle.JSON_STYLE);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(17, 37, this);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}
