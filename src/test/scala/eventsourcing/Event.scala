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
import swaydb.data.order.KeyOrder
import swaydb.data.slice.Slice
import swaydb.data.util.ByteSizeOf
import swaydb.serializers.Serializer

sealed trait Event {
  val sequenceNumber: Long
  val persistentId: Long
  val createTime: LocalDateTime
  val eventTypeId: Int
}

object Event {
  /**
    * This event is not persistent and is used to read full Event from the database by supplying a partial Event with
    * persistentId and sequenceNumber only. Note: [[eventSortOrder]] uses persistentId & sequenceNumber for keys.
    *
    * A sub type named NonPersistentEvent is probably required to get compilation error when trying to persist this Event.
    * But to keep the example simple staying as is.
    */
  case class ReadOnlyEvent(persistentId: Long, sequenceNumber: Long) extends Event {
    override val createTime: LocalDateTime = LocalDateTime.now()
    override val eventTypeId: Int = 0
  }

  object SequencePointerEvent {
    def eventTypeId = 0
  }

  /**
    * Each persisted aggregate Event gets batched with this Event. It's is a pointer to the aggregate Event
    * to provide iteration in the sequence their were inserted.
    *
    * 0 is the persistentId of this Event. No other aggregate uses this ID.
    *
    * @param targetPersistentId persistent ID of the target event
    * @param sequenceNumber     sequence number
    */
  case class SequencePointerEvent(targetPersistentId: Long, sequenceNumber: Long) extends Event {
    override val persistentId: Long = 0
    override val createTime: LocalDateTime = LocalDateTime.now()
    override val eventTypeId: Int = SequencePointerEvent.eventTypeId
  }

  object StartUserAggregate {
    val eventTypeId: Int = 1
  }
  /**
    * This Event gets batched with every [[UserCreated]] aggregate Event. It's sequence number is 0 so that it gets
    * placed as the first Event on the aggregate's Event's list. This Event is used to find the starting position of the
    * aggregate which is used to build the aggregate.
    *
    * @param persistentId persistentId of the aggregate
    */
  case class StartUserAggregate(persistentId: Long) extends Event {
    override val sequenceNumber: Long = 0
    override val createTime: LocalDateTime = LocalDateTime.now()
    override val eventTypeId: Int = StartUserAggregate.eventTypeId
  }

  object UserCreated {
    val eventTypeId: Int = 2
  }

  case class UserCreated(persistentId: Long,
                         createTime: LocalDateTime,
                         name: String)(val sequenceNumber: Long) extends Event {
    override val eventTypeId: Int = UserCreated.eventTypeId

  }

  object UserNameUpdated {
    val eventTypeId: Int = 3
  }
  case class UserNameUpdated(persistentId: Long,
                             createTime: LocalDateTime,
                             name: String)(val sequenceNumber: Long) extends Event {
    override val eventTypeId: Int = UserNameUpdated.eventTypeId
  }

  object UserDeleted {
    val eventTypeId: Int = 4
  }
  case class UserDeleted(persistentId: Long,
                         createTime: LocalDateTime)(val sequenceNumber: Long) extends Event {
    override val eventTypeId: Int = UserDeleted.eventTypeId
  }

  /**
    * External serializing libraries can be used to easily create serializers but this demo is to show
    * how to use Slice[Byte] to create serializers.
    */
  implicit object EventSerializer extends Serializer[Event] {
    override def write(data: Event): Slice[Byte] = {
      val createTimeBytes = data.createTime.toString.getBytes

      val slice = //create a slice with a default size of 100 bytes and with default properties added.
        Slice.create[Byte](100)
          .addLong(data.persistentId)
          .addLong(data.sequenceNumber)
          .addInt(createTimeBytes.length)
          .addAll(createTimeBytes)
          .addInt(data.eventTypeId)

      data match {
        case SequencePointerEvent(targetPersistentId, _) =>
          slice.addLong(targetPersistentId)

        case UserCreated(_, _, name) =>
          val nameBytes = name.getBytes
          slice.addInt(nameBytes.length)
          slice.addAll(nameBytes)

        case UserNameUpdated(_, _, name) =>
          val nameBytes = name.getBytes
          slice.addInt(nameBytes.length)
          slice.addAll(nameBytes)

        case _ =>
        //no extra bytes required
      }

      //close the slice to ignore any empty bytes.
      slice.close()

    }

    override def read(data: Slice[Byte]): Event = {
      val reader = data.createReaderUnsafe()
      val persistentId = reader.readLong()
      val sequenceNumber = reader.readLong()
      val createTime = LocalDateTime.parse(reader.read(reader.readInt()).readString())
      val eventTypeId = reader.readInt()

      if (eventTypeId == SequencePointerEvent.eventTypeId)
        SequencePointerEvent(
          sequenceNumber = sequenceNumber,
          targetPersistentId = reader.readLong()
        )
      else if (eventTypeId == StartUserAggregate.eventTypeId)
        StartUserAggregate(persistentId)

      else if (eventTypeId == UserCreated.eventTypeId)
        UserCreated(
          persistentId = persistentId,
          createTime = createTime,
          name = reader.read(reader.readInt()).readString()
        )(sequenceNumber = sequenceNumber)
      else if (eventTypeId == UserNameUpdated.eventTypeId)
        UserNameUpdated(
          persistentId = persistentId,
          createTime = createTime,
          name = reader.read(reader.readInt()).readString()
        )(sequenceNumber = sequenceNumber)
      else
        UserDeleted(
          persistentId = persistentId,
          createTime = createTime
        )(sequenceNumber = sequenceNumber)
    }
  }

  /**
    * Partial ordering on event based on persistentId & sequenceNumber.
    * This ordering will keep the aggregate's [[Event]]s together so that it's quicker to build the Events.
    *
    * [[StartUserAggregate]] has a fixed sequence number 0 which will always be the first Event inserted for each aggregate.
    * This event is used to get the starting point to read Events for a specific aggregate.
    */
  implicit val eventSortOrder = new Ordering[Slice[Byte]] {
    def compare(slice1: Slice[Byte], slice2: Slice[Byte]): Int = {
      KeyOrder.default.compare(
        slice1.take(ByteSizeOf.long * 2),
        slice2.take(ByteSizeOf.long * 2)
      )
    }
  }

}


