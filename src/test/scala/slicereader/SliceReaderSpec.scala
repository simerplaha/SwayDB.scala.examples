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

package slicereader

import org.scalatest.{Matchers, WordSpec}

class SliceReaderSpec extends WordSpec with Matchers {

  "A SliceReader" should {
    "read all written bytes" in {

      import swaydb.data.slice.Slice

      //stores performance results of a program written in programming languages
      case class ProgramPerformance(speedScore: Int,
                                    linesOfCode: Long,
                                    language: String)

      val program = ProgramPerformance(10, 10L, "Scala")

      //write case class to Slice[Byte] as a sequence of bytes.
      val slice =
        Slice.create[Byte](100) //create an unknown size of Slice
          .addInt(program.speedScore)
          .addLong(program.linesOfCode)
          .addString(program.language)
          .close() //close so that Slice's toOffset is re-adjusted to the last written byte.

      //read Slice[Byte] to rebuild the case class
      val reader = slice.createReader()
      ProgramPerformance(
        speedScore = reader.readInt(),
        linesOfCode = reader.readLong(),
        language = reader.readString()
      ) shouldBe program
    }
  }
}