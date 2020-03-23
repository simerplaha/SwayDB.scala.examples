package monixExample

import base.TestBase
import base.UserTable.UserValues.ExpiredUser
import base.UserTable.{UserFunctions, UserKeys, UserValues}
import monix.eval.Task
import org.scalatest.OptionValues._

class MonixExample extends TestBase {

  "Monix example with functions" in {
    implicit val scheduler = monix.execution.Scheduler.global
    import swaydb.monix.Bag._ //import monix tag to support Task.

    //functions should always be registered before database startup.
    implicit val functions = swaydb.Map.Functions[UserKeys, UserValues, UserFunctions]()
    functions.register(UserFunctions.ExpireUserFunction)

    val map = swaydb.memory.Map[UserKeys, UserValues, UserFunctions, Task]().get //Create a memory database

    val userName = UserKeys.UserName("iron_man")
    val activeUser = UserValues.ActiveUser(name = "Tony Stark", email = "tony@stark.com", lastLogin = System.nanoTime())

    //write the above user as active
    map.put(key = userName, value = activeUser).awaitTask

    //get the user from the database it will result active.
    map.get(userName).awaitTask.value shouldBe activeUser

    //expire user using the registered ExpireFunction
    map.applyFunction(key = userName, function = UserFunctions.ExpireUserFunction).awaitTask

    //the function expires the user "iron_man" - blame Thanos!
    map.get(userName).awaitTask.value shouldBe ExpiredUser
  }
}
