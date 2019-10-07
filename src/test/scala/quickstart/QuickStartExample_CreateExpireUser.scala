package quickstart

import swaydb.data.slice.Slice
import swaydb.serializers.Serializer

object QuickStartExample_CreateExpireUser extends App {

  import io.circe.generic.auto._
  import io.circe.parser._
  import io.circe.syntax._
  import swaydb._

  sealed trait Key //In SQL this would be primaryKey
  object Key {
    case class UserName(username: String) extends Key
  }

  sealed trait Value //in SQL this would be row values
  object Value {
    case class ActiveUser(name: String,
                          email: String,
                          lastLogin: Long) extends Value

    case object ExpiredUser extends Value //in SQL this would be row values
  }

  sealed trait Function //in SQL there would be update statements.
  object Function {
    case object ExpireUser extends Function with swaydb.Function.GetValue[Value] {

      override def apply(value: Value): Apply.Map[Value] =
        value match {
          case Value.ActiveUser(name, email, lastLogin) =>
            if (lastLogin < System.nanoTime()) //if user has been inactive for a while then expire the user
              Apply.Update(Value.ExpiredUser)
            else
              Apply.Nothing //else do nothing
        }

      override val id: Slice[Byte] =
        Slice.writeInt(1)
    }
  }

  implicit object KeySerializer extends Serializer[Key] { //Serializer for key
    override def write(data: Key): Slice[Byte] =
      Slice.writeString(data.asJson.noSpaces)

    override def read(data: Slice[Byte]): Key =
      decode[Key](data.readString()).right.get
  }

  implicit object ValueSerializer extends Serializer[Value] { //Serializer for Value
    override def write(data: Value): Slice[Byte] =
      Slice.writeString(data.asJson.noSpaces)

    override def read(data: Slice[Byte]): Value =
      decode[Value](data.readString()).right.get
  }

  val map = memory.Map[Key, Value, Function]().get //Create a memory database

  //functions should always be registered on database startup.
  map.registerFunction(Function.ExpireUser)

  map.put(
    key = Key.UserName("iron_man"),
    value = Value.ActiveUser(name = "Tony Stark", email = "tony@stark.com", lastLogin = System.nanoTime())
  ).get

  //get the user
  println("User status: " + map.get(Key.UserName("iron_man")).get) //User status: Some(ActiveUser(Tony Stark,tony@stark.com,705876613043474))

  //expire user using the registered ExpireFunction
  map.applyFunction(Key.UserName("iron_man"), Function.ExpireUser).get

  println("User status: " + map.get(Key.UserName("iron_man")).get) //User status: Some(ExpiredUser)
}
