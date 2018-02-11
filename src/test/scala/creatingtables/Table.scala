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

package creatingtables

import creatingtables.Table.{TimeLogTable, UserTable}
import swaydb.data.slice.Slice
import swaydb.data.util.ByteSizeOf
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

sealed trait PrimaryKey {
  val key: Long
  val table: Table
}

object PrimaryKey {

  /**
    * Primary Key for [[UserTable]]
    */
  case class UserKey(key: Long) extends PrimaryKey {
    override val table: Table = UserTable
  }
  /**
    * Primary key for [[TimeLogTable]]
    */
  case class TimeLogKey(key: Long) extends PrimaryKey {
    override val table: Table = TimeLogTable
  }

  //Serializer for UserKey and TimeLogKey
  implicit object KeySerializer extends Serializer[PrimaryKey] {
    override def write(data: PrimaryKey): Slice[Byte] = {
      //Serialise key-values with tableId as the key prefix.
      //Create a Slice of 12 bytes, 4 byte for tableId: Int and 8 bytes for primaryKey: Long
      Slice.create(ByteSizeOf.int + ByteSizeOf.long)
        .addInt(data.table.tableId)
        .addLong(data.key)
    }

    override def read(data: Slice[Byte]): PrimaryKey = {
      //create a reader on the slice to read the data
      val reader = data.createReader()
      if (reader.readInt() == UserTable.tableId) //read the tableId and check if it's a User or Product table.
        UserKey(reader.readLong())
      else
        TimeLogKey(reader.readLong())
    }
  }
}

sealed trait Row {
  val table: Table
}

object Row {

  //row data for UserTable
  case class UserRow(age: Int, name: String) extends Row {
    override val table: Table = UserTable
  }

  //row data for TimeLogTable
  case class TimeLogRow(time: Long, task: String) extends Row {
    override val table: Table = TimeLogTable
  }

  //Row serializer
  implicit object UserRowSerializer extends Serializer[Row] {
    override def write(data: Row): Slice[Byte] =
      data match {
        case UserRow(age, name) =>
          Slice.create(100)
            .addInt(data.table.tableId)
            .addInt(age)
            .addString(name)
            .close() //close the slice so that empty bytes in the Slice are discarded by re-adjusting the toOffset.

        case TimeLogRow(time, task) =>
          Slice.create(100)
            .addInt(data.table.tableId)
            .addLong(time)
            .addString(task)
            .close() //close the slice so that empty bytes in the Slice are discarded by re-adjusting the toOffset.
      }

    override def read(data: Slice[Byte]): Row = {
      //create a reader on the slice to read the data
      val reader = data.createReader()
      if (reader.readInt() == UserTable.tableId) //read the tableId and check if it's a User or Product.
        UserRow(reader.readInt(), reader.readString())
      else
        TimeLogRow(reader.readLong(), reader.readString())
    }
  }
}
