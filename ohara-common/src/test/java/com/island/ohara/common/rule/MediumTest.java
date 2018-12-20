package com.island.ohara.common.rule;

import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.rules.Timeout;

public abstract class MediumTest extends OharaTest {
  @Rule public final Timeout globalTimeout = new Timeout(5, TimeUnit.MINUTES);
}
