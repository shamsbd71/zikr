"""GTK dialogs for update notifications and the in-app changelog.
Mirrors UpdateAvailableView.swift / UpdateAvailableForm.cs: Skip This
Version / Remind Me Later / Download Update, with a changelog preview.
"""
import webbrowser

import gi

gi.require_version("Gtk", "3.0")
from gi.repository import Gtk  # noqa: E402

RELEASES_URL = "https://github.com/shamsbd71/zikr/releases/latest"

_SKIP, _REMIND, _DOWNLOAD = 1, 2, 3

_whatsnew_window = None


def show_message(title, text):
    dialog = Gtk.MessageDialog(
        transient_for=None, flags=0, message_type=Gtk.MessageType.INFO,
        buttons=Gtk.ButtonsType.OK, text=title,
    )
    dialog.format_secondary_text(text)
    dialog.run()
    dialog.destroy()


def show_update_available(current_version, new_version, changelog_body, on_skip):
    dialog = Gtk.Dialog(title="Zikr Update")
    dialog.set_default_size(440, 320)
    dialog.add_button("Skip This Version", _SKIP)
    dialog.add_button("Remind Me Later", _REMIND)
    dialog.add_button("Download Update", _DOWNLOAD)

    box = dialog.get_content_area()
    box.set_spacing(10)
    box.set_border_width(16)

    header = Gtk.Label(label="<b>A new version of Zikr is available!</b>", use_markup=True, xalign=0)
    box.pack_start(header, False, False, 0)

    subtitle = Gtk.Label(
        label=f"Zikr {new_version} is now available — you have {current_version}. "
              "Would you like to download it now?",
        xalign=0,
    )
    subtitle.set_line_wrap(True)
    box.pack_start(subtitle, False, False, 0)

    if changelog_body:
        scroller = Gtk.ScrolledWindow()
        scroller.set_size_request(-1, 130)
        text_view = Gtk.TextView()
        text_view.set_editable(False)
        text_view.set_cursor_visible(False)
        text_view.set_wrap_mode(Gtk.WrapMode.WORD)
        text_view.get_buffer().set_text(changelog_body)
        scroller.add(text_view)
        box.pack_start(scroller, True, True, 0)

    box.show_all()
    response = dialog.run()
    dialog.destroy()

    if response == _SKIP:
        on_skip()
    elif response == _DOWNLOAD:
        _open_releases()
    # _REMIND, or the window closed some other way: no-op


def show_whats_new(entries):
    global _whatsnew_window
    if _whatsnew_window:
        _whatsnew_window.present()
        return

    win = Gtk.Window(title="What's New")
    win.set_default_size(440, 420)
    win.set_border_width(16)

    def closed(*_a):
        global _whatsnew_window
        _whatsnew_window = None

    win.connect("destroy", closed)

    if not entries:
        label = Gtk.Label(label="Couldn't load the changelog. Check your internet connection.")
        label.set_line_wrap(True)
        win.add(label)
        win.show_all()
        _whatsnew_window = win
        return

    scroller = Gtk.ScrolledWindow()
    win.add(scroller)
    outer = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=16)
    scroller.add(outer)

    for entry in entries:
        header = Gtk.Label(
            label=f"<b>v{entry['version']}</b>  <span foreground='gray'>{entry['date']}</span>",
            use_markup=True, xalign=0,
        )
        outer.pack_start(header, False, False, 0)
        body = Gtk.Label(label=entry["body"], xalign=0)
        body.set_line_wrap(True)
        outer.pack_start(body, False, False, 0)

    win.show_all()
    _whatsnew_window = win


def _open_releases():
    try:
        webbrowser.open(RELEASES_URL)
    except Exception:
        import subprocess
        subprocess.Popen(["xdg-open", RELEASES_URL])
