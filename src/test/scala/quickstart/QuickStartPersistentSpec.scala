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

    import swaydb._
    import swaydb.serializers.Default._ //import default serializers

    //Create a persistent database. If the directories do not exist, they will be created.
    val db = persistent.Map[Int, String, Nothing, IO.ApiIO](dir = dir.resolve("disk1")).get

    db.put(1, "one").get
    db.get(1).get should contain("one")
    db.remove(1).get
    db.commit(
      Prepare.Put(key = 1, value = "one value"),
      Prepare.Update(from = 1, to = 100, value = "range update"),
      Prepare.Remove(key = 1),
      Prepare.Remove(from = 1, to = 100)
    ).get

    //write 100 key-values
    (1 to 100) foreach { i => db.put(key = i, value = i.toString).get }
    //Iteration: fetch all key-values withing range 10 to 90, update values and batch write updated key-values
    db
      .from(10)
      .takeWhile(_._1 <= 90)
      .map {
        case (key, value) =>
          (key, value + "_updated")
      }
      .materialize
      .flatMap(db.put)
      .get
    //assert the key-values were updated
    db.from(10).takeWhile(_._1 <= 90).foreach(_._2 should endWith("_updated")).materialize.get
  }
}