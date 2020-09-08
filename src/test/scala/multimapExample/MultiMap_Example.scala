package multimapExample

import base.TestBase
import multimapExample.PrimaryKey._
import multimapExample.Table._
import multimapExample.TableRow._
import org.scalatest.OptionValues._
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

/**
 * [[swaydb.MultiMap]] is an extension of [[swaydb.Map]] which allows creating nested [[swaydb.MultiMap]] or tree like structure.
 *
 * [[swaydb.MultiMap.schema]] can be used to add nested map.
 */
class MultiMap_Example extends TestBase with Eventually {

  "example" in {
    import swaydb._

    //for simplicity use Bag-less type..
    implicit val bag = Bag.less

    //Initialise in-memory MultiMap which returns the root map.
    val root = memory.MultiMap[Table, PrimaryKey, TableRow, Nothing, Bag.Less]()

    //create two child maps under root.
    val userTable = root.schema.init(mapKey = UserTable, keyType = classOf[UserPrimaryKey], valueType = classOf[UserRow])
    val productTable = root.schema.init(mapKey = ProductTable, keyType = classOf[ProductPrimaryKey], valueType = classOf[ProductRow])

    //create another child map under the UserTable map which expires after 10.seconds.
    val childUserTable = userTable.schema.init(mapKey = UserTable, keyType = classOf[UserPrimaryKey], valueType = classOf[UserRow], expireAfter = 10.seconds)

    //user table entries
    userTable.put(UserPrimaryKey(email = "simer@mail.com"), UserRow(firstName = "Simer", lastName = "Plaha"))
    userTable.put(UserPrimaryKey(email = "val@mail.com"), UserRow(firstName = "Valentyn", lastName = "Kolesnikov"))

    //product table entries
    productTable.put(ProductPrimaryKey(id = 1), ProductRow(price = 10.0))
    productTable.put(ProductPrimaryKey(id = 2), ProductRow(price = 20.0))

    //child table entries
    childUserTable.put(UserPrimaryKey(email = "child1@mail.com"), UserRow(firstName = "Child", lastName = "One"))
    childUserTable.put(UserPrimaryKey(email = "child2@mail.com"), UserRow(firstName = "Child", lastName = "Two"))

    //read schema of parent UserTable.
    userTable.schema.keys.materialize.toList should contain only UserTable

    //stream entries from both tables
    userTable.stream.materialize.toList should contain only((UserPrimaryKey("simer@mail.com"), UserRow("Simer", "Plaha")), (UserPrimaryKey("val@mail.com"), UserRow("Valentyn", "Kolesnikov")))
    productTable.stream.materialize.toList should contain only((ProductPrimaryKey(1), ProductRow(10.0)), (ProductPrimaryKey(2), ProductRow(20.0)))
    childUserTable.stream.materialize.toList should contain only((UserPrimaryKey("child1@mail.com"), UserRow("Child", "One")), (UserPrimaryKey("child2@mail.com"), UserRow("Child", "Two")))

    //replace parent UserTable with a new table. This clear all previous entries and all it's child maps.
    root.schema.replace(UserTable, classOf[UserPrimaryKey], classOf[UserRow])

    //clear all existing entries.
    userTable.stream.materialize.toList shouldBe empty
    userTable.schema.stream.materialize.toList shouldBe empty

    //create a transaction that runs on all tables.
    val transaction =
      userTable.toTransaction(Prepare.Remove(UserPrimaryKey("simer@mail.com"))) ++
        childUserTable.toTransaction(Prepare.Remove(UserPrimaryKey("child2@mail.com"))) ++
        productTable.toTransaction(Prepare.Expire(ProductPrimaryKey(1), 2.seconds))

    //commit the transactions on the root map.
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
