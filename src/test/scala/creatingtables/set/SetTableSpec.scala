package creatingtables.set

import base.TestBase
import creatingtables.set.Row.{TimeLogRow, UserRow}
import creatingtables.set.Table.{TimeLogTable, UserTable}

class SetTableSpec extends TestBase {

  "SetTableSpec" in {
    import Row.rowSortOrder
    import swaydb._
    implicit val bag = Bag.less

    //A Set database (SwaySetDB) is different to a key-value database (SwayMapDB).
    //It stores Primary key and row data together and requires only one disk seek for fetching both data.
    //A partial key ordering is used for storing data required fields.
    val db = persistent.Set[Row, Nothing, Bag.Less](dir = dir)

    //write key-values to each Table
    (1 to 10) foreach {
      i =>
        //write to User table
        db.add(UserRow(key = i, age = 20 + 1, name = s"User $i"))
        //create a task in Time log table
        db.add(TimeLogRow(key = i, time = System.currentTimeMillis(), task = s"Task done $i"))
    }

    //iterating TimeLogs
    db
      .stream
      .from(TimeLogRow(1, 0, ""))
      .takeWhile(_.table == TimeLogTable)
      .foreach {
        row =>
          println("Time log: " + row)
      }

    //iterating Users
    db
      .stream
      .from(UserRow(1, 0, ""))
      .takeWhile(_.table == UserTable)
      .foreach {
        row =>
          println("User: " + row)
      }
  }
}