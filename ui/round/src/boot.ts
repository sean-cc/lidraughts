import { RoundOpts, RoundData } from './interfaces';
import { RoundApi, RoundMain } from './main';
import { ChatCtrl } from 'chat';
import { tourStandingCtrl, TourStandingCtrl, TourPlayer } from './tourStanding';
import { updateSimulStanding, SimulStanding } from './simulStanding';

const li = window.lidraughts;

export default function (opts: RoundOpts, element: HTMLElement): void {
    const data: RoundData = opts.data;
    const socketParams: any = { userTv: data.userTv && data.userTv.id };
    if (socketParams.userTv && data.userTv && data.userTv.gameId)
        socketParams.gameId = data.userTv.gameId;

    let round: RoundApi, chat: ChatCtrl | undefined;
    if (data.tournament) $('body').data('tournament-id', data.tournament.id);

    li.socket = li.StrongSocket(
        data.url.socket,
        data.player.version, {
            options: { name: 'round' },
            params: socketParams,
            receive(t: string, d: any) {
                round.socketReceive(t, d);
            },
            events: {
                crowd(e: { watchers: number }) {
                    $watchers.watchers("set", e.watchers);
                },
                tvSelect(o: any) {
                    if (data.tv && data.tv.channel == o.channel) li.reload();
                    else $('#tv_channels a.' + o.channel + ' span').html(
                        o.player ? [
                            o.player.title,
                            o.player.name,
                            '(' + o.player.rating + ')'
                        ].filter(x => x).join('&nbsp') : 'Anonymous');
                },
                end() {
                    $.ajax({
                        url: [(data.tv ? '/tv/' + data.tv.channel : ''), data.game.id, data.player.color, 'sides'].join('/'),
                        success: function (html) {
                            const $html = $(html);
                            $('#site_header div.side').replaceWith($html.find('.side'));
                            $('#lidraughts div.crosstable').replaceWith($html.find('.crosstable'));
                            li.pubsub.emit('content_loaded')();
                            startTournamentClock();
                        }
                    });
                },
                tourStanding(s: TourPlayer[]) {
                    if (opts.chat && opts.chat.plugin && chat) {
                        (opts.chat.plugin as TourStandingCtrl).set(s);
                        chat.redraw();
                    }
                },
                simulStanding(s: SimulStanding) {
                    if (data.simul && s.fg) {
                      $('#others_' + s.fg).remove();
                      if (!$('#now_playing.other_games > a:not(.game_timeout)').length)
                          $('.simul_tomove').hide();
                      if (!$('#now_playing.other_games > a.game_timeout').length)
                          $('.simul_timeouts').hide();
                    }
                    if (data.simul && data.simul.id == s.id) {
                        updateSimulStanding(s, round.trans, round.draughtsResult);
                        if (data.simul.nbPlaying != s.g) {
                          data.simul.nbPlaying = s.g;
                          if (s.g <= 1) round.redraw();
                        }
                    }
                }
            }
        });

    function startTournamentClock() {
        $("div.game_tournament div.clock").each(function (this: HTMLElement) {
            $(this).clock({
                time: parseFloat($(this).data("time"))
            });
        });
    };

    function getPresetGroup(d: RoundData) {
        if (d.player.spectator) return;
        if (d.steps.length < 4) return 'start';
        else if (d.game.status.id >= 30) return 'end';
        return;
    };

    opts.element = element.querySelector('.round') as HTMLElement;
    opts.socketSend = li.socket.send;
    if (!opts.tour && !data.simul) opts.onChange = (d: RoundData) => {
        if (chat) chat.preset.setGroup(getPresetGroup(d));
    };
    opts.crosstableEl = element.querySelector('.crosstable') as HTMLElement;

    let $watchers: JQuery;
    function letsGo() {

        round = (window['LidraughtsRound'] as RoundMain).app(opts);

        if (opts.chat) {
            if (opts.tour) {
                opts.chat.plugin = tourStandingCtrl(opts.tour, opts.i18n.standing);
                opts.chat.alwaysEnabled = true;
            } else if (!data.simul) {
                opts.chat.preset = getPresetGroup(opts.data);
                opts.chat.parseMoves = true;
            }
            li.makeChat('chat', opts.chat, function (c) {
                chat = c;
            });
        }

        $watchers = $('#site_header div.watchers').watchers();
        startTournamentClock();
        $('#now_playing').find('.move_on input').change(function () {
            var t = round.moveOn.toggle();
            $('#now_playing .move_seq').css('visibility', t ? 'visible' : 'collapse');
        }).prop('checked', round.moveOn.get()).on('click', 'a', function () {
            li.hasToReload = true;
            return true;
        });
        $('#now_playing').find('.move_seq input').change(function () {
            round.moveOn.toggleSeq();
        }).prop('checked', round.moveOn.getSeq())
        $('#now_playing .move_seq').css('visibility', round.moveOn.get() ? 'visible' : 'collapse');
        if (location.pathname.lastIndexOf('/round-next/', 0) === 0)
            history.replaceState(null, '', '/' + data.game.id);
        if (!data.player.spectator && data.game.status.id < 25) li.topMenuIntent();
        $('#zentog').click(round.toggleZen);

        li.storage.make('reload-round-tabs').listen(li.reload);
    };

    if (li.isTrident) setTimeout(letsGo, 150);
    else letsGo();

}
