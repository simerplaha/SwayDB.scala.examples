package multimapExample

import base.TestBase
import multimapExample.PrimaryKey._
import multimapExample.Table._
import multimapExample.TableRow._
import org.scalatest.OptionValues._
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout

import scala.concurrent.duration._

class MultiMap_Example extends TestBase with Eventually {

  "example" in {
    import swaydb._ //import default serializers

    implicit val bag = Bag.less

    //Create a root map
    val root = memory.MultiMap[Table, PrimaryKey, TableRow, Nothing, Bag.Less]()

    val userTable = root.schema.init(UserTable, classOf[UserPrimaryKey], classOf[UserRow])
    val productTable = root.schema.init(ProductTable, classOf[ProductPrimaryKey], classOf[ProductRow])

    //add child table to UserTable which expires after 10.seconds. Child table should be the same type or sub type of parent table.
    val childUserTable = userTable.schema.init(UserTable, classOf[UserPrimaryKey], classOf[UserRow], 10.seconds)

    //user table entries
    userTable.put(UserPrimaryKey("simer@mail.com"), UserRow("Simer", "Plaha"))
    userTable.put(UserPrimaryKey("val@mail.com"), UserRow("Valentyn", "Kolesnikov"))

    //product table entries
    productTable.put(ProductPrimaryKey(1), ProductRow(10.0))
    productTable.put(ProductPrimaryKey(2), ProductRow(20.0))

    //child table entries
    childUserTable.put(UserPrimaryKey("child1@mail.com"), UserRow("Child", "One"))
    childUserTable.put(UserPrimaryKey("child2@mail.com"), UserRow("Child", "Two"))

    //read schema of parent UserTable.
    userTable.schema.keys.materialize.toList should contain only UserTable

    //stream entries from both tables
    userTable.stream.materialize shouldBe List((UserPrimaryKey("simer@mail.com"), UserRow("Simer", "Plaha")), (UserPrimaryKey("val@mail.com"), UserRow("Valentyn", "Kolesnikov")))
    productTable.stream.materialize shouldBe List((ProductPrimaryKey(1), ProductRow(10.0)), (ProductPrimaryKey(2), ProductRow(20.0)))
    childUserTable.stream.materialize shouldBe List((UserPrimaryKey("child1@mail.com"), UserRow("Child", "One")), (UserPrimaryKey("child2@mail.com"), UserRow("Child", "Two")))

    //replace parent UserTable with a new table. This clear all previous entries.
    root.schema.replace(UserTable, classOf[UserPrimaryKey], classOf[UserRow])

    //clear all existing entries.
    userTable.stream.materialize.toList shouldBe empty
    userTable.schema.stream.materialize.toList shouldBe empty

    //transaction remove and expire
    val transaction =
      userTable.toTransaction(Prepare.Remove(UserPrimaryKey("simer@mail.com"))) ++
        childUserTable.toTransaction(Prepare.Remove(UserPrimaryKey("child2@mail.com"))) ++
        productTable.toTransaction(Prepare.Expire(ProductPrimaryKey(1), 2.seconds))

    root.commit(transaction)

    userTable.get(UserPrimaryKey("simer@mail.com")) shouldBe empty
    childUserTable.get(UserPrimaryKey("child2@mail.com")) shouldBe empty
    productTable.get(ProductPrimaryKey(1)).value shouldBe ProductRow(10.0)
    productTable.get(ProductPrimaryKey(2)).value shouldBe ProductRow(20.0)

    //product1 is eventually expired
    super.eventually(Timeout(2.seconds)) {
      productTable.get(ProductPrimaryKey(1)) shouldBe empty
    }
    //product2 remains unchanged.
    productTable.get(ProductPrimaryKey(2)).value shouldBe ProductRow(20.0)
  }
}
