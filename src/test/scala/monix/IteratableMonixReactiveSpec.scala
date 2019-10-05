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

package monix

import base.TestBase

class IteratableMonixReactiveSpec extends TestBase {


  import scala.concurrent.duration._
  "Dasdas" in {

    System.currentTimeMillis()

    val deadline: Deadline = 10.seconds.fromNow

    import java.time.Instant
    import java.time.ZoneId
    val instant = Instant.ofEpochMilli(deadline.time.toMillis)
    instant.atZone(ZoneId.systemDefault).toLocalDateTime
  }

  "Monix reactive example" in {

    import swaydb._
    import swaydb.serializers.Default._ //import default serializers

    //Create a memory database.
    val db = memory.Map[Int, String, Nothing]().get

    //write some data
    (1 to 100) foreach {
      i =>
        db.put(i, i.toString + " value").get
    }

    import monix.execution.Scheduler.Implicits.global
    import monix.reactive._

    val everyTenthValue =
      Observable.fromIterable(db.asScala)
//        .drop(1)
        .map(_._1)
        .take(10)
        .dump("Value")

    everyTenthValue.subscribe()
  }
}