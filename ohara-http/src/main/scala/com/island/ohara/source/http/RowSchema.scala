package com.island.ohara.source.http

import com.island.ohara.common.data.DataType

/**
  * (Cell#name, CellType)
  *
  * @param schema
  */
final case class RowSchema(schema: Vector[(String, DataType)])
