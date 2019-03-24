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

package chunk

import base.TestBase
import swaydb.data.slice.Slice

class ChunkSpec extends TestBase {

  "Store a 3.mb byte array in chunks of 1.mb without creating copies of original array" in {

    import swaydb._
    import swaydb.serializers.Default._

    val db = persistent.Map[Int, Slice[Byte]](dir = dir).get

    val file: Array[Byte] = randomBytes(3.mb) //a 3.mb byte array

    val chunks = Slice(file).groupedSlice(3) //splits of 3 slices of 1.mb each without creating copies of original array

    db.put((1, chunks(0)), (2, chunks(1)), (3, chunks(2))).get //batch write the slices.

    db.size.get shouldBe 3

    val chunk1 = db.get(1).get.get.toArray
    val chuck2 = db.get(2).get.get.toArray
    val chuck3 = db.get(3).get.get.toArray

    chunk1.length shouldBe 1.mb
    chuck2.length shouldBe 1.mb
    chuck3.length shouldBe 1.mb

    chunk1 shouldBe chunks(0).toArray
    chuck2 shouldBe chunks(1).toArray
    chuck3 shouldBe chunks(2).toArray
  }
}