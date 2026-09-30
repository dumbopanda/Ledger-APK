# Rename plan: Ledger to Spent Map

Written 30 Sept 2026. Plan only; nothing here has been executed.

Name clearance is in `SPEND-NAMES-REPORT.md` and `NAME-SEARCH.md`. Still open before starting: the owner's own IP India search (WIPO's Indian data stops at 7 March 2025).

## Why now

No build has been uploaded to Play yet, and the 15 to 20 lined-up testers have not installed anything. The package ID is not locked, so this is the cheapest moment the rename will ever have.

## Scope

| Area | What carries "Ledger" |
|---|---|
| App code | About 1,790 mentions in 252 files. About 95% are internal (the Kotlin package `com.dumbopanda.ledger` and class names like `LedgerTheme`). **About 30 are user-visible text.** |
| App identity | App name (`strings.xml`), package ID `com.dumbopanda.ledger`, biometric prompt, backup file name `ledger-backup-DATE.json` |
| Backend | `supabase/functions/email-otp/index.ts`: subject "is your Ledger sign-in code", body, sender name "Ledger". The database, schema and user accounts need **no change**. |
| Public pages | `ledger-pages/`: 5 files, 52 mentions (privacy, terms, delete-account, home, `assets/ledger.css`). URLs contain `Ledger-Pages`. |
| Outside accounts | Google Cloud sign-in setup, Play Console, support email `theledgerapp.support@gmail.com`, Sentry, GitHub |
| Docs | `PLAY-LISTING.md`, `KNOWN-ISSUES.md`, memory. `BUILD-LOG.md` history stays as written. |

## Decisions needed from the owner

| # | Decision | Recommendation |
|---|---|---|
| 1 | New package ID (permanent after first upload) | `com.spentmap.app` (matches the owned domain). Fallback: `com.dumbopanda.spentmap`. |
| 2 | Rename internal code too? | Not now. Rename everything users see plus the package ID; keep "Ledger" as the internal code name. |
| 3 | Support and sign-in email | support@spentmap.com, with sign-in codes sent through Brevo or Resend (fixes the Gmail sending limit). Fallback: a new Gmail address. |
| 4 | Where the policy pages live | spentmap.com/privacy, /terms, /delete-account, still hosted free on GitHub Pages. |
| 5 | Play title (30 characters max) | "Spent Map: Expense Tracker" (26) or "Spent Map" |
| 6 | Tagline and positioning | Owner's call. Options: "See where your money went." / "Every rupee, mapped." / "Know where it went. Know who owes you." (keeps the IOU side visible) |
| 7 | Logo | Keep the lime rupee mark; it does not depend on the name. |

## Phases

### Phase 0 (owner): before anything changes

- IP India search: Wordmark "SPENT MAP" and "SPENTMAP", classes 9, 36 and 42. Also check "SPENDMAP".
- Buy spentmap.com (plus .in and .app if wanted).
- Claim @spentmap on Instagram, X and YouTube.
- Answer the 7 decisions above.

### Phase 1 (owner, with guidance): outside accounts and identity

1. **Brand identity:** set up the Spent Map admin identity (see "Moving accounts to a Spent Map identity" below).
2. **Google Cloud:**
   - New Android OAuth client for the new package ID, with the debug and upload-key SHA-1 fingerprints. Add the Play app-signing SHA-1 after the first upload.
   - Rename the consent screen to "Spent Map". Set the user support email to the brand address, not a personal one.
   - Add https://spentmap.com as an authorized JavaScript origin on the web client, for the delete page.
3. **Domain:**
   - GitHub Pages custom domain for spentmap.com, with HTTPS.
   - Email: forwarding for support@, plus SPF, DKIM and DMARC records for the sending service.
4. **Play Console:**
   - Create the app as "Spent Map" (nothing uploaded yet).
   - Set the developer name, and the public contact email to support@spentmap.com.
5. **Optional:** rename the Sentry project and the GitHub repos. Do **not** rename the local folders (`D:\[02] GROWTH\Ledger APK`, `ledger-app`); the tooling and the Sentry workaround in KNOWN-ISSUES depend on those paths.

### Phase 2 (Claude): the app

1. `applicationId` to the new package ID; `app_name` to "Spent Map".
2. Rewrite the roughly 30 user-visible lines:
   - "Start my Ledger" and "Create my Ledger"
   - "Start your / Ledger." and the Home and onboarding wordmarks
   - "Ledger is / locked." and "Unlock Ledger"
   - "Ledger works out the year" and "Ledger files this under yesterday"
   - "That isn't a Ledger file"
   - "Balance when you started with Ledger"
   - The tour, profile setup and consent copy
