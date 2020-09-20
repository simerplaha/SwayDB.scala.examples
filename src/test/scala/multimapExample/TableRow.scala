package multimapExample

import boopickle.Default._

sealed trait TableRow

object TableRow {

  case class UserRow(firstName: String, lastName: String) extends TableRow
  case class ProductRow(price: Double) extends TableRow

  /**
   * User PrimaryKey serializer.
   */
  implicit val tableRowSerializer = swaydb.serializers.BooPickle[TableRow]
}
