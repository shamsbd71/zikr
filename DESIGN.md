# Design system — GitHub Pages site

Reference for `docs/index.html`. Keep edits consistent with this rather
than reaching for generic defaults — those are fine looks in general but
aren't *this* brief's choice.

## Direction

A quiet catalogue entry, in the colours of an early morning: soft sage and
mist on near-white, a muted teal accent, a little warm sand. Serif type in
three scripts. Nothing on the page should raise its voice.

**This is a catalogue, not a sales page.** Zikr is free, so the page's job
is to tell its story and describe it accurately: what it does, what it
sets, what it needs, where it falls short. Plain tables carry the facts.
Avoid urgency, comparison panels and big gradient CTAs.

**Light is the default** (a dusk teal dark theme sits behind a toggle).

The page's central idea stays: **it performs the product rather than only
describing it.** The header holds a live card that cycles adhkar on its
own timer and will speak one aloud on request.

## Themes

Two, switched by `data-theme` on `<html>`:

- Saved choice wins, then `prefers-color-scheme`, then light.
- The choice is written to `localStorage` under `zikrTheme`.
- An inline script at the top of `<body>` sets the attribute **before first
  paint**, so a dark-theme visitor never sees the light page flash past.

## Color

Named tokens live in `:root` (light) and `html[data-theme="dark"]`.
Don't hardcode hex values in new rules, reference the token. Every text
pair below measures at least 5:1 (WCAG AA).

| Token | Light | Dark | Use |
|---|---|---|---|
| `--bg` | `#F2F6F3` | `#151F1F` | page background |
| `--bg-deep` | `#E6EEEA` | `#0F1818` | scrollbar track |
| `--panel` | `#FAFCFA` | `#1C2929` | library rows |
| `--panel-raised` | `#FFFFFF` | `#233232` | live card, hover / active row |
| `--ink` | `#22322E` | `#E4ECE7` | body text |
| `--ink-dim` | `#52645E` | `#A2B3AC` | secondary text |
| `--accent` | `#2C6E63` | `#8FCABB` | links, primary button, focus |
| `--accent-strong` | `#235A51` | `#A9DACD` | Arabic text, hover, text on `--tint` |
| `--tint` | `#DDEBE6` | `#27403C` | soft fills: play buttons, maker note |
| `--sand` | `#7A6840` | `#CFBC90` | time-of-day tag |
| `--on-accent` | `#F7FBF8` | `#12201D` | text on `--accent` |
| `--hero-from` | `#DFECEE` | `#1B2C2E` | top of the header's fade into `--bg` |

Link rules stay scoped `a:not(.btn)`: a bare `a` selector outranks the
buttons' own colour.

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

**No eyebrows or kickers above headings.** The heading carries its own
weight.

## Layout concept

In order: header with live card, the story, at-a-glance spec table, the
library, options table, downloads table, FAQ.

- **The story is the signature.** It is the one section allowed to run
  long, and ends with the maker's own note. Keep that note in first person
  and true: it reports real use, so don't embellish it.
- **The tables are the catalogue.** Options name the platforms they exist
  on, because parity is not total (see AGENTS.md). If a setting's platforms
  or default changes, this table changes with it. File sizes are rounded
  ("about 6 MB") so they survive small releases.
- **The library**: eight adhkar, each playable, with real durations.

## Copy

Written to the copywriting skill's rules: plain, specific, checkable. Say
what happens instead of listing absences, keep em dashes out of short copy,
and state limits honestly (a sleeping computer stays silent).

Claims about the app must match the code. In particular the update check
runs on launch and daily, so don't write that it only goes online when
asked. Make no claim about religious reward.

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

- **The dhikr arrives out of soft focus**: each line of the live card
  animates in on a blur + translate, staggered ~70ms, on an exponential
  ease-out (`--ease: cubic-bezier(0.16, 1, 0.3, 1)`).
- The dot breathes slowly; the progress bar tracks real playback time.
- Everything else is a hover colour change. Nothing lifts or bounces.

Do not add a generic scroll-reveal to every section: one identical
entrance repeated is the opposite of an authored moment.

`prefers-reduced-motion` stops the pulse, the card entrance, *and* the
auto-cycling (the card holds one dhikr instead).

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

## Platform icons

The downloads table marks each OS with a hand-drawn inline SVG,
single-colour, ~16px, evoking the platform by convention rather than
tracing an official logo: a plain apple silhouette with no bite notch, a
Tux-inspired penguin, a generic 2x2 pane grid, a simple robot head. Cut-out
details use `fill="var(--bg)"`. If a fifth platform ships, follow the same
rule.

## Platform-aware CTA

The primary header button's label swaps on `navigator.userAgent`
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
