package lidraughts.analyse

import scala.concurrent.duration.FiniteDuration

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json.JsValue

import lidraughts.socket._

private final class AnalyseSocket(
    timeout: FiniteDuration
) extends SocketActor[AnalyseSocket.Member](timeout) {

  import AnalyseSocket._

  def receiveSpecific = {

    case Join(uid, userId) => {
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, userId)
      addMember(uid, member)
      sender ! Connected(enumerator, member)
    }
  }
}

private object AnalyseSocket {

  case class Member(
      channel: JsChannel,
      userId: Option[lidraughts.user.User.ID]
  ) extends SocketMember {

    val troll = false
  }

  case class Join(uid: Socket.Uid, userId: Option[lidraughts.user.User.ID])
  case class Connected(enumerator: JsEnumerator, member: Member)
}
