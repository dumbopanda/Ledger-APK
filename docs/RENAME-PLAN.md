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
| 3 | Support and sign-in email | **Decided:** one Google Workspace Base user (admin@spentmap.com) with 4 aliases, from day one. Sign-in codes go out through Brevo's free plan as no-reply@spentmap.com, isolated from the admin mailbox. No Gmail is used, so users never see one. See "Email on spentmap.com". |
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
   - Email: Google Workspace (MX records), Brevo sender verification, and SPF, DKIM and DMARC records covering both. Full steps in "Email on spentmap.com".
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

1. `email-otp`: subject, text, HTML and sender name to Spent Map. Replace the Gmail SMTP sender (nodemailer, `GMAIL_USER` and `GMAIL_APP_PASSWORD` secrets) with Brevo (API key or SMTP secret), sending as `no-reply@spentmap.com` with `Reply-To: support@spentmap.com`. Remove the two Gmail secrets once verified.
2. **Global daily send cap (abuse protection).** Count codes issued in the last 24 hours (from `email_otps`); above a limit set below Brevo's 300 a day (start at 250), stop sending, return a friendly "try again later" to the app, and alert the owner once. This sits on top of the existing per-email, per-IP and per-code limits. Add a pgTAP test for it.
3. Optional: a free bot check (Cloudflare Turnstile) on the sign-up screen, only if the cap ever trips from bot traffic.
4. CORS allowlist: add https://spentmap.com (and www). Redeploy.
5. `supabase/config.toml` `project_id`: cosmetic, local only.

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
3. Email sign-in, checking the new sender name arrives in the inbox and not in spam (test Gmail and Outlook), and that "show original" reveals no Gmail address. Also confirm the global daily cap pauses sending and alerts.
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
| Sign-in emails go to spam from a new domain | SPF, DKIM and DMARC covering Workspace and Brevo, plus a test send to Gmail and Outlook before switching. |
| Sign-in form abused, sender suspended | Codes go through Brevo, not the admin mailbox, so a suspension only stops codes and never locks the owner out of Play Console or Google Cloud. A global daily cap (Phase 3) stops it before Brevo's 300 a day is exhausted. |
| Sign-in breaks when the old Gmail is retired | The Gmail is only deleted after the Brevo switch is verified live (see "Retiring theledgerapp.support@gmail.com"). |

## Effort

About 3 working sessions for Claude (app, backend and pages, verification), plus 1 to 2 hours of the owner's time in Google Cloud, the domain and Play Console.

## Moving accounts to a Spent Map identity

Today the Play developer account, GitHub, Sentry, Supabase and Google Cloud all hang off the owner's personal Google account. The goal is to separate the product from the personal identity without breaking anything, at low cost.

### Step 1: one brand identity (Workspace Base, about ₹99 a month)

1. After buying spentmap.com, sign up for Google Workspace Base and create the single user admin@spentmap.com. Add support@, privacy@, hello@ and no-reply@ as aliases of that user (see "Email on spentmap.com").
2. admin@spentmap.com is the product's admin login. It is a real Workspace account with admin recovery controls, not a forwarding trick.
3. Store every login in a password manager (e.g. Bitwarden free), with 2-step verification on each.

### Step 2: add the brand identity everywhere, then shift what's public

| Service | What to do now | Cost |
|---|---|---|
| **Play Console** | Users and permissions: add admin@spentmap.com as Admin. Set the developer name and each app's contact email to Spent Map / support@spentmap.com. The verified legal identity behind a personal account stays the owner's; that only changes when converting to an organization account with a company (see LAUNCH-PLAN.md). | ₹0 |
| **Google Cloud** (sign-in) | IAM: add admin@spentmap.com as Owner. The consent screen support email becomes the brand address. | ₹0 |
| **GitHub** | Create a free organization (e.g. `spentmap`) and transfer both repos into it. History and issues move with them, and old URLs redirect. Set git to commit with the GitHub no-reply email from now on. | ₹0 |
| **Supabase** | Invite admin@spentmap.com to the organization as Owner, or transfer the project to a new Spent Map organization. The project URL and keys stay the same, so the app needs no change. | ₹0 now |
| **Sentry** | The free Developer plan allows 1 user, so change the login email on the existing account to admin@spentmap.com instead of adding a second member. | ₹0 |
| **Sign-in email sender** | Move from theledgerapp.support@gmail.com to no-reply@spentmap.com through Brevo's free plan (300 emails a day). Sign up for Brevo with admin@spentmap.com. | ₹0 |

