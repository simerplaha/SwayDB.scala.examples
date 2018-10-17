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

package crud

import base.TestBase
import swaydb.serializers.Default._

class CRUDSpec extends TestBase {

  import swaydb._

  def assertCRUD(keyValueCount: Int)(db: swaydb.Map[Int, String]): Unit = {
    //CREATE
    (1 to keyValueCount) foreach {
      key =>
        db.put(key, key.toString).assertSuccess
    }
    //check size
    db.size shouldBe keyValueCount

    //READ FORWARD
    db.foldLeft(0) {
      case (size, (key, value)) =>
        val expectedKey = size + 1
        key shouldBe expectedKey
        value shouldBe expectedKey.toString
        expectedKey
    } shouldBe keyValueCount

    //READ REVERSE
    db.foldRight(keyValueCount) {
      case ((key, value), size) =>
        key shouldBe size
        value shouldBe size.toString
        size - 1
    } shouldBe 0

    //UPDATE
    db.map {
      case (key, value) =>
        (key, value + " value updated")
    } andThen db.batchPut assertSuccess

    //ASSERT UPDATE
    db.foreach {
      case (key, value) =>
        value should endWith("value updated")
    }

    //DELETE
    db.batchRemove(db.keys).assertSuccess
    db.isEmpty shouldBe true
  }

  //number of key-values
  val keyValueCount = 1000

  "A persistent database" should {

    "perform Create, read (forward & reverse), update & delete (CRUD) on 100,000 key-values" in {
      assertCRUD(keyValueCount) {
        SwayDB.persistent[Int, String](dir.resolve("persistentDB")).assertSuccess
      }
    }
  }

  "A memory database" should {
    "perform Create, read (forward & reverse), update & delete (CRUD) on 100,000 key-values" in {
      assertCRUD(keyValueCount) {
        SwayDB.memory[Int, String]().assertSuccess
      }
    }
  }

  "A memory-persistent database" should {
    "perform Create, read (forward & reverse), update & delete (CRUD) on 100,000 key-values" in {
      assertCRUD(keyValueCount) {
        SwayDB.memoryPersistent[Int, String](dir.resolve("memoryPersistentDB")).assertSuccess
      }
    }
  }
}
