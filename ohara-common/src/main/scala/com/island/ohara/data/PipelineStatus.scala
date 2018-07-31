package com.island.ohara.data

import com.island.ohara.reflection.ClassName

sealed abstract class PipelineStatus extends ClassName

object PipelineStatus {

  object RUNNING extends PipelineStatus
  object PAUSE extends PipelineStatus
  object STOP extends PipelineStatus

  /**
    * @return a array of all job status
    */
  def all = Array(RUNNING, RUNNING, STOP)

  /**
    * seek the data type by the type name
    * @param name index of pipeline status
    * @return pipeline status
    */
  def of(name: String): PipelineStatus = all.find(_.name.equalsIgnoreCase(name)).get
}