Nothing is deleted or re-created in step 2. Each service gains the brand identity first, and the personal account can step back later.

### Step 3: later, only with a company (and employer approval)

- Convert the Play account from personal to organization (needs a company and a free D-U-N-S number).
- Payments profile, Supabase and Sentry billing, and GitHub organization ownership move to the company.

## Email on spentmap.com (decided 30 Sept 2026)

### Decision

**Google Workspace Base from day one, one paid user, and sign-in codes sent through Brevo's free plan.** This replaces the earlier "free forwarding first" idea. Reasons:

- No Gmail with "ledger" in its name sits in the chain, and users never see a Gmail address anywhere.
- One system to run: Google receives mail directly, so Cloudflare Email Routing is not needed. Cloudflare stays only as free DNS.
- Replying as support@ is built in (Send mail as an alias).
- The admin mailbox never sends bulk mail, so an abused sign-up form cannot get the mailbox that holds the Play Console and Google Cloud logins suspended.

Why Brevo and not the free Gmail as a hidden sender: mail sent through Gmail's servers is signed by gmail.com, not spentmap.com, so some clients show "via gmail.com" or "on behalf of", delivery is weaker, and the old Ledger Gmail would have to be kept alive. Brevo is a background service, not a mailbox, signs as spentmap.com with the domain's own records, and shows nothing to users.

| Piece | Choice | Cost |
|---|---|---|
| Mailbox | Google Workspace Base, **1 user** (admin@spentmap.com), 20 GB, up to 30 aliases per user | ₹99 per user per month (₹49.50 for the first 3 months), about ₹1,040 in year 1 plus 18% GST |
| Sign-in codes | Brevo free plan, sending as no-reply@spentmap.com | ₹0 (300 emails a day) |
| DNS | Cloudflare free (Porkbun DNS also works) | ₹0 |

Create **one Workspace user with aliases, not five users.** Five users would cost five times as much (₹495 a month).

Skipped: Microsoft 365 Business Basic (₹170 per user per month), Porkbun Email (about ₹260 per user per month), and Zoho Mail Lite (₹59 per user per month billed yearly; the cheapest paid alternative if Google is ever too costly).

### Addresses (1 mailbox, 4 aliases)

| Address | Kind | Display name | Purpose |
|---|---|---|---|
| admin@spentmap.com | The one Workspace user | Spent Map Admin | Brand login for Play Console, Google Cloud, Supabase, GitHub, Sentry, Brevo |
| support@spentmap.com | Alias | Spent Map Support | Users, Play listing contact, app Settings, Instagram bio |
| privacy@spentmap.com | Alias | Spent Map Privacy | Privacy policy and the DPDP grievance contact |
| hello@spentmap.com | Alias (optional) | Spent Map | Press, partners, marketing |
| no-reply@spentmap.com | Alias, sending identity only | Spent Map | Sender of sign-in codes through Brevo, with Reply-To: support@ |

Add a Gmail filter per alias to keep them sorted into labels.

Safety rule: the **domain registrar (Porkbun) and Cloudflare logins must not use an @spentmap.com address.** If the domain breaks, those are the accounts needed to fix it. Keep them on an independent email with 2-step verification.

### Setup order

1. Buy spentmap.com (Porkbun, $11.08 a year). Use an independent email for the registrar login.
2. Point DNS at Cloudflare (free). GitHub Pages works fine behind it.
3. Sign up for Google Workspace Base, verify spentmap.com with the TXT record, and create admin@spentmap.com.
4. Add the four aliases in the Workspace admin console, and add Google's MX records in Cloudflare.
5. In Gmail, set up "Send mail as" for support@ and privacy@ so replies show the right address.
6. Sign up for Brevo using admin@spentmap.com, add spentmap.com as a sender domain, and publish Brevo's DKIM and verification records in Cloudflare.
7. Publish one SPF record that includes both Google and Brevo (a domain can have only one SPF record), Google's DKIM, and a DMARC record starting at `p=none`, moving to `quarantine` after a week of clean reports.
8. Switch the `email-otp` function to Brevo (Phase 3), including the global daily cap. Test delivery to Gmail and Outlook, and confirm "show original" shows no Gmail address.
9. Update the pages, Play listing and Google consent screen contact to support@ and privacy@spentmap.com.

