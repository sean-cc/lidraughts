package lidraughts.analyse

import lidraughts.game.Pov
import lidraughts.tree.Eval._

object Accuracy {

  private def withSignOf(i: Int, signed: Int) = if (signed < 0) -i else i

  private val makeDiff: PartialFunction[(Option[Cp], Option[Win], Option[Cp], Option[Win]), Int] = {
    case (Some(s1), _, Some(s2), _) => s2.ceiled.centipieces - s1.ceiled.centipieces
    case (Some(s1), _, _, Some(m2)) => withSignOf(Cp.CEILING, m2.value) - s1.ceiled.centipieces
    case (_, Some(m1), Some(s2), _) => s2.ceiled.centipieces - withSignOf(Cp.CEILING, m1.value)
    case (_, Some(m1), _, Some(m2)) => withSignOf(Cp.CEILING, m2.value) - withSignOf(Cp.CEILING, m1.value)
  }

  case class PovLike(
      color: draughts.Color,
      startColor: draughts.Color,
      startedAtTurn: Int
  )

  implicit def povToPovLike(pov: Pov): PovLike = PovLike(
    color = pov.color,
    startColor = pov.game.startColor,
    startedAtTurn = pov.game.draughts.startedAtTurn
  )

  def diffsList(pov: PovLike, analysis: Analysis): List[Int] = {
    if (pov.color == pov.startColor) Info.start(pov.startedAtTurn) :: analysis.infos
    else analysis.infos
  }.grouped(2).foldLeft(List[Int]()) {
    case (list, List(i1, i2)) =>
      makeDiff.lift(i1.cp, i1.win, i2.cp, i2.win).fold(list) { diff =>
        (if (pov.color.white) -diff else diff).max(0) :: list
      }
    case (list, _) => list
  }.reverse

  def prevColorInfos(pov: PovLike, analysis: Analysis): List[Info] = {
    if (pov.color == pov.startColor) Info.start(pov.startedAtTurn) :: analysis.infos
    else analysis.infos
  }.zipWithIndex.collect {
    case (e, i) if (i % 2) == 0 => e
  }

  def mean(pov: PovLike, analysis: Analysis): Option[Int] = {
    val diffs = diffsList(pov, analysis)
    val nb = diffs.size
    (nb != 0) option (diffs.sum / nb)
  }
  def mean(pov: Pov, analysis: Analysis): Option[Int] = mean(povToPovLike(pov), analysis)
}
