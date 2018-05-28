package com.island.ohara.configurator.data

sealed abstract class JobStatus {

  /**
    * the name to this job status.
    * NOTED: DON'T change the class name
    * @return status name
    */
  def name: String = getClass.getSimpleName
}
case object RUNNING extends JobStatus
case object PAUSE extends JobStatus
case object STOP extends JobStatus

object JobStatus {

  /**
    * @return a array of all job status
    */
  def all = Array(RUNNING, RUNNING, STOP)

  /**
    * seek the data type by the type name
    * @param name index of job status
    * @return job status
    */
  def of(name: String): JobStatus = all.find(_.name.equalsIgnoreCase(name)).get
}
