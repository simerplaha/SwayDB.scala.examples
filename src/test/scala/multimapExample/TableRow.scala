package multimapExample

import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import swaydb.data.slice.Slice
import swaydb.serializers.Serializer

sealed trait TableRow

object TableRow {

  case class UserRow(firstName: String, lastName: String) extends TableRow
  case class ProductRow(price: Double) extends TableRow

  /**
   * User PrimaryKey serializer.
   */
  implicit object TableRowSerializer extends Serializer[TableRow] {
    override def write(data: TableRow): Slice[Byte] =
      Slice.writeString(data.asJson.noSpaces)

    override def read(data: Slice[Byte]): TableRow =
      decode[TableRow](data.readString()).right.get
  }
}
