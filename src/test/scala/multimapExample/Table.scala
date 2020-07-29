package multimapExample

import swaydb.data.slice.Slice
import swaydb.serializers.Serializer
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._

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
  implicit object TableSerializer extends Serializer[Table] {
    override def write(data: Table): Slice[Byte] =
      Slice.writeString(data.asJson.noSpaces)

    override def read(data: Slice[Byte]): Table =
      decode[Table](data.readString()).right.get
  }
}
