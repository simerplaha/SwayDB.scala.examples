package base

import boopickle.Default._
import swaydb.Apply
import swaydb.PureFunctionScala.OnValue

/**
 * Test domains objects. Mimics how it similar data would be structured
 * in a SQL table.
 *
 * Supports
 *
 * Primary key                      | Columns - All columns stored as a case class.
 * ------------------------------------------------------------------
 * [[base.UserTable.UserKey]]      | [[base.UserTable.UserValue]]
 * ------------------------------------------------------------------
 */
object UserTable {
  //Give key's a type. In SQL this would be primaryKey.
  sealed trait UserKey
  object UserKey {
    //Uniqueness is on primary key.
    case class UserName(username: String) extends UserKey
  }

  //type for values. In SQL this would be row values
  sealed trait UserValue
  object UserValue {
    //two value types.
    //ActiveUser and ExpiredUser.
    case class ActiveUser(name: String,
                          email: String,
                          lastLogin: Long) extends UserValue

    case object ExpiredUser extends UserValue
  }

  //Lets define our functions. In SQL there would be update statements.
  val expireUserFunction: OnValue[UserValue] = {
    case UserValue.ActiveUser(_, _, lastLogin) =>
      if (lastLogin < System.nanoTime()) //if user has been inactive for a while then expire the user
        Apply.Update(UserValue.ExpiredUser)
      else
        Apply.Nothing //else do nothing
  }

  implicit val userKeySerializer = swaydb.serializers.BooPickle[UserKey]
  implicit val userValuesSerializer = swaydb.serializers.BooPickle[UserValue]
}
