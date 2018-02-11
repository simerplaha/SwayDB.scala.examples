/*
 * Copyright (C) 2018 Simer Plaha (@simerplaha)
 *
 * This file is a part of SwayDB.
 *
 * SwayDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * SwayDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with SwayDB. If not, see <https://www.gnu.org/licenses/>.
 */

package creatingtables.set

import creatingtables.set.Table.{TimeLogTable, UserTable}
import swaydb.data.slice.Slice
import swaydb.data.util.ByteSizeOf
import swaydb.order.KeyOrder
import swaydb.serializers.Serializer

sealed trait Table {
  val tableId: Int
}

object Table {

  object UserTable extends Table {
    val tableId = 1
  }

  object TimeLogTable extends Table {
    val tableId = 2
  }
}

sealed trait Row {
  val key: Long
  val table: Table
}

object Row {

  case class UserRow(key: Long, age: Int, name: String) extends Row {
    override val table: Table = UserTable
  }

  case class TimeLogRow(key: Long, time: Long, task: String) extends Row {
    override val table: Table = TimeLogTable
  }

  implicit object UserRowSerializer extends Serializer[Row] {
    override def write(data: Row): Slice[Byte] =
      data match {
        case UserRow(key, age, name) =>
          Slice.create(100)
            .addInt(data.table.tableId)
            .addLong(key)
            .addInt(age)
            .addString(name)
            .close()

        case TimeLogRow(key, time, task) =>
          Slice.create(100)
            .addInt(data.table.tableId)
            .addLong(key)
            .addLong(time)
            .addString(task)
            .close()
      }

    override def read(data: Slice[Byte]): Row = {
      //create a reader on the slice to read the data
      val reader = data.createReader()
      if (reader.readInt() == UserTable.tableId) //read the tableId and check if it's a User or Product.
        UserRow(reader.readLong(), reader.readInt(), reader.readString())
      else
        TimeLogRow(reader.readLong(), reader.readLong(), reader.readString())
    }
  }

  //provide partial ordering on the fields - tableId & key.
  implicit val rowSortOrder = new Ordering[Slice[Byte]] {
    def compare(slice1: Slice[Byte], slice2: Slice[Byte]): Int = {
      KeyOrder.default.compare(
        //int (tableId) + long (key)
        slice1.take(ByteSizeOf.int + ByteSizeOf.long),
        slice2.take(ByteSizeOf.int + ByteSizeOf.long)
      )
    }
  }
}
