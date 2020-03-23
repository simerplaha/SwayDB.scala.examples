package quickstart

import base.TestBase

class QuickStartMemorySpec extends TestBase {

  "Quick start" in {

    import swaydb._
    import swaydb.serializers.Default._ //import default serializers

    implicit val bag = Bag.apiIO

    //Create a memory database
    val db = memory.Map[Int, String, Nothing, IO.ApiIO]().get

    db.put(1, "one").get
    db.get(1).get should contain("one")
    db.remove(1).get
    db.commit(
      Prepare.Put(key = 1, value = "one value"),
      Prepare.Update(from = 1, to = 100, value = "range update"),
      Prepare.Remove(key = 1),
      Prepare.Remove(from = 1, to = 100)
    ).get

    //write 100 key-values
    (1 to 100) foreach { i => db.put(key = i, value = i.toString).get }

    //Iteration: fetch all key-values withing range 10 to 90, update values and batch write updated key-values
    db
      .from(10)
      .stream
      .takeWhile(_._1 <= 90)
      .map {
        case (key, value) =>
          (key, value + "_updated")
      }
      .materialize
      .flatMap(db.put)
      .get

    //assert the key-values were updated
    db
      .from(10)
      .stream
      .takeWhile(_._1 <= 90)
      .foreach(_._2 should endWith("_updated")).get
  }
}