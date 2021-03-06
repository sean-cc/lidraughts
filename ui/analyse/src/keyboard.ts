import * as control from './control';
import AnalyseCtrl from './ctrl';
import { bind as bindEvent, dataIcon, spinner } from './util';
import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

function preventing(f: () => void): (e: MouseEvent) => void {
  return function(e) {
    if (e.preventDefault) {
      e.preventDefault();
    } else {
      // internet explorer
      e.returnValue = false;
    }
    f();
  };
}

export function bind(ctrl: AnalyseCtrl): void {
    if (!window.Mousetrap) return;
    const kbd = window.Mousetrap;
    kbd.bind(['left', 'j'], preventing(function () {
        control.prev(ctrl);
        ctrl.redraw();
    }));
    kbd.bind(['shift+left', 'shift+j', 'h'], preventing(function () {
        control.exitVariation(ctrl);
        ctrl.redraw();
    }));
    kbd.bind(['right', 'k'], preventing(function () {
        if (!ctrl.fork.proceed()) control.next(ctrl);
        ctrl.redraw();
    }));
    kbd.bind(['shift+right', 'shift+k'], preventing(function () {
        control.enterVariation(ctrl);
        ctrl.redraw();
    }));
    kbd.bind(['up', '0'], preventing(function () {
        if (!ctrl.fork.prev()) control.first(ctrl);
        ctrl.redraw();
    }));
    kbd.bind(['down', '$'], preventing(function () {
        if (!ctrl.fork.next()) control.last(ctrl);
        ctrl.redraw();
    }));
    kbd.bind('shift+c', preventing(function () {
        ctrl.showComments = !ctrl.showComments;
        ctrl.autoScroll();
        ctrl.redraw();
    }));
    kbd.bind('shift+i', preventing(function () {
        ctrl.treeView.toggle();
        ctrl.redraw();
    }));
    kbd.bind('z', preventing(function() {
      ctrl.toggleComputer();
      ctrl.redraw();
    }));

    if (ctrl.embed) return;

    for (let i = 1; i < 10; i++) {
      kbd.bind('ctrl+' + i, preventing(function() {
        ctrl.setBookmark(i);
      }));
      kbd.bind('' + i, preventing(function() {
        ctrl.restoreBookmark(i);
      }));
    }
    kbd.bind('q', preventing(function() {
      ctrl.setBookmark(1);
    }));
    kbd.bind('o', preventing(function() {
      ctrl.jumpToCurrentPath();
    }));

    kbd.bind('space', preventing(function() {
      const gb = ctrl.gamebookPlay();
      if (gb) gb.onSpace();
      else if (ctrl.studyPractice) return;
      else if (ctrl.ceval.enabled()) ctrl.playBestMove();
      else ctrl.toggleCeval();
    }));

    if (ctrl.studyPractice) return;

    kbd.bind('f', preventing(ctrl.flip));
    kbd.bind('?', preventing(function () {
        ctrl.keyboardHelp = !ctrl.keyboardHelp;
        ctrl.redraw();
    }));
    kbd.bind('l', preventing(ctrl.toggleCeval));
    kbd.bind('a', preventing(function () {
        ctrl.toggleAutoShapes(!ctrl.showAutoShapes());
        ctrl.redraw();
    }));
    kbd.bind('x', preventing(ctrl.toggleThreatMode));
    /*kbd.bind('e', preventing(function () {
        ctrl.toggleExplorer();
        ctrl.redraw();
    }));*/
    if (ctrl.study) {
        const keyToMousedown = (key: string, selector: string) => {
            kbd.bind(key, preventing(function () {
                $(selector).each(function (this: HTMLElement) {
                    window.lidraughts.dispatchEvent(this, 'mousedown');
                });
            }));
        };
        keyToMousedown('c', '.study_buttons a.comments');
        keyToMousedown('g', '.study_buttons a.glyphs');
    }
}

export function view(ctrl: AnalyseCtrl): VNode {

  return h('div.lidraughts_overboard.keyboard_help', {
    hook: {
      insert: vnode => {
        window.lidraughts.loadCss('stylesheets/keyboard.css')
        $(vnode.elm as HTMLElement).find('.scrollable').load('/analysis/help?study=' + (ctrl.study ? 1 : 0));
      }
    }
  }, [
    h('a.close.icon', {
      attrs: dataIcon('L'),
      hook: bindEvent('click', () => ctrl.keyboardHelp = false, ctrl.redraw)
    }),
    h('div.scrollable', spinner())
  ]);
}
