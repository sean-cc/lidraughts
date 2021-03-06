import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Position, MaybeVNodes } from '../interfaces';
import * as game from 'game';
import * as status from 'game/status';
import { Player }  from 'game/interfaces';
import { renderClock } from '../clock/clockView';
import renderCorresClock from '../corresClock/corresClockView';
import renderReplay from './replay';
import renderExpiration from './expiration';
import * as renderUser from './user';
import * as button from './button';
import RoundController from '../ctrl';

function renderPlayer(ctrl: RoundController, player: Player) {
  return ctrl.nvui ? undefined : (
    player.ai ? h('div.username.user_link.online', [
      h('i.line'),
      h('name', renderUser.aiName(ctrl, player.ai))
    ]) :
    renderUser.userHtml(ctrl, player)
  );
}

function isLoading(ctrl: RoundController): boolean {
  return ctrl.loading || ctrl.redirecting;
}

function loader() { return h('span.ddloader'); }

function renderTableWith(ctrl: RoundController, buttons: MaybeVNodes) {
  return [
    renderReplay(ctrl),
    h('div.control.buttons', buttons),
    renderPlayer(ctrl, ctrl.playerAt('bottom'))
  ];
}

function renderTableEnd(ctrl: RoundController) {
  return renderTableWith(ctrl, [
    isLoading(ctrl) ? loader() : (button.backToTournament(ctrl) || button.followUp(ctrl))
  ]);
}

function renderTableWatch(ctrl: RoundController) {
  return renderTableWith(ctrl, [
    isLoading(ctrl) ? loader() : button.watcherFollowUp(ctrl)
  ]);
}

function renderTablePlay(ctrl: RoundController) {
  const d = ctrl.data,
    loading = isLoading(ctrl),
    submit = button.submitMove(ctrl),
    drawHint = d.drawLimit === undefined ? 'offerDraw' : (d.drawLimit <= 0 ? 'drawOffersNotAllowed' : (d.game.turns < d.drawLimit * 2 ? 'drawOffersAfterX' : 'offerDraw')),
    drawHintArg = (d.drawLimit !== undefined && d.drawLimit > 0 && d.game.turns < d.drawLimit * 2) ? d.drawLimit.toString() : undefined,
    icons = (loading || submit) ? [] : [
      game.abortable(d) ? button.standard(ctrl, undefined, 'L', 'abortGame', 'abort') :
      (ctrl.canTimeOut() ? (ctrl.timeOutConfirm ? button.timeOutConfirm(ctrl) : button.timeOutButton(ctrl)) :
      button.standard(ctrl, game.takebackable, 'i', 'proposeATakeback', 'takeback-yes', ctrl.takebackYes)),
      ctrl.timeOutConfirm ? button.timeOutConfirmChoice(ctrl) : null,
      ctrl.timeOutConfirm ? null : ctrl.drawConfirm ? button.drawConfirm(ctrl) : button.standard(ctrl, ctrl.canOfferDraw, '2', drawHint, 'draw-yes', () => ctrl.offerDraw(true), drawHintArg),
      ctrl.timeOutConfirm ? null : ctrl.resignConfirm ? button.resignConfirm(ctrl) : button.standard(ctrl, game.resignable, 'b', 'resign', 'resign-confirm', () => ctrl.resign(true))
    ],
    buttons: MaybeVNodes = loading ? [loader()] : (submit ? [submit] : [
      button.forceResign(ctrl),
      button.threefoldClaimDraw(ctrl),
      button.cancelDrawOffer(ctrl),
      button.answerOpponentDrawOffer(ctrl),
      button.cancelTakebackProposition(ctrl),
      button.answerOpponentTakebackProposition(ctrl)
    ]);
  return [
    renderReplay(ctrl),
    h('div.control.icons', {
      class: { 'confirm': !!(ctrl.drawConfirm || ctrl.resignConfirm || ctrl.timeOutConfirm) }
    }, icons),
    h('div.control.buttons', buttons),
    renderPlayer(ctrl, ctrl.playerAt('bottom'))
  ];
}

function whosTurn(ctrl: RoundController, color: Color) {
  var d = ctrl.data;
  if (status.finished(d) || status.aborted(d)) return;
  return h('div.whos_turn',
    d.game.player === color ? (
      d.player.spectator ? ctrl.trans(d.game.player + 'Plays') : ctrl.trans(
        d.game.player === d.player.color ? 'yourTurn' : 'waitingForOpponent'
      )
    ) : ''
  );
}

function anyClock(ctrl: RoundController, position: Position) {
  const player = ctrl.playerAt(position);
  if (ctrl.clock) return renderClock(ctrl, player, position);
  else if (ctrl.data.correspondence && ctrl.data.game.turns > 1)
    return renderCorresClock(
      ctrl.corresClock!, ctrl.trans, player.color, position, ctrl.data.game.player
    );
  else return whosTurn(ctrl, player.color);
}

export function renderInner(ctrl: RoundController): VNode {
  return h('div.table_inner',
    ctrl.data.player.spectator ? renderTableWatch(ctrl) : (
      game.playable(ctrl.data) ? renderTablePlay(ctrl) : renderTableEnd(ctrl)
    )
  );
}

export function renderTable(ctrl: RoundController): VNode {
  const contents: MaybeVNodes = [
    renderPlayer(ctrl, ctrl.playerAt('top')),
    renderInner(ctrl)
  ],
    expiration = game.playable(ctrl.data) && renderExpiration(ctrl);
  return h('div.table_wrap', {
    class: { with_expiration: !!expiration }
  }, [
    anyClock(ctrl, 'top'),
    expiration && !expiration[1] ? expiration[0] : null,
    h('div.table', contents),
    expiration && expiration[1] ? expiration[0] : null,
    anyClock(ctrl, 'bottom')
  ]);
};
