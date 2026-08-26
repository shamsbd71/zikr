"""Entry point: `python3 -m zikr` or the installed `zikr` command."""
from .app import ZikrApp


def main():
    app = ZikrApp()
    app.run()


if __name__ == "__main__":
    main()
