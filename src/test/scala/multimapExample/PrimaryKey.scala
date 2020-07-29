package multimapExample

import swaydb.data.slice.Slice
import swaydb.serializers.Serializer
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._

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
  implicit object PrimaryKeySerializer extends Serializer[PrimaryKey] {
    override def write(data: PrimaryKey): Slice[Byte] =
      Slice.writeString(data.asJson.noSpaces)

    override def read(data: Slice[Byte]): PrimaryKey =
      decode[PrimaryKey](data.readString()).right.get
  }
}
