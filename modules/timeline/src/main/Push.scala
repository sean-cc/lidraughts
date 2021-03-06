package lidraughts.timeline

import akka.actor._
import org.joda.time.DateTime

import lidraughts.hub.actorApi.lobby.NewForumPost
import lidraughts.hub.actorApi.timeline.propagation._
import lidraughts.hub.actorApi.timeline.{ Propagate, Atom, ForumPost, ReloadTimelines }
import lidraughts.security.{ Granter, Permission }
import lidraughts.user.UserRepo

private[timeline] final class Push(
    bus: lidraughts.common.Bus,
    renderer: ActorSelection,
    getFriendIds: String => Fu[Set[String]],
    getFollowerIds: String => Fu[Set[String]],
    entryApi: EntryApi,
    unsubApi: UnsubApi
) extends Actor {

  def receive = {

    case Propagate(data, propagations) =>
      data match {
        case _: ForumPost => bus.publish(NewForumPost, 'lobbySocket)
        case _ =>
      }
      propagate(propagations) flatMap { users =>
        unsubApi.filterUnsub(data.channel, users)
      } foreach { users =>
        if (users.nonEmpty) makeEntry(users, data) >>-
          bus.publish(ReloadTimelines(users), 'lobbySocket)
        lidraughts.mon.timeline.notification(users.size)
      }
  }

  private def propagate(propagations: List[Propagation]): Fu[List[String]] =
    propagations.map {
      case Users(ids) => fuccess(ids)
      case Followers(id) => getFollowerIds(id)
      case Friends(id) => getFriendIds(id)
      case StaffFriends(id) => getFriendIds(id) flatMap UserRepo.byIdsSecondary map {
        _ filter Granter(_.StaffForum) map (_.id)
      }
      case ExceptUser(_) => fuccess(Nil)
      case ModsOnly(_) => fuccess(Nil)
    }.sequence flatMap { users =>
      propagations.foldLeft(fuccess(users.flatten.distinct)) {
        case (fus, ExceptUser(id)) => fus.map(_.filter(id!=))
        case (fus, ModsOnly(true)) => for {
          us <- fus
          userIds <- UserRepo.userIdsWithRoles(modPermissions.map(_.name))
        } yield us filter userIds.contains
        case (fus, _) => fus
      }
    }

  private def modPermissions = List(
    Permission.ModNote,
    Permission.Hunter,
    Permission.Admin,
    Permission.SuperAdmin
  )

  private def makeEntry(users: List[String], data: Atom): Fu[Entry] = {
    val entry = Entry.make(data)
    entryApi.findRecent(entry.typ, DateTime.now minusMinutes 60, 1000) flatMap { entries =>
      if (entries.exists(_ similarTo entry)) fufail[Entry]("[timeline] a similar entry already exists")
      else entryApi insert Entry.ForUsers(entry, users) inject entry
    }
  }
}
