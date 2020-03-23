package chunk

import base.TestBase
import swaydb.data.slice.Slice

class ChunkSpec extends TestBase {

  "Store a 3.mb byte array in chunks of 1.mb without creating copies of original array" in {

    import swaydb._
    import swaydb.serializers.Default._

    implicit val bag = Bag.less

    val db = persistent.Map[Int, Slice[Byte], Nothing, Bag.Less](dir = dir).get

    val file: Array[Byte] = randomBytes(3.mb) //a 3.mb byte array

    val chunks = Slice(file).groupedSlice(3) //splits of 3 slices of 1.mb each without creating copies of original array

    db.put((1, chunks(0)), (2, chunks(1)), (3, chunks(2))) //batch write the slices.

    db.stream.size shouldBe 3

    val chunk1 = db.get(1).get.toArray
    val chuck2 = db.get(2).get.toArray
    val chuck3 = db.get(3).get.toArray

    chunk1.length shouldBe 1.mb
    chuck2.length shouldBe 1.mb
    chuck3.length shouldBe 1.mb

    chunk1 shouldBe chunks(0).toArray
    chuck2 shouldBe chunks(1).toArray
    chuck3 shouldBe chunks(2).toArray
  }
}