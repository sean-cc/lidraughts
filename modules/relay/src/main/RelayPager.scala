package lidraughts.relay

import lidraughts.common.paginator.Paginator
import lidraughts.db.dsl._
import lidraughts.db.paginator.{ Adapter, CachedAdapter }
import lidraughts.user.User

final class RelayPager(
    repo: RelayRepo,
    withStudy: RelayWithStudy,
    maxPerPage: lidraughts.common.MaxPerPage
) {

  import BSONHandlers._

  def finished(me: Option[User], page: Int) = paginator(
    repo.selectors finished true, me, page, fuccess(9999).some
  )

  private def paginator(
    selector: Bdoc,
    me: Option[User],
    page: Int,
    nbResults: Option[Fu[Int]] = none
  ): Fu[Paginator[Relay.WithStudyAndLiked]] = {
    val adapter = new Adapter[Relay](
      collection = repo.coll,
      selector = selector,
      projection = $empty,
      sort = $sort desc "startedAt"
    ) mapFutureList withStudy.andLiked(me)
    Paginator(
      adapter = nbResults.fold(adapter) { nb =>
        new CachedAdapter(adapter, nb)
      },
      currentPage = page,
      maxPerPage = maxPerPage
    )
  }
}
