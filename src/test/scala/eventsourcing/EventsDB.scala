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

import java.nio.file.Path
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

import eventsourcing.Event._

case class UserState(userId: Long,
                     name: String,
                     isDeleted: Boolean,
                     createTime: LocalDateTime)

class EventsDB(dir: Path) {

  import swaydb._

  implicit val bag = Bag.less
  implicit def unwrap[A](less: Bag.Less[A]): A = less

  private val db = persistent.Set[Event, Nothing, Bag.Less](dir).get

  private val seqNumberGenerator = new AtomicLong(1)

  /**
    * Persists partial Event that inputs the next event sequence number and returns an [[Event]].
    */
  def writeEvent(event: Long => Event): OK = {
    //initialise the Event with the next Sequence number.
    event(seqNumberGenerator.getAndIncrement()) match {
      case event @ UserCreated(persistentId, _, _) =>
        //for every UserCreated event, SequencePointerEvent and StartUserAggregate gets batched.
        //SequencePointerEvent are used to iterate the Events in the order they were inserted. Eg: global iterators
        //StartUserAggregate indicates the starting Event for this aggregate which can be used to build aggregates.
        db.add(SequencePointerEvent(event.persistentId, event.sequenceNumber), StartUserAggregate(persistentId), event)

      case event =>
        //for all Events SequencePointerEvent also gets persistent. SequencePointerEvent is a pointer event which can be
        //used to iterate events in the actual order they were inserted.
        db.add(SequencePointerEvent(event.persistentId, event.sequenceNumber), event)
    }
  }

  /**
    * Give a userId (persistentId) builds the UserState or UserAggregate.
    */
  def buildUserAggregate(userId: Int): Option[UserState] =
    db
      //StartUserAggregate is only a pointer Event to the first Event of the aggregate. Use 'after' to get the first actual Event of the aggregate.
      .after(StartUserAggregate(userId))
      .stream
      .takeWhile(_.persistentId == userId) //fetch Events for only this User
      .foldLeft(Option.empty[UserState]) { //and then build User's state.
        case (userState, event) =>
          event match {
            case UserCreated(persistentId, createTime, name) =>
              Some(UserState(userId = persistentId, name = name, isDeleted = false, createTime = createTime))

            case UserNameUpdated(_, _, name) =>
              userState.map(_.copy(name = name))

            case UserDeleted(_, _) =>
              userState.map(_.copy(isDeleted = true))
          }
      }

  //print all Event. This gives a actual view of how the Events are organised in the database
  def printAll =
    db
      .stream
      .foreach(println)

  /**
    * Full database iteration.
    *
    * Print all events in the sequence they were inserted by using [[SequencePointerEvent]]
    */
  def iterateDB =
    db
      .stream
      .takeWhile(_.eventTypeId == SequencePointerEvent.eventTypeId)
      .foreach {
        case sequenceEvent: SequencePointerEvent =>
          db.get(ReadOnlyEvent(sequenceEvent.targetPersistentId, sequenceEvent.sequenceNumber)) foreach println
      }
}
