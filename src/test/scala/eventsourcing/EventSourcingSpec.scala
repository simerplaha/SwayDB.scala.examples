package eventsourcing

import java.time.LocalDateTime

import base.TestBase
import eventsourcing.Event._
import swaydb.Bag

class EventSourcingSpec extends TestBase {

  val db = new EventsDB(dir)
  implicit val bag = Bag.apiIO

  "Write Events, iterate Events & build UserState from Events" in {
    //create 10 Users
    (1 to 10) foreach {
      userId =>
        db.writeEvent(
          UserCreated(
            persistentId = userId,
            createTime = LocalDateTime.now(),
            name = randomCharacters()
          )
        )
    }
    //update names of all 10 Users
    (1 to 10) foreach {
      userId =>
        db.writeEvent(
          UserNameUpdated(
            persistentId = userId,
            createTime = LocalDateTime.now(),
            name = "name updated"
          )
        )
    }
    //delete all 10 Users
    (1 to 10) foreach {
      userId =>
        db.writeEvent(
          UserDeleted(
            persistentId = userId,
            createTime = LocalDateTime.now()
          )
        )
    }

    db
      .printAll

    //re-build user aggregates
    (1 to 10) foreach {
      userId =>
        val user = db.buildUserAggregate(userId).get
        user.isDeleted shouldBe true
        user.name shouldBe "name updated"
    }

    db
      .iterateDB
  }

}
