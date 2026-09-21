# Design system — GitHub Pages site

Reference for `docs/index.html`. Keep edits consistent with this rather
than reaching for generic defaults — those are fine looks in general but
aren't *this* brief's choice.

## Direction

An illuminated manuscript at night. Jewel-tone emerald and luminous gold
on near-black, serif type in three scripts, restrained ornament (a
drifting diamond lattice, gold hairlines) rather than photos or
icons-as-decoration.

**Dark is the default** because the hour people actually reach for dhikr
is a dark one, and gold only reads as *illumination* when there is
something to illuminate. The earlier parchment world is preserved intact
as the light theme — nothing was thrown away, it moved behind a toggle.

The page's central idea: **it performs the product rather than
describing it.** The hero holds a live card that cycles adhkar on its
own timer, exactly as the app does, and will speak one aloud on request.
A visitor learns what Zikr is by watching it happen.

## Themes

Two, switched by `data-theme` on `<html>`:

- Saved choice wins, then `prefers-color-scheme`, then dark.
- The choice is written to `localStorage` under `zikrTheme`.
- An inline script in `<head>` sets the attribute **before first paint**,
  so a light-theme visitor never sees the dark world flash past.

## Color

Named tokens live in `:root` (dark) and `html[data-theme="light"]`.
Don't hardcode hex values in new rules — reference the token.

| Token | Dark | Light | Use |
|---|---|---|---|
| `--bg` | `#0A0F0D` | `#F7EFDC` | page background |
| `--bg-deep` | `#060A09` | `#F1E6CE` | scrollbar track, recesses |
| `--panel` | `#121B17` | `#FCF7EB` | cards, rows |
| `--panel-raised` | `#17231D` | `#FFFDF6` | hover / active card |
| `--ink` | `#EFE6D5` | `#29231D` | body text |
| `--ink-dim` | `#A6997F` | `#6B6053` | secondary text |
| `--emerald` | `#34A77F` | `#0E6B52` | links, primary accents |
| `--emerald-deep` | `#0C3A2D` | `#093D30` | play buttons, pressed states |
| `--emerald-bright` | `#47C79A` | `#16855F` | link hover, live dot |
| `--gold` | `#D6A63C` | `#B8892E` | eyebrow-free accents, tags |
| `--gold-bright` | `#F2D184` | `#8A6416` | emphasis **on the page surface** |
| `--terracotta` | `#D9714F` | `#B7472A` | the struck-through "no" column |

### Inverted surfaces — read this before touching the hero

The hero and the signature "what Zikr asks of you" panel are **dark
emerald in both themes**. Their foregrounds therefore must not flip with
the theme, and these tokens hold the same value in both:

| Token | Value | Use |
|---|---|---|
| `--on-dark` | `#F3EADA` | text on the hero / yes-panel |
| `--gold-on-dark` | `#F2D184` | gold on the hero / yes-panel |
| `--gold-on-dark-deep` | `#D6A63C` | the CTA gradient's far end |
| `--lattice-on-dark` | `rgba(242,209,132,0.17)` | the hero lattice |

This is not theory. Using the theme-flipping `--gold-bright` on the hero
dropped the primary CTA to **1.21:1** against its own label in light
mode — invisible — because light mode darkens that token for parchment.
If an element sits on a surface that is dark in both themes, it takes an
`-on-dark` token.

The related trap: **`a` selectors outrank component colors.** The CTAs
and platform badges are anchors, so a bare
`html[data-theme="light"] a { color: … }` (specificity 0,1,2) beat
`.btn-secondary` (0,1,0) and painted every hero control emerald on dark
emerald. The link rules are scoped `a:not(.btn):not(.platform-badge)`
for that reason — keep any new link rule scoped the same way, or give
the component a rule that outranks it.

## Type

Three serif families, one per script, swapped via `html[lang]`:

- English: `Noto Serif` (`--font-en`)
- Bangla: `Noto Serif Bengali` (`--font-bn`)
- Arabic: `Noto Naskh Arabic` (`--font-ar`)

`--font-body` resolves to whichever is active; set once on `body`.
Prose uses the serif; UI chrome (nav, buttons, labels, badges, captions,
durations) uses the system sans stack (`--font-ui`). Keep that split.

