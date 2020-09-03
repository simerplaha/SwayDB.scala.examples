package crud

import base.TestBase
import swaydb.data.util.PipeOps._
import swaydb.serializers.Default._

class CRUDSpec extends TestBase {

  import swaydb._

  implicit val bag = Bag.less

  def assertCRUD(keyValueCount: Int)(db: swaydb.Map[Int, String, Nothing, Bag.Less]): Unit = {
    //CREATE
    (1 to keyValueCount) foreach {
      key =>
        db.put(key, key.toString)
    }
    //check size
    db.stream.size shouldBe keyValueCount

    //READ FORWARD
    db
      .stream
      .foldLeft(0) {
        case (size, (key, value)) =>
          val expectedKey = size + 1
          key shouldBe expectedKey
          value shouldBe expectedKey.toString
          expectedKey
      } shouldBe keyValueCount

    //READ REVERSE
    db
      .stream
      .reverse
      .foldLeft(keyValueCount) {
        case (size, (key, value)) =>
          key shouldBe size
          value shouldBe size.toString
          size - 1
      } shouldBe 0

    //UPDATE
    db
      .stream
      .map {
        case (key, value) =>
          (key, value + " value updated")
      }
      .materialize
      .==>(db.put)

    //ASSERT UPDATE
    db
      .stream
      .foreach {
        case (key, value) =>
          value should endWith("value updated")
      }

    //DELETE
    db
      .keys
      .stream
      .materialize
      .==>(db.remove)

    db.isEmpty shouldBe true
  }

  //number of key-values
  val keyValueCount = 1000

  "A persistent database" should {

    "perform Create, read (forward & reverse), update & delete (CRUD) on 100,000 key-values" in {
      assertCRUD(keyValueCount) {
        persistent.Map[Int, String, Nothing, Bag.Less](dir.resolve("persistentDB"))
      }
    }
  }

  "A memory database" should {
    "perform Create, read (forward & reverse), update & delete (CRUD) on 100,000 key-values" in {
      assertCRUD(keyValueCount) {
        memory.Map[Int, String, Nothing, Bag.Less]()
      }
    }
  }

  "A memory-persistent database" should {
    "perform Create, read (forward & reverse), update & delete (CRUD) on 100,000 key-values" in {
      assertCRUD(keyValueCount) {
        eventually.persistent.Map[Int, String, Nothing, Bag.Less](dir.resolve("memoryPersistentDB"))
      }
    }
  }
}
