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

package functions

import base.TestBase

class LikesSpec extends TestBase {

  "count likes" in {

    import swaydb._
    import swaydb.serializers.Default._

    val likesMap = memory.Map[String, Int]().get

    (1 to 100) foreach {
      i =>
        likesMap.put(i + "@email.com", 0)
    }

    likesMap.applyFunction(key = "user1", functionID = "increment likes counts")

    val incrementLikes =
      likesMap.registerFunction(
        functionID = "my likes counts",
        function =
          (likes: Int) =>
            Apply.Update(likes + 1)
      )

    //concurrently apply the function
    (1 to 100).par foreach {
      _ =>
        (1 to 100).par foreach {
          i =>
            likesMap.applyFunction(i + "@email.com", incrementLikes).get
        }
    }

    //all functions applied return expected 100 likes in total.
    (1 to 100).par foreach {
      i =>
        likesMap.get(i + "@email.com").get.get shouldBe 100
    }
  }
}