3. Rewrite the onboarding and app tour copy for the new positioning, once the tagline is chosen.
4. Backup file name to `spentmap-backup-DATE.json`. Keep old `ledger-backup` files restoring (the import does not check the app name today), and add a test that restores a `"app": "ledger"` file.
5. Merge the policy URLs duplicated in `OnboardingScreen.kt`, `SettingsScreen.kt` and `AuthScreen.kt` into the existing `core/ui/ExternalLinks.kt`, pointing at spentmap.com.
6. Update the tests that assert copy (e.g. `DateFieldHintTest`), and run the full unit test suite.

### Phase 3 (Claude): backend

1. `email-otp`: subject, text, HTML and sender name to Spent Map, and switch sending to the new address or service. Update the function secrets.
2. CORS allowlist: add https://spentmap.com (and www). Redeploy.
3. `supabase/config.toml` `project_id`: cosmetic, local only.

### Phase 4 (Claude): public pages

1. Rebrand privacy, terms, delete-account and home, with a new effective date, the new contact email and the new app name. Rename `ledger.css`.
2. Serve the pages from spentmap.com. Keep the old `dumbopanda.github.io/Ledger-Pages/...` URLs working (forwarding pages) until the Play listing points at the new ones.
3. The public repo's commit history shows the author email of every commit. Consider starting the Spent Map pages in a fresh repo, with the GitHub no-reply email configured, rather than renaming Ledger-Pages.

### Phase 5 (Claude drafts, owner files): store and compliance

- Rewrite `PLAY-LISTING.md`: title, short and long description, and feature graphic text.
- Refresh the contact email in the Data Safety answers. The data practices themselves do not change.
- Take store screenshots once the new copy is final.

### Phase 6 (Claude on the emulator, owner on a real phone): verification

1. Fresh install of the new package.
2. Google sign-in through the new Android client.
3. Email sign-in, checking the new sender name arrives in the inbox and not in spam (test Gmail and Outlook).
4. Restore from an **old Ledger backup file**.
5. The biometric lock prompt text.
6. Account deletion in the app and on the new web page.
7. The forced-update screen.
8. Sentry receiving a test crash.

### Phase 7 (Claude): close out

- Build log entry, `KNOWN-ISSUES.md` update, memory update, commit and push.
- Then the signed AAB goes to internal testing, and closed testing starts under the new name.

### Optional, later: internal code rename

Kotlin packages, the `Ledger*` classes, the build plugin IDs (`ledger.android.*`), `rootProject.name`, Room schema folders, proto `java_package`, and ProGuard keep rules. Only if wanted for future developers' clarity.

## Risks

| Risk | Handling |
|---|---|
| Local data on the owner's phone and the emulator | The new package installs as a fresh app. Synced data returns on sign-in. Press "Send everything now" on the old app first. |
| Google sign-in breaks | The new Android client is created before the app build changes, and is tested first in Phase 6. |
| Old backups stop restoring | A new test restores a `ledger` backup. |
| Links to the old pages break | The old GitHub Pages URLs keep forwarding until Play Console is updated. |
| Sign-in emails go to spam from a new domain | SPF, DKIM and DMARC, plus a test send to Gmail and Outlook before switching. |

## Effort

About 3 working sessions for Claude (app, backend and pages, verification), plus 1 to 2 hours of the owner's time in Google Cloud, the domain and Play Console.

## Moving accounts to a Spent Map identity

Today the Play developer account, GitHub, Sentry, Supabase and Google Cloud all hang off the owner's personal Google account. The goal is to separate the product from the personal identity without breaking anything, at close to zero cost.

### Step 1: one brand identity (₹0)

