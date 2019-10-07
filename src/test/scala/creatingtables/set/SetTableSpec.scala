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

import base.TestBase
import creatingtables.set.Row.{TimeLogRow, UserRow}
import creatingtables.set.Table.{TimeLogTable, UserTable}

class SetTableSpec extends TestBase {

  "SetTableSpec" in {
    import Row.rowSortOrder
    import swaydb._

    //A Set database (SwaySetDB) is different to a key-value database (SwayMapDB).
    //It stores Primary key and row data together and requires only one disk seek for fetching both data.
    //A partial key ordering is used for storing data required fields.
    val db = persistent.Set[Row, Nothing, IO.ApiIO](dir = dir).get

    //write key-values to each Table
    (1 to 10) foreach {
      i =>
        //write to User table
        db.add(UserRow(key = i, age = 20 + 1, name = s"User $i")).get
        //create a task in Time log table
        db.add(TimeLogRow(key = i, time = System.currentTimeMillis(), task = s"Task done $i")).get
    }

    //iterating TimeLogs
    db
      .from(TimeLogRow(1, 0, ""))
      .takeWhile(_.table == TimeLogTable)
      .foreach {
        row =>
          println("Time log: " + row)
      }

    //iterating Users
    db
      .from(UserRow(1, 0, ""))
      .takeWhile(_.table == UserTable)
      .foreach {
        row =>
          println("User: " + row)
      }
  }
}