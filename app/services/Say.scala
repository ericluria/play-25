package services

import javax.inject._
import play.api.inject.ApplicationLifecycle
import scala.concurrent.Future

trait Say {
  def hello(): Unit
  def goodbye(): Unit
}

@Singleton
class SayImpl @Inject() (appLifecycle: ApplicationLifecycle) extends Say {  
    override def hello(): Unit = println("Hello!")
    override def goodbye(): Unit = println("Goodbye!")

    def start(): Unit = hello()

    // When the application starts, register a stop hook with the
    // ApplicationLifecycle object. The code inside the stop hook wil
    // be run when the application stops.
    appLifecycle.addStopHook { () =>
        goodbye()
        Future.successful(())
    }

    // Called when the singleton is constructed
    start()
}
