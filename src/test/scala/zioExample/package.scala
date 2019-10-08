import zio.{DefaultRuntime, Runtime, Task}

import scala.concurrent.Await
import scala.concurrent.duration.{FiniteDuration, _}

package object zioExample {

  val runtime = new DefaultRuntime {}

  implicit class TaskAwait[T](task: Task[T]) {

    def awaitTask(implicit scheduler: Runtime[_]): T =
      await()

    def await(timeout: FiniteDuration = 1.second)(implicit runtime: Runtime[_]): T =
      Await.result(runtime.unsafeRunToFuture(task), timeout)
  }

}
