# Design system — GitHub Pages site

Reference for `docs/index.html`. Keep edits consistent with this rather
than reaching for generic defaults (cream/serif/terracotta, near-black
+ one neon accent, broadsheet hairlines) — those are fine looks in
general but aren't *this* brief's choice.

## Direction

Islamic manuscript / illuminated-text feel: jewel-tone palette on warm
parchment, serif type in three scripts, restrained ornament (a
diamond-lattice hero texture, gold hairline accents) rather than photos
or icons-as-decoration. The one deliberate risk is trilingual RTL/LTR
support built in from the type system up, not bolted on.

## Color

Named tokens live in `:root` in `docs/index.html`. Don't hardcode hex
values in new rules — reference the token.

| Token | Hex | Use |
|---|---|---|
| `--parchment` | `#F7EFDC` | page background |
| `--parchment-panel` | `#FCF7EB` | card/panel background |
| `--ink` | `#29231D` | body text |
| `--ink-dim` | `#6B6053` | secondary text |
| `--emerald` | `#0E6B52` | primary brand color, links |
| `--emerald-deep` | `#093D30` | headings, hero gradient dark end |
| `--emerald-bright` | `#16855F` | hero gradient light end |
| `--gold` | `#B8892E` | section eyebrows, primary button gradient |
| `--gold-bright` | `#E4B94F` | hover states, hero accents |
| `--terracotta` | `#B7472A` | hover/active accent, "no" column |
| `--lapis` | `#2A5C8A` | reserved, not yet used |

The hero and the signature "yes" panel are the only two places using
the full emerald gradient — that's deliberate, it's the accent to
spend, not a background to reuse everywhere.

## Type

Three serif families, one per script, swapped via `html[lang]`:

- English: `Noto Serif` (`--font-en`)
- Bangla: `Noto Serif Bengali` (`--font-bn`)
- Arabic: `Noto Naskh Arabic` (`--font-ar`)

`--font-body` resolves to whichever is active; set once on `body`.
Headings/body copy use the serif; UI chrome (nav, buttons, labels,
badges, captions) uses the system sans-serif stack
(`-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif`) — this
split is intentional, keep new UI-level text (buttons, pills, form
labels) on the sans stack and prose on the serif.

## Layout concept

- **Hero as thesis**: headline states the actual USP ("you don't need
  to remember, this app reminds you itself") over a full-bleed emerald
  gradient with a subtle gold diamond-lattice texture — not a generic
  screenshot-plus-headline template.
- **Signature element**: the "what most reminder apps ask of you" vs.
  "what Zikr asks of you" two-column panel (`.signature-grid`) is the
  one memorable thing on the page. It's structural, not decorative — it
  literally answers the FAQ's first question. Don't add a second
  competing signature element elsewhere on the page.
- Everything else (features grid, 3-step "how it works", zikr sample
  cards, FAQ) is deliberately plain: uppercase gold eyebrows, generous
  whitespace, no competing ornament.

## Platform badges

`.platform-badges` in the hero — one pill per OS, icon + label, linking
to the latest release. Icons are hand-drawn inline SVG, single-color
(`currentColor`), ~15px:

- **macOS**: a plain apple silhouette (round body + leaf, no bite
  notch) — reads as "Mac" via the fruit convention without redrawing
  Apple's actual trademarked bitten-apple mark.
- **Linux**: a simplified penguin silhouette (Tux-inspired; Tux itself
  has a permissive license for derivative art).
- **Windows**: a plain 2×2 rounded-square grid — a generic "four panes"
  glyph, not Microsoft's flag colors/tilt.

If a fourth platform ever ships, follow the same rule: redraw something
that evokes the platform by convention, don't trace an official logo.

## Motion

Hover-only, subtle, consistent durations (`0.15s ease` for color/
background/border, transform lifts of 1–2px). No scroll-triggered or
autoplaying animation — the page's one "moment" is the static hero, not
a load sequence. `prefers-reduced-motion` only needs to guard
`scroll-behavior`, since nothing else is ambient/autoplaying.

## i18n / RTL

Three languages (`en`, `bn`, `ar`) via a `STRINGS` dict in the trailing
`<script>` and `data-i18n`/`data-i18n-html` attributes — never hardcode
user-facing copy in the markup outside that dict. Arabic flips
`dir="rtl"` on `<html>`; check any new layout (especially flex/grid
direction and icon-before-text ordering) in Arabic before shipping,
since `flex-direction`/margins don't auto-mirror.

## Platform-aware CTA

The primary hero button's label swaps based on `navigator.userAgent`
(`detectPlatform()` in the trailing script) — Mac/Linux/Windows/generic
"Download" text, all pointing at the same `releases/latest` URL. Update
the `STRINGS[lang].ctaDownload*` keys in all three languages together
if you touch this.
