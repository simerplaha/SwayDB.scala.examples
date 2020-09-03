package quickstart

import base.TestBase

class QuickStartSetSpec extends TestBase {

  "Quick start with a set database" in {

    import swaydb._
    import swaydb.serializers.Default._ //import default serializers

    implicit val bag = Bag.apiIO

    //Create a persistent set database. If the directories do not exist, they will be created.
    val db = persistent.Set[Int, Nothing, IO.ApiIO](dir = dir.resolve("disk1")).get

    db.add(1).get
    db.contains(1).get shouldBe true
    db.remove(1).get
    db.commit(
      Prepare.Add(1),
      Prepare.Remove(1),
      Prepare.Remove(from = 1, to = 100)
    ).get

    db.add(1, 2)

    //write 100 key-values
    (1 to 100) foreach { i => db.add(i).get }
    //Iteration: remove all items withing range 1 to 50 and batch add 50 new items ranging from 101 to 150
    db
      .stream
      .from(1)
      .takeWhile(_ < 50)
      .materialize
      .flatMap(db.remove)
      .flatMap(_ => db.add(101 to 150))
    //assert the key-values were updated
    db
      .stream
      .materialize
      .get shouldBe (50 to 150)
  }
}