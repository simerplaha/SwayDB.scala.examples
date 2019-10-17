package base

import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import swaydb.{Apply, PureFunction}
import swaydb.data.slice.Slice
import swaydb.serializers.Serializer

object UserTable {
  sealed trait UserKeys //In SQL this would be primaryKey
  object UserKeys {
    case class UserName(username: String) extends UserKeys
  }

  sealed trait UserValues //in SQL this would be row values
  object UserValues {
    case class ActiveUser(name: String,
                          email: String,
                          lastLogin: Long) extends UserValues

    case object ExpiredUser extends UserValues //in SQL this would be row values
  }

  sealed trait UserFunctions //in SQL there would be update statements.
  object UserFunctions {
    case object ExpireUserFunction extends UserFunctions with PureFunction.OnValue[UserValues, Apply.Map[UserValues]] {
      override def apply(value: UserValues): Apply.Map[UserValues] =
        value match {
          case UserValues.ActiveUser(name, email, lastLogin) =>
            if (lastLogin < System.nanoTime()) //if user has been inactive for a while then expire the user
              Apply.Update(UserValues.ExpiredUser)
            else
              Apply.Nothing //else do nothing
        }
    }
  }

  implicit object UserKeySerializer extends Serializer[UserKeys] { //Serializer for key
    override def write(data: UserKeys): Slice[Byte] =
      Slice.writeString(data.asJson.noSpaces)

    override def read(data: Slice[Byte]): UserKeys =
      decode[UserKeys](data.readString()).right.get
  }

  implicit object UserValuesSerializer extends Serializer[UserValues] { //Serializer for Value
    override def write(data: UserValues): Slice[Byte] =
      Slice.writeString(data.asJson.noSpaces)

    override def read(data: Slice[Byte]): UserValues =
      decode[UserValues](data.readString()).right.get
  }

}
