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

class QuickStartSetSpec extends TestBase {

  "Quick start with a set database" in {

    import swaydb._
    import swaydb.serializers.Default._ //import default serializers

    //Create a persistent set database. If the directories do not exist, they will be created.
    val db = SwayDB.persistentSet[Int](dir = dir.resolve("disk1")).assertSuccess

    db.add(1).assertSuccess
    db.contains(1).assertSuccess shouldBe true
    db.remove(1).assertSuccess
    db.batch(Batch.Add(1), Batch.Remove(1)).assertSuccess

    //write 100 key-values
    (1 to 100) foreach { i => db.add(i).assertSuccess }
    //Iteration: remove all items withing range 1 to 50 and batch add 50 new items ranging from 101 to 150
    db
      .from(1)
      .until(_ < 50)
      .foreach(db.remove)
      .andThen {
        _ =>
          db.batchAdd(101 to 150).assertSuccess
      }
    //assert the key-values were updated
    db.toList shouldBe (50 to 150)
  }
}