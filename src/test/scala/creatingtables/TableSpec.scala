package creatingtables

import base.TestBase
import creatingtables.PrimaryKey.{TimeLogKey, UserKey}
import creatingtables.Row.{TimeLogRow, UserRow}
import creatingtables.Table.{TimeLogTable, UserTable}

class TableSpec extends TestBase {

  "TableSpec" in {
    import swaydb._
    implicit val bag = Bag.less

    val db = persistent.Map[PrimaryKey, Row, Nothing, Bag.Less](dir = dir).get

    //write key-values to each Table
    (1 to 10) foreach {
      i =>
        //write to User table
        db.put(UserKey(i), UserRow(age = i, name = s"User $i"))
        //write to Task table
        db.put(TimeLogKey(i), TimeLogRow(time = i, task = s"Task done $i"))
    }

    //iterating Task keys
    db
      .from(TimeLogKey(1))
      .stream
      .takeWhile(_._1.table == TimeLogTable)
      .foreach(println)

    //iterating User keys
    db
      .from(UserKey(1))
      .stream
      .takeWhile(_._1.table == UserTable)
      .foreach(println)
  }
}