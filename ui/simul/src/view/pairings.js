var m = require('mithril');
var util = require('./util');
var ceval = require('./ceval');
var status = require('game/status');

var boardContent = m('div.cg-board-wrap', m('div.cg-board'));

function miniPairing(ctrl) {
  return function(pairing) {
    var game = pairing.game;
    var player = pairing.player;
    var result = pairing.game.status >= status.ids.aborted ? (
      pairing.winnerColor === 'white' ? (ctrl.pref.draughtsResult ? '2-0' : '1-0')
      : (pairing.winnerColor === 'black' ? (ctrl.pref.draughtsResult ? '0-2' : '0-1')
      : (ctrl.pref.draughtsResult ? '1-1' : '½-½'))
    ) : '*';
    return m('div', { class: ctrl.evals !== undefined ? 'gauge_displayed' : '' }, [
      m('a', {
        href: '/' + game.id + '/' + game.orient,
        class: 'mini_board live_' + game.id + ' parse_fen is2d',
        'data-color': game.orient,
        'data-fen': game.fen,
        'data-lastmove': game.lastMove,
        config: function(el, isUpdate) {
          if (!isUpdate) lidraughts.parseFen($(el));
        }
      }, boardContent),
      m('div', {
        class: 'vstext clearfix' + (ctrl.data.host.gameId === game.id ? ' host' : '')
      }, [
        m('div.left', [
          util.playerVariant(ctrl, player).name,
          m('br'),
          result
        ]),
        m('div.right', [
          player.username,
          m('br'),
          player.title ? player.title + ' ' : '',
          player.officialRating ? ('FMJD ' + player.officialRating) : player.rating
        ])
      ]),
      ctrl.evals !== undefined ? ceval.renderGauge(pairing, ctrl.evals) : null
    ]);
  };
}

module.exports = function(ctrl) {
  return m('div.game_list.playing', ctrl.data.pairings.map(miniPairing(ctrl)));
};
