package multimapExample

import boopickle.Default._

sealed trait Table
object Table {
  /**
   * User Table
   */
  sealed trait UserTable extends Table
  case object UserTable extends UserTable

  /**
   * Product Table
   */
  sealed trait ProductTable extends Table
  case object ProductTable extends ProductTable

  /**
   * User Table serializer.
   */

  implicit val tableSerializer = swaydb.serializers.BooPickle[Table]
}
