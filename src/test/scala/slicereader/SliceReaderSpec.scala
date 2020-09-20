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
        Slice.of[Byte](100) //create an unknown size of Slice
          .addInt(program.speedScore)
          .addLong(program.linesOfCode)
          .addString(program.language)
          .close() //close so that Slice's toOffset is re-adjusted to the last written byte.

      //read Slice[Byte] to rebuild the case class
      val reader = slice.createReader()
      ProgramPerformance(
        speedScore = reader.readInt(),
        linesOfCode = reader.readLong(),
        language = reader.readRemainingAsString()
      ) shouldBe program
    }
  }
}