### Retiring theledgerapp.support@gmail.com

It is the current sender for sign-in codes and is printed on the live privacy policy and terms, so retire it in this order:

1. Brevo sending verified live (step 8 above).
2. Pages, listing and app updated to the new addresses (step 9).
3. Set the old Gmail to auto-forward everything to support@spentmap.com.
4. Leave it for a few months until nothing points to it, then delete it. A deleted Gmail account is recoverable only for a short time, and an idle one costs nothing, so there is no rush.
5. Any test users created with it lose their sign-in, so delete or re-create those test accounts first.

### If a second mailbox is needed later

Add a second Workspace user (another ₹99 a month), for a team member or a dedicated sending mailbox. There is one domain only (spentmap.com); no second domain is bought or planned.

### Abuse protection

- **Existing limits (email-otp):** one code per email every 2 minutes, at most 5 an hour and 10 a day per email, at most 20 an hour per IP, and a code dies after 5 wrong guesses.
- **Added in Phase 3:** a global daily cap below Brevo's 300 a day, with an alert, so many rotating IPs cannot exhaust the quota or get the sender suspended. Optional Turnstile bot check.
- **Blast radius:** if Brevo ever suspends sending, only sign-in codes stop. Google login still works, and so do the owner's admin accounts.

### Limits and prices checked (30 Sept 2026)

- **Google Workspace (India):** Base ₹99, Starter ₹270, Standard ₹1,080 (₹864 introductory) per user per month. Base allows up to 20 users, 20 GB pooled storage, 14-day trial.
- **Brevo free:** 300 emails a day. Paid Starter from about ₹625 a month.
- **Resend free (fallback sender):** 100 emails a day, 3 domains.
- **Microsoft 365 Business Basic:** ₹170 per user per month, billed yearly, plus GST.
- **Zoho Mail:** Mail Lite ₹59, Workplace Standard ₹99 per user per month billed yearly; a forever-free 5-user plan exists in select regions.
- **Cloudflare Email Routing:** free, unlimited inbound; Email Sending needs Workers Paid at $5 a month.
- **Porkbun:** free forwarding (20 addresses), Porkbun Email about $3 per user per month.

## Upcoming costs

| Item | When | Cost |
|---|---|---|
| spentmap.com | Now | about ₹950 a year (Porkbun $11.08) |
| spentmap.in / .app (optional) | Now | about ₹675 a year / about ₹750, then ₹1,300 a year |
| **Google Workspace Base, 1 user** (mailbox, aliases, admin login) | Now | ₹49.50 a month for the first 3 months, then ₹99 (about ₹1,040 in year 1) plus 18% GST |
| GitHub org, Brevo free (sign-in codes), Cloudflare DNS, Sentry Developer, Supabase Free, Bitwarden | Now | ₹0 |
| Trademark filing, class 9 (optional, recommended) | Soon | about ₹4,500 (online, individual or startup) |
| Supabase Pro (daily backups, no pausing) | From public launch | $25 a month, about ₹2,150 |
| Brevo paid plan | Only if sign-in codes exceed 300 a day | from about ₹625 a month |
| Sentry Team (optional) | Only with teammates | $26 a month |
| Company, D-U-N-S, organization Play account | When monetizing | LLP about ₹10,000 to ₹20,000 a year; D-U-N-S free (see LAUNCH-PLAN.md) |
| Apple Developer Program | Only for iOS | $99 a year |

**Before public launch:** about ₹950 to ₹2,400 a year for domains, about ₹1,040 for Workspace in year 1 (plus GST), and the optional ₹4,500 trademark filing. **From public launch:** add about ₹2,150 a month for Supabase Pro.
