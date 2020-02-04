package partialkey

import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import swaydb.Bag
import swaydb.data.order.KeyOrder
import swaydb.data.slice.Slice

object PartialKeyExample extends App {

  case class Key(id: Int, userName: Option[String])

  implicit object Serialiser extends swaydb.serializers.Serializer[Key] {
    override def write(data: Key): Slice[Byte] =
      Slice.writeString(data.asJson.noSpaces)

    override def read(data: Slice[Byte]): Key =
      decode[Key](data.readString()).getOrElse(throw new Exception("Invalid Key"))
  }

  implicit val keyOrder =
    Right(
      new KeyOrder[Key] {
        override def compare(x: Key, y: Key): Int =
          x.id compare y.id

        override def comparableKey(data: Key): Key =
          data.copy(userName = None)
      }
    )

  val set = swaydb.memory.Set[Key, Nothing, Bag.Less]().get
  //or persistent set
  //val set = swaydb.persistent.Set[Key, Nothing, Bag.Less](Paths.get("target").resolve(this.getClass.getSimpleName)).get

  (1 to 100) foreach {
    i =>
      set.add(Key(i, Some(s"value$i")))
  }

  (1 to 100) foreach {
    i =>
      val key = set.get(Key(i, None)).get
      assert(key == Key(i, Some(s"value$i")))
  }
}
