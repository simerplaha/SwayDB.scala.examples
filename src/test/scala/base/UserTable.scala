package base

import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import swaydb.{Apply, PureFunction}
import swaydb.data.slice.Slice
import swaydb.serializers.Serializer

/**
  * Test domains objects. Mimics how it similar data would be structured
  * in a SQL table.
  *
  * Supports
  *
  * Primary key                      | Columns - All columns stored as a case class.
  * ------------------------------------------------------------------
  * [[base.UserTable.UserKeys]]      | [[base.UserTable.UserValues]]
  * ------------------------------------------------------------------
  */
object UserTable {
  //Give key's a type. In SQL this would be primaryKey.
  sealed trait UserKeys
  object UserKeys {
    //Uniqueness is on primary key.
    case class UserName(username: String) extends UserKeys
  }

  //type for values. In SQL this would be row values
  sealed trait UserValues
  object UserValues {
    //two value types.
    //ActiveUser and ExpiredUser.
    case class ActiveUser(name: String,
                          email: String,
                          lastLogin: Long) extends UserValues

    case object ExpiredUser extends UserValues
  }

  //Lets define our functions. In SQL there would be update statements.
  sealed trait UserFunctions
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

  //Serializer for key. Any external serialisation library can be used here.
  //For this demo we use Circe.
  implicit object UserKeySerializer extends Serializer[UserKeys] {
    override def write(data: UserKeys): Slice[Byte] =
      Slice.writeString(data.asJson.noSpaces)

    override def read(data: Slice[Byte]): UserKeys =
      decode[UserKeys](data.readString()).right.get
  }

  //Serializer for values. Any external serialisation library can be used here.
  //For this demo we use Circe.
  implicit object UserValuesSerializer extends Serializer[UserValues] {
    override def write(data: UserValues): Slice[Byte] =
      Slice.writeString(data.asJson.noSpaces)

    override def read(data: Slice[Byte]): UserValues =
      decode[UserValues](data.readString()).right.get
  }

}
