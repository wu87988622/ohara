package com.island.ohara.configurator.data

import com.island.ohara.reflection.ClassName

sealed abstract class JobStatus extends ClassName

object JobStatus {

  object RUNNING extends JobStatus
  object PAUSE extends JobStatus
  object STOP extends JobStatus

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
