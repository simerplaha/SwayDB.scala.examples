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

package monixExample

import base.TestBase
import monix.eval.Task
import swaydb.serializers.Default._

import scala.util.Random

class MonixSimpleExample extends TestBase {

  "Simple monix example without functions" in {
    implicit val scheduler = monix.execution.Scheduler.global
    import swaydb.monix.Tag._ //provide monix tag to support Task.

    //Create a memory database without functions support (F: Nothing). See MonixExample.scala for an example with function.
    val map = swaydb.memory.Map[Int, String, Nothing, Task]().get

    //create some random key-values
    val keyValues =
      (1 to 100) map {
        key =>
          (key, Random.alphanumeric.take(10).mkString)
      }

    val mapSize: Task[Int] =
      map
        .put(keyValues) //write 100 key-values as single transaction/batch.
        .flatMap {
          _ =>
            //print all key-values
            map.foreach(println).materialize
        }
        .flatMap {
          _ =>
            //result the size of the database.
            map.size
        }

    //above tasks are not executed yet so the database stream will return empty
    map.stream.materialize.awaitTask shouldBe empty
    //execute the task above so it persists the above 100 key-values.
    mapSize.awaitTask shouldBe 100
  }
}
