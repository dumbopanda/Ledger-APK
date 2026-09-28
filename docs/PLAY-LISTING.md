# Play Store listing — Ledger

Ready-to-paste content for Play Console. Companion to `BUILD-LOG.md`/`ledger-plan-v2.md`;
covers §11.3's "Data Safety form" and "Store listing" requirements. Update this file, not just
Play Console, whenever the listing changes — same reason `KNOWN-ISSUES.md` exists: one place
that stays true, not tribal knowledge in a form only Google can see.

---

## App details

| Field | Value |
|---|---|
| App name | Ledger |
| Short description (≤80 chars) | Track expenses, loans and transfers — private, offline-first, no ads. |
| Category | Finance |
| Contact email | theledgerapp.support@gmail.com |
| Privacy policy URL | https://dumbopanda.github.io/Ledger-Pages/privacy/ |
| Website (optional) | https://dumbopanda.github.io/Ledger-Pages/ |
| Distribution | **India only** — see "Country availability" below |

## Country availability — India only

**Decided 26 Sep 2026.** This app was built for personal use first; a wider audience is
welcome but not the goal, and going worldwide brings compliance obligations (GDPR and similar
regimes elsewhere) that aren't worth taking on for an app with no plan to actively support a
global user base yet. Restricting distribution is a Play Console **listing** setting, not an
app-code change — the app itself has no geofencing, and none is needed.

**Where to set it:** Play Console → your app → **Grow → Store presence → Main store listing**
is the content; the actual restriction lives at **Grow → Store presence → Countries/regions**
(sometimes just labeled "Availability" depending on Console's current layout). It defaults to
every country selected — deselect all, then select only **India**. This applies to the
production listing; closed/internal testing tracks have their own separate country list, so
set it there too when creating the testing release, or testers outside India won't be able to
install it.

This also simplifies the Data Safety form and privacy policy: DPDP Act (India) is the only
regime the policy needs to speak to, which is already how `Ledger-Pages/privacy/` is written —
no GDPR-specific language exists there to walk back.

Short description is exactly 71 characters — fits the 80-char limit with room to spare.

## Full description (≤4000 chars)

```
Ledger is a private, no-nonsense expense tracker for people who want to know exactly
where their money went, without an app trying to sell them a credit card in the
process.

WHAT IT DOES
• Log payments and income across as many accounts as you use: cash, bank, card,
  UPI wallet, whatever you actually track. Start each one with the balance it holds.
• Move money between your own accounts with a single Self Transfer entry. It never
  counts as spending.
• Track money you've lent with "Will I get this back?". Open loans stay in Owed to me
  until they're repaid, so nothing quietly gets forgotten, and loans never inflate
  your spending.
• See your history day by day, edit or delete anything, and check each account's
  balance and where the month's money went in Stats.

BUILT TO WORK OFFLINE
Every screen works immediately, with or without a connection. Entries save to your
phone first and back up to your account as soon as you're online. No spinner, no
"waiting for network" screen between you and logging a payment.

YOUR DATA, KEPT PRIVATE
• Sign in with Google, or with a 6-digit code sent to your email. No password to
  create or lose.
• Your entries sit in a private database only your account can read. Not shared,
  not sold, never used for advertising.
• No analytics, no tracking SDKs, no ad network of any kind.
• Optional app lock with your fingerprint or phone PIN.
• Delete your account and everything in it, permanently, any time: from inside the
  app or from a public web page, with no waiting period.

Ledger doesn't connect to your bank, doesn't move real money, and doesn't offer
financial advice. It's a fast, private place to write down what you spent, what you
lent, and what's still open. Nothing more, and nothing hidden.
```

Character count: about 1,780, well under the 4,000 limit.

## Graphics needed

| Asset | Size | Status |
|---|---|---|
| App icon (hi-res) | 512×512 PNG, 32-bit with alpha | **Done** — `store-assets/ic_launcher-playstore.png` (repo root, parent dir) |
| Feature graphic | 1024×500 PNG or JPG | Not made yet — see below |
| Phone screenshots | 2–8, min 320px, JPG/24-bit PNG | **Needs the owner's device** — capture during the real-device test pass (any screen: Add Payment, History, Accounts, Settings all work) |

**App icon:** the owner's own design (a lime rupee-circle badge on dark `#121614` — matches the app's existing dark-mode canvas color and lime accent exactly), replacing an earlier AI-drafted placeholder. Wired in as the real in-app launcher icon (`app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png` + a solid-color background vector) and exported separately as the flat 512×512 `store-assets/ic_launcher-playstore.png` for Play Console — upload that file directly, no further export step needed.

**Feature graphic (1024×500):** no template exists yet. Simplest option: the same dark `#121614` background, the lime rupee-circle mark, and the word "Ledger" in white — recreate at 1024×500 in any free tool (Canva, or even Google Slides sized to that canvas, exported as PNG). Not code, so left for a design pass rather than guessed at here.

## Data Safety form

Straight from `ledger-plan-v2.md` §11.3 — this must match what the app actually does exactly, or Google can suspend the listing, not just warn.

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | Yes |
| Data collected: **Personal info → Email address** | Yes, required. **Account management** (sign-in, by Google or by emailed code) and **App functionality** |
| Data collected: **Personal info → Name** | Yes, required (typed at sign-up, or from Google). **App functionality** (greeting) and **Account management** |
| Data collected: **Personal info → User IDs** | Yes, required. **App functionality** |
| Data collected: **Personal info → Other info** (age, city, occupation) | Yes, **optional** (the user can skip it). **Analytics** (understanding who uses the app). Not shared, not used for ads. |
| Data collected: **Financial info → Other financial info** | Yes, required. **App functionality** (entries, amounts, opening balances) |
| Data collected: **App info and performance → Crash logs, Diagnostics** | Yes, required (Sentry; amounts and names stripped). **App functionality**, to fix crashes. Sentry is a service provider, so this is not "sharing". |
| Any other category (Location, Photos, Contacts, SMS, Health, etc.) | No. The city is typed in by the user, not read from the device, so it isn't "Location". |
| Is all collected data encrypted in transit? | Yes |
| Can users request their data be deleted? | Yes — in-app (Settings → Delete account) and via a public web page |
| Is data shared with third parties? | No |
| Is data used for advertising or marketing? | No |
| Is data processing purpose limited to what's declared? | Yes |

## Content rating questionnaire

Ledger has no violence, no user-generated content shown to others, no gambling, no real-money transactions inside the app, no ads, no chat/social features. Answer every category question **No** — this should land at the lowest rating tier (Everyone / 3+ equivalent) in every regional system Play asks about (IARC, ESRB, PEGI, etc. — one questionnaire drives all of them).

## Target audience

Not designed for or directed at children. Recommended age range: **18+** (a personal-finance app; Google generally expects finance apps to declare an adult primary audience even without an explicit age gate in the app itself).

## Financial features declaration

Per §11.3: declare Ledger as **not** a regulated financial product. It records numbers the user types in — no loans issued, no investments, no payment processing, no bank connection. Do not use wording anywhere in the listing that implies otherwise (checked: the description above avoids "invest," "loan you money," "pay," etc. in that sense).
