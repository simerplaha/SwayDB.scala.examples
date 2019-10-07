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

package eventsourcing

import java.time.LocalDateTime

import base.TestBase
import eventsourcing.Event._

class EventSourcingSpec extends TestBase {

  val db = new EventsDB(dir)

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
        ).get
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
        ).get
    }
    //delete all 10 Users
    (1 to 10) foreach {
      userId =>
        db.writeEvent(
          UserDeleted(
            persistentId = userId,
            createTime = LocalDateTime.now()
          )
        ).get
    }

    db
      .printAll
      .materialize
      .get

    //re-build user aggregates
    (1 to 10) foreach {
      userId =>
        val user = db.buildUserAggregate(userId).get
        user.isDeleted shouldBe true
        user.name shouldBe "name updated"
    }

    db
      .iterateDB
      .materialize
      .get
  }

}
