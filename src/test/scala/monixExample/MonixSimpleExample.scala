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

class MonixSimpleExample extends TestBase {

  "Simple monix example without functions" in {
    implicit val scheduler = monix.execution.Scheduler.global
    import swaydb.monix.Tag._ //provide monix tag to support Task.

    //Create a memory database without functions support (F: Nothing).
    val map = swaydb.memory.Map[Int, String, Nothing, Task]().get

    //write some data
    val mapSize: Task[Int] =
      Task
        .sequence {
          //write 100 key-values
          (1 to 100)
            .map {
              i =>
                map.put(i, i.toString + " value")
            }
        }
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

    //the above tasks are not executed yet so that database will be empty.
    map.stream.materialize.awaitTask shouldBe empty
    //execute the task above and it should persist the above 100 key-values.
    mapSize.awaitTask shouldBe 100
  }
}
