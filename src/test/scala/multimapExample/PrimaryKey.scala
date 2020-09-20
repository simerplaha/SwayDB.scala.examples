package multimapExample

import boopickle.Default._

sealed trait PrimaryKey
object PrimaryKey {
  /**
   * User PrimaryKey
   */
  case class UserPrimaryKey(email: String) extends PrimaryKey

  /**
   * Product PrimaryKey
   */
  case class ProductPrimaryKey(id: Int) extends PrimaryKey

  /**
   * User PrimaryKey serializer.
   */
  implicit val primaryKeySerializer = swaydb.serializers.BooPickle[PrimaryKey]
}
