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

import base.TestBase
import creatingtables.PrimaryKey.{TimeLogKey, UserKey}
import creatingtables.Row.{TimeLogRow, UserRow}
import creatingtables.Table.{TimeLogTable, UserTable}

class TableSpec extends TestBase {

  "TableSpec" in {
    import swaydb._

    val db = persistent.Map[PrimaryKey, Row](dir = dir).get

    //write key-values to each Table
    (1 to 10) foreach {
      i =>
        //write to User table
        db.put(UserKey(i), UserRow(age = i, name = s"User $i")).get
        //write to Task table
        db.put(TimeLogKey(i), TimeLogRow(time = i, task = s"Task done $i")).get
    }

    //iterating Task keys
    db
      .from(TimeLogKey(1))
      .takeWhileKey(_.table == TimeLogTable)
      .foreach(println)

    //iterating User keys
    db
      .from(UserKey(1))
      .takeWhileKey(_.table == UserTable)
      .foreach(println)
  }
}