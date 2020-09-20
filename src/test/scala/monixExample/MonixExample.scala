package monixExample

import base.{TestBase, UserTable}
import base.UserTable.UserValue.ExpiredUser
import base.UserTable.{UserKey, UserValue}
import monix.eval.Task
import org.scalatest.OptionValues._
import swaydb.PureFunction
import swaydb.data.Functions

class MonixExample extends TestBase {

  "Monix example with functions" in {
    implicit val scheduler = monix.execution.Scheduler.global
    import swaydb.monix.Bag._ //import monix tag to support Task.

    //functions should always be registered before database startup.
    implicit val functions = Functions[PureFunction.Map[UserKey, UserValue]](UserTable.expireUserFunction)

    val map = swaydb.memory.Map[UserKey, UserValue, PureFunction.Map[UserKey, UserValue], Task]().awaitTask //Create a memory database

    val userName = UserKey.UserName("iron_man")
    val activeUser = UserValue.ActiveUser(name = "Tony Stark", email = "tony@stark.com", lastLogin = System.nanoTime())

    //write the above user as active
    map.put(key = userName, value = activeUser).awaitTask

    //get the user from the database it will result active.
    map.get(userName).awaitTask.value shouldBe activeUser

    //expire user using the registered ExpireFunction
    map.applyFunction(key = userName, function = UserTable.expireUserFunction).awaitTask

    //the function expires the user "iron_man" - blame Thanos!
    map.get(userName).awaitTask.value shouldBe ExpiredUser
  }
}