1. After buying spentmap.com, create admin@spentmap.com and support@spentmap.com. Either forward them free to the personal inbox with Cloudflare Email Routing, or use a free mailbox (Zoho Mail's free plan).
2. Create a free Google account using admin@spentmap.com (Google accepts non-Gmail addresses). This becomes the product's admin login.
3. Store every login in a password manager (e.g. Bitwarden free), with 2-step verification on each.

### Step 2: add the brand identity everywhere, then shift what's public

| Service | What to do now | Cost |
|---|---|---|
| **Play Console** | Users and permissions: add admin@spentmap.com as Admin. Set the developer name and each app's contact email to Spent Map / support@spentmap.com. The verified legal identity behind a personal account stays the owner's; that only changes when converting to an organization account with a company (see LAUNCH-PLAN.md). | ₹0 |
| **Google Cloud** (sign-in) | IAM: add admin@spentmap.com as Owner. The consent screen support email becomes the brand address. | ₹0 |
| **GitHub** | Create a free organization (e.g. `spentmap`) and transfer both repos into it. History and issues move with them, and old URLs redirect. Set git to commit with the GitHub no-reply email from now on. | ₹0 |
| **Supabase** | Invite admin@spentmap.com to the organization as Owner, or transfer the project to a new Spent Map organization. The project URL and keys stay the same, so the app needs no change. | ₹0 now |
| **Sentry** | The free Developer plan allows 1 user, so change the login email on the existing account to admin@spentmap.com instead of adding a second member. | ₹0 |
| **Sign-in email sender** | Move from theledgerapp.support@gmail.com to support@spentmap.com through Brevo (free, 300 emails a day) or Resend (free, 100 a day). | ₹0 |

Nothing is deleted or re-created in step 2. Each service gains the brand identity first, and the personal account can step back later.

### Step 3: later, only with a company (and employer approval)

- Convert the Play account from personal to organization (needs a company and a free D-U-N-S number).
- Payments profile, Supabase and Sentry billing, and GitHub organization ownership move to the company.

## Email on spentmap.com (decided approach, researched 30 Sept 2026)

### Recommendation

**Start free now, and upgrade to one Google Workspace Base user only when launching publicly or earning.** Only one inbox is ever needed: every address forwards into it.

| Stage | Setup | Cost |
|---|---|---|
| **Now** | Cloudflare Email Routing (receive) into the existing theledgerapp.support@gmail.com, plus Brevo free (send sign-in codes, and let Gmail "Send mail as" support@spentmap.com) | ₹0 |
| **Public launch or revenue** | Google Workspace Base, 1 user (admin@spentmap.com). All other addresses become aliases or groups of that one user. | ₹99 per user per month (₹49.50 for the first 3 months), about ₹1,188 a year plus GST |
| Cheaper paid alternative | Zoho Mail Lite, 1 user | ₹59 per user per month billed yearly, about ₹708 a year plus GST |

Skipped: Microsoft 365 Business Basic (₹170 per user per month) and Porkbun Email (about ₹260 per user per month). Both cost more for what this project needs.

### Addresses (5, all into one inbox)

| Address | Purpose |
|---|---|
| support@spentmap.com | Users, the Play listing contact, app Settings, Instagram bio |
| privacy@spentmap.com | Privacy policy and the DPDP grievance contact |
| no-reply@spentmap.com | Sender of sign-in codes (Reply-To: support@) |
| admin@spentmap.com | Brand Google account: Play Console, Google Cloud, Supabase, GitHub, Sentry |
| hello@spentmap.com (optional) | Press, partners, marketing |

Safety rule: the **domain registrar and Cloudflare logins must not use an @spentmap.com address.** If the domain breaks, those are the accounts needed to fix it. Keep them on an independent email with 2-step verification.

### Setup order

1. Buy spentmap.com (Porkbun, $11.08 a year).
2. Move DNS to Cloudflare (free). GitHub Pages works fine behind it.
3. Cloudflare Email Routing: create the 5 addresses, forwarding to theledgerapp.support@gmail.com.
4. Brevo: add spentmap.com as a sender domain and publish its SPF, DKIM and DMARC records in Cloudflare.
5. Gmail: "Send mail as" support@spentmap.com through Brevo's SMTP relay.
6. Create the free brand Google account on admin@spentmap.com (verification arrives through the forwarding).
7. email-otp function: switch secrets from Gmail SMTP to Brevo, sender no-reply@spentmap.com. Test delivery to Gmail and Outlook before switching.

### Free-tier limits checked

- **Cloudflare Email Routing:** 200 addresses per domain plus catch-all, unlimited inbound.
- **Brevo free:** 300 emails a day.
- **Resend free:** 100 a day, 3 domains.
- **Zoho Forever Free:** 5 mailboxes of 5 GB each, no IMAP, select regions only.
- **Porkbun forwarding:** 20 addresses.
- **ImprovMX free:** 25 addresses, 500 forwards a day.
- **Cloudflare Email Sending:** needs Workers Paid at $5 a month, which includes 3,000 emails.

If sign-in codes ever exceed 300 a day, Brevo's paid plans start at about ₹625 a month.

## Upcoming costs

| Item | When | Cost |
|---|---|---|
| spentmap.com | Now | about ₹950 a year (Porkbun $11.08) |
| spentmap.in / .app (optional) | Now | about ₹675 a year / about ₹750, then ₹1,300 a year |
| Brand Google account, GitHub org, email forwarding, Brevo/Resend, Sentry Developer, Supabase Free, Bitwarden | Now | ₹0 |
| Trademark filing, class 9 (optional, recommended) | Soon | about ₹4,500 (online, individual or startup) |
| Supabase Pro (daily backups, no pausing) | From public launch | $25 a month, about ₹2,150 |
| Google Workspace mailbox (optional, instead of forwarding) | If wanted | roughly ₹150 to ₹200 per user per month; confirm current pricing |
| Sentry Team (optional) | Only with teammates | $26 a month |
| Company, D-U-N-S, organization Play account | When monetizing | LLP about ₹10,000 to ₹20,000 a year; D-U-N-S free (see LAUNCH-PLAN.md) |
| Apple Developer Program | Only for iOS | $99 a year |

**Before public launch:** about ₹950 to ₹2,400 a year for domains, plus the optional ₹4,500 trademark filing. **From public launch:** add about ₹2,150 a month for Supabase Pro.
