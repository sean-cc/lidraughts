package lidraughts.challenge

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.concurrent.duration.Duration

import lidraughts.hub.TimeBomb
import lidraughts.socket.actorApi.{ Connected => _, _ }
import lidraughts.socket.Socket.{ Uid, SocketVersion }
import lidraughts.socket.{ SocketActor, History, Historical }

private final class Socket(
    challengeId: String,
    val history: History[Unit],
    getChallenge: Challenge.ID => Fu[Option[Challenge]],
    uidTimeout: Duration,
    socketTimeout: Duration
) extends SocketActor[Socket.Member](uidTimeout) with Historical[Socket.Member, Unit] {

  private val timeBomb = new TimeBomb(socketTimeout)

  def receiveSpecific = {

    case Socket.Reload =>
      getChallenge(challengeId) foreach {
        _ foreach { challenge =>
          notifyVersion("reload", JsNull, ())
        }
      }

    case Ping(uid, vOpt, lagCentis) => {
      ping(uid, lagCentis)
      timeBomb.delay

      // Mobile backwards compat
      vOpt foreach { v =>
        withMember(uid) { m =>
          history.since(v).fold(resync(m))(_ foreach sendMessage(m))
        }
      }
    }

    case Broom => {
      broom
      if (timeBomb.boom) self ! PoisonPill
    }

    case GetVersion => sender ! history.version

    case Socket.Join(uid, userId, owner, version) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Socket.Member(channel, userId, owner)
      addMember(uid, member)

      val msgs: List[JsValue] = version
        .fold(history.getRecent(5).some)(history.since)
        .fold(List(resyncMessage))(_ map filteredMessage(member))

      sender ! Socket.Connected(
        lidraughts.common.Iteratee.prepend(msgs, enumerator),
        member
      )

    case Quit(uid) => quit(uid)
  }

  protected def shouldSkipMessageFor(message: Message, member: Socket.Member) = false
}

private object Socket {

  case class Member(
      channel: JsChannel,
      userId: Option[String],
      owner: Boolean
  ) extends lidraughts.socket.SocketMember {
    val troll = false
  }

  case class Join(uid: Uid, userId: Option[String], owner: Boolean, version: Option[SocketVersion])
  case class Connected(enumerator: JsEnumerator, member: Member)

  case object Reload
}
