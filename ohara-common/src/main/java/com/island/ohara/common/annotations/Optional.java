package com.island.ohara.common.annotations;

/**
 * There are many builder in ohara. Normally some methods, which used to assign value to member, are
 * not required. For example, the value can be generated automatically. This annotation is used to
 * "hint" developer that the methods which can be "ignored" in production purpose.
 */
public @interface Optional {
  String value() default "This method or member is not required to call or use";
}
