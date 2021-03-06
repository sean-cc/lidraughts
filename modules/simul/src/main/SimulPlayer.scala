package lidraughts.simul

import draughts.variant.Variant
import lidraughts.rating.Perf
import lidraughts.user.User

private[simul] case class SimulPlayer(
    user: User.ID,
    variant: Variant,
    rating: Int,
    provisional: Option[Boolean],
    officialRating: Option[Int]
) {

  def is(userId: User.ID): Boolean = user == userId
  def is(other: SimulPlayer): Boolean = is(other.user)
}

private[simul] object SimulPlayer {

  private[simul] def make(user: User, variant: Variant, perf: Perf): SimulPlayer = {
    new SimulPlayer(
      user = user.id,
      variant = variant,
      rating = perf.intRating,
      provisional = perf.provisional.some,
      officialRating = user.profile.flatMap(_.fmjdRating)
    )
  }
}
