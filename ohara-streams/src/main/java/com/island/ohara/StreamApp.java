package com.island.ohara;

public abstract class StreamApp {

  /**
   * Running a standalone streamApp. This method is usually called from the main(). It must not be
   * called more than once otherwise exception will be thrown.
   *
   * <p>Usage :
   *
   * <pre>
   *   public static void main(String[] args){
   *     StreamApp.runStreamApp(MyStreamApp.class);
   *   }
   * </pre>
   *
   * @param theClass the streamapp class that is constructed and extends from {@link StreamApp}
   * @param params parameters of {@code theClass} contructor used
   */
  public static void runStreamApp(Class<? extends StreamApp> theClass, Object... params) {

    if (StreamApp.class.isAssignableFrom(theClass)) {
      Class<? extends StreamApp> streamAppClass = theClass;
      StreamAppImpl.launchApplication(streamAppClass, params);
    } else {
      throw new RuntimeException(
          "Error: " + theClass + " is not a subclass of " + StreamApp.class.getName());
    }
  }

  /**
   * running a standalone streamapp. This method will try to find each methods in current thread
   * that is extends from {@code StreamApp}
   */
  public static void runStreamApp() {

    String entryClassName = null;
    // Find correct class to call
    StackTraceElement[] stack = Thread.currentThread().getStackTrace();

    boolean found = false;
    for (StackTraceElement se : stack) {
      // Skip entries until we get to the entry for this class
      String className = se.getClassName();
      String methodName = se.getMethodName();
      if (found) {
        entryClassName = className;
        break;
      } else if (StreamApp.class.getName().equals(className) && "runStreamApp".equals(methodName)) {

        found = true;
      }
    }
    if (entryClassName == null) {
      throw new RuntimeException("Unable to find StreamApp class.");
    }

    try {
      Class theClass =
          Class.forName(entryClassName, false, Thread.currentThread().getContextClassLoader());
      if (StreamApp.class.isAssignableFrom(theClass)) {
        Class<? extends StreamApp> streamAppClass = theClass;
        StreamAppImpl.launchApplication(streamAppClass);
      } else {
        throw new RuntimeException(
            "Error: " + theClass + " is not a subclass of " + StreamApp.class.getName());
      }
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  /** Constructor */
  public StreamApp() {}

  /**
   * User defined initialize stage before running streamapp
   *
   * @throws Exception
   */
  public void init() throws Exception {}

  /**
   * Entry function. <b>Usage:</b>
   *
   * <pre>
   *   OStream.builder()
   *   .fromTopic("topic-name")
   *   ...
   * </pre>
   */
  public abstract void start() throws Exception;

  public void stop() throws Exception {}
}
