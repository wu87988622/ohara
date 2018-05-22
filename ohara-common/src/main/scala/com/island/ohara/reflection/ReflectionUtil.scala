package com.island.ohara.reflection

/**
  * helper methods used to instantiate the classes dynamically.
  * NOTED: the parameter is made up of 1) interface type 2) object. We can't guess the method signatures since java dynamically call
  * doesn't support to auto cast.
  */
object ReflectionUtil {

  /**
    * build the instance by the class name.
    * @param name class full name
    * @param xface expected interface
    * @param parameters used to construct the object. _1 is the interface of parameter. _2 is the object
    * @tparam U instance type
    * @return instance
    */
  def instantiate[U](name: String, xface: Class[U], parameters: (Class[_], Any)): U =
    instantiate(name, xface, Array(parameters))

  /**
    * build the instance by the class name. NOTED: this method return the option rather than throwing the exception when it fail to instantiate the class.
    * @param name class full name
    * @param xface expected interface
    * @param parameters used to construct the object. _1 is the interface of parameter. _2 is the object
    * @tparam U instance type
    * @return instance or None
    */
  def tryInstantiate[U](name: String, xface: Class[U], parameters: (Class[_], Any)): Option[U] =
    tryInstantiate(name, xface, Array(parameters))

  /**
    * build the instance by the class name. NOTED: this method return the option rather than throwing the exception when it fail to instantiate the class.
    * @param name class full name
    * @param xface expected interface
    * @param parameters used to construct the object. _1 is the interface of parameter. _2 is the object
    * @tparam U instance type
    * @return instance or None
    */
  def tryInstantiate[U](name: String,
                        xface: Class[U],
                        parameters: Array[(Class[_], Any)] = Array[(Class[_], Any)]()): Option[U] = {
    try Some(instantiate(name, xface, parameters))
    catch {
      case _: Throwable => None
    }
  }

  /**
    * build the instance by the class name.
    * @param name class full name
    * @param xface expected interface
    * @param parameters used to construct the object. _1 is the interface of parameter. _2 is the object
    * @tparam U instance type
    * @return instance
    */
  def instantiate[U](name: String,
                     xface: Class[U],
                     parameters: Array[(Class[_], Any)] = Array[(Class[_], Any)]()): U = {
    val clz = Class.forName(name)
    if (!xface.isAssignableFrom(clz)) {
      throw new RuntimeException(s"expected interface:${xface.getName} but actual:${clz.getSuperclass}")
    }
    val ctr =
      if (parameters.isEmpty) clz.getDeclaredConstructor()
      else clz.getDeclaredConstructor(parameters.map(_._1): _*)
    // TODO: allow to create the instance through the non-public constructor?  by chia
    ctr.setAccessible(true)
    if (parameters.isEmpty) ctr.newInstance().asInstanceOf[U]
    else ctr.newInstance(parameters.map(_._2.asInstanceOf[Object]): _*).asInstanceOf[U]
  }
}