Arabic set as *content* (the live card, the library rows) always carries
`direction: rtl; unicode-bidi: isolate`, plus an explicit `text-align`
per page direction — `start` resolves to the right inside an rtl box,
which in an LTR layout flings the Arabic away from the transliteration
beneath it and makes the two read as unrelated.

**No eyebrows or kickers above headings.** The previous design used
uppercase gold eyebrows; they are gone and should not come back. The
heading carries its own weight.

## Layout concept

- **Hero as demonstration**: headline states the USP beside a live card
  that cycles adhkar and speaks on request.
- **Signature element**: the "what most reminder apps ask of you" vs.
  "what Zikr asks of you" panel. Structural, not decorative — it answers
  the FAQ's first question. Don't add a second competing signature.
- **The library**: eight adhkar, each playable, with real durations and
  a tag on the time-of-day one. Proof, not decoration.
- Everything else (features, 3-step how-it-works, FAQ) is deliberately
  plain: generous whitespace, no competing ornament.

## Audio on the site

`docs/audio/<id>.mp3` — eight clips copied from the canonical
`data/audio/`, ~336KB total, fetched only on click and never preloaded.
One shared `Audio` object, so only one clip plays at a time. The hero
card never cuts a recitation short to advance.

**Only short adhkar rotate in the hero card.** The 11.7s one is four
times the height of "SubhanAllah", and cycling into it shoved the page
down mid-read; it stays playable in the library, where a tall row costs
nothing. The rotation filter is `seconds <= 4`.

Local preview needs a server that honours **Range requests** —
`python3 -m http.server` does not, and Chrome's media element stalls at
`readyState 0`, which looks exactly like broken audio. GitHub Pages is
fine.

## Motion

One authored moment, not scattered effects:

- **The dhikr arrives out of soft focus** — each line of the live card
  animates in on a blur + translate, staggered ~70ms, on an exponential
  ease-out (`--ease: cubic-bezier(0.16, 1, 0.3, 1)`).
- **The lattice drifts** behind the hero, 48s linear, infinite. Ambient
  texture, not an entrance.
- The live dot pulses; the progress bar tracks real playback time.
- Everything else is hover-only and subtle: 0.15–0.2s, 1–2px lifts.

Do not add a generic scroll-reveal to every section — one identical
entrance repeated is the opposite of an authored moment.

`prefers-reduced-motion` stops the lattice, the pulse, the card
entrance, *and* the auto-cycling (the card holds one dhikr instead).

## Browser surfaces

Selection, focus rings, and scrollbars are themed from the palette in
both modes. They ship with browser defaults that belong to no design
system; keep them themed.

## i18n / RTL

Three languages (`en`, `bn`, `ar`) via a `STRINGS` dict in the trailing
`<script>` with `data-i18n` / `data-i18n-html` attributes — never
hardcode user-facing copy in the markup outside that dict. Content
rendered from JS (the library rows) carries `data-meaning-key` /
`data-tag-key` so it re-reads on a language switch instead of going
stale.

**Any copy change must land in all three languages together.** Arabic
flips `dir="rtl"` on `<html>`; check new layout in Arabic before
shipping, since `flex-direction` and margins don't auto-mirror.

## Platform badges

One pill per OS, icon + label, linking to the latest release. Icons are
hand-drawn inline SVG, single-color, ~15px, and deliberately evoke each
platform by convention rather than tracing an official logo — a plain
apple silhouette with no bite notch, a Tux-inspired penguin, a generic
2×2 pane grid, a simple robot head. If a fifth platform ships, follow
the same rule.

Note the badge SVGs use `fill="var(--hero-from)"` for cut-out details so
they track the hero background in both themes.

## Platform-aware CTA

The primary hero button's label swaps on `navigator.userAgent`
(`detectPlatform()`), all pointing at the same `releases/latest` URL.
Update the `STRINGS[lang].ctaDownload*` keys in all three languages
together if you touch this.

## Analytics

Cloudflare Web Analytics — cookieless, no personal data, no consent
banner. The beacon is **injected only when a real token is present**, so
an unconfigured checkout makes no third-party request at all; a
hardcoded placeholder would have every visitor's browser calling
Cloudflare and failing.

The site counts page views. **The app still collects nothing**, and the
FAQ says both plainly in all three languages and in the JSON-LD. If you
ever add app telemetry, that copy and the structured data have to change
first — otherwise the page is making a false claim about its own
software.
