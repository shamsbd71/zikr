"""A borderless, always-on-top window that briefly shows a zikr centered on
screen, then fades out on its own. Mirrors FlashOverlayController.swift.
"""
import gi

gi.require_version("Gtk", "3.0")
from gi.repository import GLib, Gtk  # noqa: E402

_window = None
_fade_source = None
_dismiss_source = None


def _build_window(zikr):
    win = Gtk.Window(type=Gtk.WindowType.POPUP)
    win.set_decorated(False)
    win.set_keep_above(True)
    win.set_skip_taskbar_hint(True)
    win.set_skip_pager_hint(True)
    win.set_position(Gtk.WindowPosition.CENTER)
    win.set_accept_focus(False)

    screen = win.get_screen()
    visual = screen.get_rgba_visual()
    if visual:
        win.set_visual(visual)
    win.set_app_paintable(True)

    box = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=10)
    box.set_border_width(36)

    arabic = Gtk.Label(label=zikr["arabic"])
    arabic.set_name("zikr-arabic")

    translit = Gtk.Label(label=zikr["transliteration"])
    translit.set_name("zikr-translit")

    meaning = Gtk.Label(label=zikr["translation"])
    meaning.set_name("zikr-meaning")

    for lbl in (arabic, translit, meaning):
        lbl.set_justify(Gtk.Justification.CENTER)
        box.pack_start(lbl, False, False, 0)

    win.add(box)

    css = Gtk.CssProvider()
    css.load_from_data(b"""
        window { background-color: rgba(14, 30, 27, 0.92); border-radius: 22px; }
        #zikr-arabic { color: #f4d58d; font-size: 34px; font-weight: 600; }
        #zikr-translit { color: #ffffff; font-size: 18px; }
        #zikr-meaning { color: #cfcfcf; font-size: 13px; }
    """)
    Gtk.StyleContext.add_provider_for_screen(
        screen, css, Gtk.STYLE_PROVIDER_PRIORITY_APPLICATION
    )

    return win


def present(zikr, duration_seconds):
    global _window, _fade_source, _dismiss_source

    if _dismiss_source:
        GLib.source_remove(_dismiss_source)
        _dismiss_source = None
    if _fade_source:
        GLib.source_remove(_fade_source)
        _fade_source = None
    if _window:
        _window.destroy()

    _window = _build_window(zikr)
    _window.set_opacity(0)
    _window.show_all()
    _fade_in()

    _dismiss_source = GLib.timeout_add(int(duration_seconds * 1000), _start_fade_out)


def _fade_in(step=0.0):
    if not _window:
        return False
    step = min(1.0, step + 0.15)
    _window.set_opacity(step)
    if step < 1.0:
        GLib.timeout_add(16, lambda: _fade_in(step))
    return False


def _start_fade_out():
    global _dismiss_source
    _dismiss_source = None
    _fade_out(1.0)
    return False


def _fade_out(step):
    global _window
    if not _window:
        return False
    step = max(0.0, step - 0.08)
    _window.set_opacity(step)
    if step > 0.0:
        GLib.timeout_add(16, lambda: _fade_out(step))
    else:
        _window.destroy()
        _window = None
    return False
