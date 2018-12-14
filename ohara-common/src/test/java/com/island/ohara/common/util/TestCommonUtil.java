package com.island.ohara.common.util;

import com.island.ohara.common.rule.SmallTest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;

public class TestCommonUtil extends SmallTest {

  @Test
  public void testTimer() throws InterruptedException {
    Assert.assertTrue(CommonUtil.current() != 0);
    CommonUtil.inject(() -> 0);
    Assert.assertEquals(0, CommonUtil.current());
    TimeUnit.SECONDS.sleep(2);
    Assert.assertEquals(0, CommonUtil.current());
    CommonUtil.reset();
    Assert.assertTrue(CommonUtil.current() != 0);
  }

  @Test
  public void testPath() {
    Assert.assertEquals("/ccc/abc", CommonUtil.path("/ccc", "abc"));
    Assert.assertEquals("/ccc/abc", CommonUtil.path("/ccc/", "abc"));
  }

  @Test
  public void testName() {
    Assert.assertEquals("ddd", CommonUtil.name("/abc/ddd"));
    Assert.assertEquals("aaa", CommonUtil.name("/abc/ddd/aaa"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void failedToExtractNameFromRootPath() {
    CommonUtil.name("/");
  }

  @Test
  public void testReplaceParentFolder() {
    Assert.assertEquals("ccc/ddd", CommonUtil.replaceParent("ccc", "/abc/ddd"));
    Assert.assertEquals("/a/ddd", CommonUtil.replaceParent("/a", "/abc/ddd"));
    Assert.assertEquals("/a/ddd", CommonUtil.replaceParent("/a", "/abc/ttt/ddd"));
    Assert.assertEquals("/a/ddd", CommonUtil.replaceParent("/a", "/abc/tt/t/ddd"));
  }

  @Test
  public void testGetAddress() {
    Assert.assertEquals(CommonUtil.address("localhost"), "127.0.0.1");
    Assert.assertEquals(CommonUtil.address("127.0.0.1"), "127.0.0.1");
  }

  @Test
  public void listEquals() {
    List<String> list = new MyList<>();
    list.add("a");
    list.add("b");
    List<String> list2 = new MyList<>(list);
    List<String> list3 = new MyList<>(list);
    list3.add("c");

    Assert.assertFalse(list.equals(list2));
    Assert.assertFalse(list.equals(list3));
    Assert.assertTrue(CommonUtil.equals(list, list2));
    Assert.assertFalse(CommonUtil.equals(list, list3));
  }

  @Test
  public void mapEquals() {
    Map<String, String> map = new MyMap<>();
    map.put("a", "valueA");
    map.put("b", "valueB");
    Map<String, String> map2 = new MyMap<>(map);
    Map<String, String> map3 = new MyMap<>(map);
    map3.put("c", "valueC");

    Assert.assertFalse(map.equals(map2));
    Assert.assertFalse(map.equals(map3));
    Assert.assertTrue(CommonUtil.equals(map, map2));
    Assert.assertFalse(CommonUtil.equals(map, map3));
  }

  @Test
  public void setEquals() {
    Set<String> set = new MySet<>();
    set.add("a");
    set.add("b");
    Set<String> set2 = new MySet<>(set);
    Set<String> set3 = new MySet<>(set);
    set3.add("c");

    Assert.assertFalse(set.equals(set2));
    Assert.assertFalse(set.equals(set3));
    Assert.assertTrue(CommonUtil.equals(set, set2));
    Assert.assertFalse(CommonUtil.equals(set, set3));
  }

  @Test
  public void listMap() {
    Map<String, String> map = new MyMap<>();
    map.put("a", "valueA");
    map.put("b", "valueB");
    Map<String, String> map3 = new MyMap<>(map);
    map3.put("c", "valueC");

    List<Map<String, String>> nestedMap = new MyList<>();
    nestedMap.add(new MyMap<>(map));
    nestedMap.add(new MyMap<>(map));

    List<Map<String, String>> nestedMap2 = new MyList<>(); // same
    nestedMap2.add(new MyMap<>(map));
    nestedMap2.add(new MyMap<>(map));

    List<Map<String, String>> nestedMap3 = new MyList<>(); // not same
    nestedMap3.add(new MyMap<>(map));
    nestedMap3.add(new MyMap<>(map3));

    Assert.assertFalse(nestedMap.equals(nestedMap2));
    Assert.assertFalse(nestedMap.equals(nestedMap3));
    Assert.assertTrue(CommonUtil.equals(nestedMap, nestedMap2));
    Assert.assertFalse(CommonUtil.equals(nestedMap, nestedMap3));
  }

  @Test
  public void listList() {

    List<String> list = new MyList<>();
    list.add("a");
    list.add("b");
    List<String> list3 = new MyList<>(list);
    list3.add("c");

    List<List<String>> nestedList = new MyList<>();
    nestedList.add(new MyList<>(list));
    nestedList.add(new MyList<>(list));

    List<List<String>> nestedList2 = new MyList<>();
    nestedList2.add(new MyList<>(list));
    nestedList2.add(new MyList<>(list));

    List<List<String>> nestedList3 = new MyList<>();
    nestedList3.add(new MyList<>(list));
    nestedList3.add(new MyList<>(list3));

    Assert.assertFalse(nestedList.equals(nestedList2));
    Assert.assertFalse(nestedList.equals(nestedList3));
    Assert.assertTrue(CommonUtil.equals(nestedList, nestedList2));
    Assert.assertFalse(CommonUtil.equals(nestedList, nestedList3));
  }

  @Test
  public void listSet() {
    Set<String> set = new MySet<>();
    set.add("a");
    set.add("b");
    Set<String> set2 = new MySet<>(set);
    Set<String> set3 = new MySet<>(set);
    set3.add("c");

    List<Set<String>> nestedList = new MyList<>();
    nestedList.add(new MySet<>(set));
    nestedList.add(new MySet<>(set2));

    List<Set<String>> nestedList2 = new MyList<>(); // same
    nestedList2.add(new MySet<>(set));
    nestedList2.add(new MySet<>(set2));

    List<Set<String>> nestedList3 = new MyList<>(); // not same
    nestedList3.add(new MySet<>(set));
    nestedList3.add(new MySet<>(set3));

    Assert.assertFalse(nestedList.equals(nestedList2));
    Assert.assertFalse(nestedList.equals(nestedList3));
    Assert.assertTrue(CommonUtil.equals(nestedList, nestedList2));
    Assert.assertFalse(CommonUtil.equals(nestedList, nestedList3));
  }

  @Test
  public void mapList() {
    List<String> list = new MyList<>();
    list.add("a");
    list.add("b");
    List<String> list3 = new MyList<>(list);
    list3.add("c");

    Map<String, List<String>> nestedMap = new MyMap<>();
    nestedMap.put("key", new MyList<>(list));
    nestedMap.put("key2", new MyList<>(list));

    Map<String, List<String>> nestedMap2 = new MyMap<>();
    nestedMap2.put("key", new MyList<>(list));
    nestedMap2.put("key2", new MyList<>(list));

    Map<String, List<String>> nestedMap3 = new MyMap<>();
    nestedMap3.put("key", new MyList<>(list));
    nestedMap3.put("key2", new MyList<>(list3));

    Assert.assertFalse(nestedMap.equals(nestedMap2));
    Assert.assertFalse(nestedMap.equals(nestedMap3));
    Assert.assertTrue(CommonUtil.equals(nestedMap, nestedMap2));
    Assert.assertFalse(CommonUtil.equals(nestedMap, nestedMap3));
  }

  @Test
  public void mapSet() {
    Set<String> set = new MySet<>();
    set.add("a");
    set.add("b");
    Set<String> set3 = new MySet<>(set);
    set3.add("c");

    Map<String, Set<String>> nestedMap = new MyMap<>();
    nestedMap.put("key", new MySet<>(set));
    nestedMap.put("key2", new MySet<>(set));

    Map<String, Set<String>> nestedMap2 = new MyMap<>();
    nestedMap2.put("key", new MySet<>(set));
    nestedMap2.put("key2", new MySet<>(set));

    Map<String, Set<String>> nestedMap3 = new MyMap<>();
    nestedMap3.put("key", new MySet<>(set));
    nestedMap3.put("key2", new MySet<>(set3));

    Assert.assertFalse(nestedMap.equals(nestedMap2));
    Assert.assertFalse(nestedMap.equals(nestedMap3));
    Assert.assertTrue(CommonUtil.equals(nestedMap, nestedMap2));
    Assert.assertFalse(CommonUtil.equals(nestedMap, nestedMap3));
  }

  @Test
  public void mapMap() {
    Map<String, String> map = new MyMap<>();
    map.put("a", "valueA");
    map.put("b", "valueB");
    Map<String, String> map3 = new MyMap<>(map);
    map3.put("c", "valueC");

    Map<String, Map<String, String>> nestedMap = new MyMap<>();
    nestedMap.put("key", new MyMap<>(map));
    nestedMap.put("key2", new MyMap<>(map));

    Map<String, Map<String, String>> nestedMap2 = new MyMap<>();
    nestedMap2.put("key", new MyMap<>(map));
    nestedMap2.put("key2", new MyMap<>(map));

    Map<String, Map<String, String>> nestedMap3 = new MyMap<>();
    nestedMap3.put("key", new MyMap<>(map));
    nestedMap3.put("key2", new MyMap<>(map3));

    Assert.assertFalse(nestedMap.equals(nestedMap2));
    Assert.assertFalse(nestedMap.equals(nestedMap3));
    Assert.assertTrue(CommonUtil.equals(nestedMap, nestedMap2));
    Assert.assertFalse(CommonUtil.equals(nestedMap, nestedMap3));
  }

  private static class MyList<T> extends ArrayList<T> {

    MyList() {
      super();
    }

    MyList(List<T> list) {
      super(list);
    }

    @Override
    public boolean equals(Object o) {
      return false;
    }
  }

  private static class MyMap<K, V> extends HashMap<K, V> {

    MyMap() {
      super();
    }

    MyMap(Map<K, V> m) {
      super(m);
    }

    @Override
    public boolean equals(Object o) {
      return false;
    }
  }

  private static class MySet<T> extends HashSet<T> {

    MySet() {
      super();
    }

    MySet(Set<T> m) {
      super(m);
    }

    @Override
    public boolean equals(Object o) {
      return false;
    }
  }
}
