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

package quickstart

import base.TestBase

class QuickStartPersistentSpec extends TestBase {

  "Quick start" in {

    import swaydb._ //import database API
    import swaydb.serializers.Default._ //import default serializers

    //Create a persistent database. If the directories do not exist, they will be created.
    val db = SwayDB.persistent[Int, String](dir = dir.resolve("disk1")).assertSuccess

    db.put(1, "one").assertSuccess
    db.get(1).assertSuccess should contain("one")
    db.remove(1).assertSuccess
    db.batch(
      Batch.Put(key = 1, value = "one value"),
      Batch.Update(from = 1, to = 100, value = "range update"),
      Batch.Remove(key = 1),
      Batch.Remove(from = 1, to = 100)
    ).assertSuccess

    //write 100 key-values
    (1 to 100) foreach { i => db.put(key = i, value = i.toString).assertSuccess }
    //Iteration: fetch all key-values withing range 10 to 90, update values and batch write updated key-values
    db
      .from(10)
      .tillKey(_ <= 90)
      .map {
        case (key, value) =>
          (key, value + "_updated")
      } andThen {
      updatedKeyValues =>
        db.batchPut(updatedKeyValues).assertSuccess
    }
    //assert the key-values were updated
    db.from(10).tillKey(_ <= 90).foreach(_._2 should endWith("_updated"))

    db.cacheFunction("myFunctionId", _ + "updated again with function").assertSuccess
    db.update(from = 10, to = 90, functionId = "myFunctionId").assertSuccess
    //assert the key-values were updated
    db.from(10).tillKey(_ <= 90).foreach(_._2 should endWith("updated again with function"))
  }
}