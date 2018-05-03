package com.island.ohara.serialization

import java.io.ByteArrayOutputStream

import com.island.ohara.core.{Cell, Row, Table}
import com.island.ohara.io.CloseOnce._
import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestRW extends SmallTest with Matchers {
  private[this] val row_0 = Row(for (index <- 0 until 5) yield Cell.builder.name(index.toString).build(index))
  private[this] val row_1 = Row(for (index <- 0 until 10) yield Cell.builder.name(index.toString).build(index))
  private[this] val table = Table("test_table", row_0, row_1)

  @Test
  def testWriteEmptyTable(): Unit = {
    val bs = new ByteArrayOutputStream(100)
    TableWriter(bs, "empty_table", 0).close()
    doClose(TableReader(bs.toByteArray)) { tableReader =>
      {
        tableReader.id shouldBe "empty_table"
        tableReader.rowCount shouldBe 0
      }
    }
  }

  @Test
  def testWriteEmptyRow(): Unit = {
    val bs = new ByteArrayOutputStream(100)
    doClose(TableWriter(bs, "empty_row", 1)) { tableWriter =>
      {
        tableWriter.startRow(0).close()
      }
    }
    doClose(TableReader(bs.toByteArray)) { tableReader =>
      {
        tableReader.id shouldBe "empty_row"
        tableReader.rowCount shouldBe 1
        tableReader.next().cellCount shouldBe 0
      }
    }
  }

  @Test
  def testConversion(): Unit = {
    val bs = new ByteArrayOutputStream(100)
    doClose(TableWriter(bs, table.id, table.rowCount)) { tableWriter =>
      {
        table.foreach((row: Row) =>
          doClose(tableWriter.startRow(row.cellCount)) { rowWriter =>
            row.foreach(rowWriter.append(_))
        })
      }
    }

    def checkRow = (row: Row) => {
      row.zipWithIndex.foreach {
        case (cell, cellIndex) => cell.value.asInstanceOf[Int] shouldBe cellIndex
      }
    }

    def verify = (buf: Array[Byte]) => {
      doClose(TableReader(bs.toByteArray)) { tableReader =>
        {
          tableReader.id shouldBe "test_table"
          tableReader.rowCount shouldBe table.rowCount
          tableReader.zipWithIndex.foreach {
            case (row, rowIndex) => {
              rowIndex match {
                case 0 => {
                  row.cellCount shouldBe row_0.cellCount
                  checkRow(row)
                }
                case 1 => {
                  row.cellCount shouldBe row_1.cellCount
                  checkRow(row)
                }
              }
            }
          }
        }
      }
    }

    def verify2 = (buf: Array[Byte]) => {
      val table = TableReader.toTable(buf)
      table.id shouldBe "test_table"
      table.rowCount shouldBe table.rowCount
      table.seekRow(0).cellCount shouldBe row_0.cellCount
      checkRow(table seekRow 0)
      table.seekRow(1).cellCount shouldBe row_1.cellCount
      checkRow(table seekRow 1)
    }

    val input = Array(bs.toByteArray, TableWriter.toBytes(table))
    val verifier: Array[(Array[Byte]) => Unit] = Array(verify, verify2)
    input.foreach((bf: Array[Byte]) => verifier.foreach(_(bf)))
  }
}
