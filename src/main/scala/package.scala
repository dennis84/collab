package object collab {

  import akka.util.Timeout
  import scala.concurrent.duration._

  implicit val timeout = Timeout(1 seconds)
}
