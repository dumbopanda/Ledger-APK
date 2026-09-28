# BUILD LOG — Ledger

**Wed 16 Sep → ~Fri 16 Oct 2026 · 30 days** (was 31 — Sprint A took 2 days, not 3; everything from Sprint B on is pulled forward one calendar day to match. See note at "Schedule update" below.)

Companion to `ledger-plan-v2.md` (the *what*). This is the *when*.

---

## The one thing that matters now

**You are not the bottleneck. Google is.**

| Phase | Duration | Who controls it |
|---|---|---|
| Building the app | ~10 days | **You** — compressible |
| Play Console identity verification | days–weeks | Google — **starts when you submit** |
| 14-day closed testing clock | exactly 14 days | Google — **starts when 12 testers install** |
| Play review | 2–5 days | Google |

Everything after "closed track" is fixed-duration waiting. So the whole plan is: **reach the closed track as fast as possible**, then use the 14 days you're forced to wait for hardening and polish.

**Two things must start TODAY regardless of code:**
1. Play Console identity verification — this can take weeks and blocks everything
2. Line up your 15 testers — social lead time, not technical

---

## Sessions

No hour allocations. A **block** is one task run to its check. Do as many blocks per session as you have appetite for.

| | Time |
|---|---|
| **Primer** | 16:00 — trivial chat, opens window 1 |
| **S1** | 19:00–21:00 |
| **S2** | 21:00–23:00 — opens window 2 |
| **Weekend** | add afternoon blocks freely |

**Start of every session:** *"Read BUILD-LOG.md. Today is [date]. What's next, and does the repo match?"*
**End:** *"Update BUILD-LOG.md — what's done, what's next."*

Sonnet throughout. Opus only if a Phase 3 test fails twice and you can't see why.

---

## What changed from the 60-day plan

Build work collapsed roughly 5×. Verification did not — a property test either passes or you debug it, and that takes what it takes. So the plan now separates them: **generate fast, verify properly.** The verify blocks below are the ones you cannot rush, and they're marked ⚠️.

---

# SPRINT A · Backend + shell · Wed 16 – Thu 17 Sep ✅ DONE (1 day under the 3 allocated)

Goal: **a real phone signs in with Google and reads its own row from Postgres.**

| Block | Task | Check |
|---|---|---|
| A1 | Repo, `.gitignore`, Supabase project ap-south-1, CLI linked | ✅ done tonight |
| A2 | GitHub private repo · `keepalive.yml` · `backup.yml` | Workflows run green |
| A3 | ⚠️ **Take a backup and restore it into a scratch project** | Data verified by hand 🚪**P0** |
| A4 | Migrations `0001`–`0003` — profiles, app_config, accounts, entries, enums, all CHECKs, all indexes | `supabase db reset` twice, clean |
| A5 | `0004` all six triggers | Client `updated_at` overwritten · one transfer leg deletes its sibling · one-legged transfer can't commit · entry cap fires |
| A6 | `0005` RLS all policies | ⚠️ **Verify the omissions by hand: no DELETE policy anywhere, no `profiles_insert`** |
| A7 | `0006` RPCs — `pull_entries` keyset, `push_entries` upsert, `open_y_ids` | ⚠️ **Page-boundary test: 3 rows one txn, limit 2, none skipped.** Push 3× → 1 row. `coalesce` delete guard holds. |
| A8 | `0007` delete_my_account + Edge Function · Supabase Google provider wired | Sign-in on Supabase's own auth page creates a `profiles` row |
| A9 | pgTAP suite · `db.yml` CI | ⚠️ **Break a policy → CI fails → revert** 🚪**P1** |
| A10 | Android project, version catalog, convention plugins, all 17 modules, Hilt | `./gradlew build` green |
| A11 | `core:model` value classes · `core:designsystem` (TextMuted → `#5F5C56`) · 8 components ≥48dp | Previews render at 1.0× and 2.0× |
| A12 | `core:database` entities, DAOs, per-user DB file · `core:network` · `core:datastore` | DAO tests green · `openYIds()` correct |
| A13 | GCP Android OAuth client (needs SHA-1) · Credential Manager → `signInWithIdToken` | ⚠️ **Real phone, not emulator** 🚪**P2** |
| A14 | **[ADDED]** `0008_hardening.sql` per plan §4.8 — account-ownership FK, RLS `select`-wrap, statement-level entry-count trigger, `check_transfer_group` user-scoped, `Coral` contrast fix in `LedgerColors.kt` | `db reset` clean · `test db --local` 7/7 · insert entry as user A against user B's `account_id` → rejected |

**📌 Office, Wed/Thu:** start Play Console identity verification. Nothing here unblocks it.

---

# SPRINT B · Sync + entry form · Fri 18 – Tue 22 Sep

Goal: **the two things that are genuinely hard.** Do not rush either.

| Block | Task | Check |
|---|---|---|
| B1 | ⚠️ **Read §5 of the plan in full before writing anything.** `PendingOpEntity`, `OpLog` | Ops return in strict `seq` order |
| B2 | `OpCoalescer` — all 8 rules | — |
| B3 | ⚠️ **`OpCoalescerTest` — all 8 rows, plus transfer-pair atomicity** | 8/8 pass. Not 7. |
| B4 | `Pusher` — seq order, batching, stop-at-first-failure, 401/4xx/5xx branches | 250 ops → 3 calls · failure on call 2 leaves 2 and 3 queued · 4xx dead-letters and never loops |
| B5 | `Puller` — keyset cursor, tombstones, one txn per page, cursor advances after commit | Kill mid-pull → resumes clean, no dupes |
| B6 | `SyncWorker` + `SyncScheduler` (settle/ceiling/3 modes), 6 trigger points | — |
| B7 | ⚠️ **Property test, 1,000 iterations** | Green. If it fails, this is the work — don't move on. |
| B8 | ⚠️ **Airplane mode on a real phone: add → edit → delete → reconnect** | **Server has no row** 🚪**P3** |
| B9 | `ParseDateInputUseCase` + `DateParserTest` 45+ cases | Every error message correct · `29.2` finds a real leap day · `31.4` errors, doesn't clamp |
| B10 | `DateField` — filter, caret-on-reject, hint states, blur rewrite | ⚠️ **By hand: select-all-delete, mid-string edit, partial input preserved** |
| B11 | `FormatAmountUseCase` + caret mapping tests · `AmountField` VisualTransformation | ⚠️ **By hand: type `500000`, caret must not move. Lakh readback shows.** |
| B12 | `AccountChips` + swap rule · `TxnIdField` · `TxnTypeCombo` | One tap resolves a collision, never blocks |
| B13 | `SelfTransferToggle` — all 9 state changes · `ValidateEntryDraft` in order · `ConfirmSheet` | Toggle on/off repeatedly → clean state every time 🚪**P4** |

**📌 By Tue 22:** Play verification should be submitted and in progress. If it hasn't been started, stop and do it.

---

# SPRINT C · Features + ship · Wed 23 – Sun 27 Sep

Goal: **on the closed track by Sunday.**

**Gap noticed 25 Sep, flagging rather than resolving on my own judgment:** the Stats tab
(`ui-spec.md` §3, the personal-tracker-vs-APK tile split) has never appeared in this block
table under any name, C1 through C14. It isn't skippable — it's one of the three top-level
tabs — but nothing here schedules building it. Needs a decision: fold it into an existing
block (C6 Accounts is the closest existing home, since both read account balances) or add a
new block.

| Block | Task | Check |
|---|---|---|
| C1 | `addEntry` / `addTransfer` · History PagingSource, grouping, day nets | Offline entry appears instantly, one pending op |
| C2 | Transfer folding by `transfer_group` · row anatomy · status chips · month card | Transfer is one line · month card ignores transfers |
| C3 | Owed-to-me tab, reactive query, badge · Entry detail sheet | Y entry appears, its return removes it |
| C4 | Edit mode — banner, frozen original, shape refusal, transfer both-leg | Edit twice before sync → second edit wins |
| C5 | `DeleteEntryUseCase` · 3 confirm variants · 5s undo | 🚪**P5** |
| C6 | Accounts screen · first-run seeding · archived handling | Rename collision refused readably |
| C7 | Settings — sync, security, about, dead-letter, sign-out guard | Pending ops block sign-out with an honest count |
| C8 | Export + Import via SAF · userId mismatch rejection · nightly nudge | ⚠️ **Export → uninstall → reinstall → import → identical ledger. Import twice → no change.** 🚪**P6** |
| C9 | Onboarding 3 panes (export button on pane 2) · consent versioned · Boot routing all 6 rows | ⚠️ **Airplane + expired token still opens the ledger** |
| C10 | SignIn 7 states · Restoring · SessionExpired · two-user DB isolation | Sign in as A, then B, then A → A's data intact |
| C11 | Delete-account 7 steps + public web page | New account → use → export → delete → nothing left 🚪**P7** |
| C12 | Keystore + **Play App Signing enrolment** + offline copy · R8 · Sentry with PII scrub | Deliberate crash → readable trace, no amounts attached |
| C13 | Privacy policy · ToS · deletion URL live · Play listing · **Data Safety form** | All three URLs open in a private window |
| C14 | Signed AAB → internal track → install from Play → promote to closed | 🚪**P8** |

---

# ⏱ THE CLOCK · Mon 28 Sep – Sun 11 Oct (target — see caveat below)

**Starts the day 12 testers have installed and opened the app. Recruit 15.**

Nothing you build shortens this. Use it.

**Dates below assume Sprint C also finishes on its new (pulled-forward) schedule and 12 testers are ready to install the moment it lands on the closed track.** Unlike Sprint A/B/C, this phase's start date is not something finishing code early can move — it's gated purely on tester recruitment. If testers aren't lined up by the time C8 (closed track) lands, the clock starts whenever they actually install, not on this date.

| Days | Work |
|---|---|
| 28–30 Sep | Triage day-1 feedback · ship fix build #1 · **daily: Sentry, Vitals, reply to every tester** |
| 1–3 Oct | TalkBack pass · 48dp audit · 2.0× font scale · contrast verification · Roborazzi baselines · Compose UI tests |
| 4–5 Oct | ⚠️ **Low-end device full flow** · process death mid-form · airplane cycle · 2-account and 0-account devices · wrong clock · fix everything found |
| 6 Oct | ⚠️ **Sync drift check — Supabase table vs 3 tester devices.** Any drift is a Phase 3 bug and drops everything else. |
| 7 Oct | ⚠️ **Backup restore drill** · property test at 10,000 iterations |
| 8 Oct | Copy review — every string read aloud · empty states · fresh-account first run |
| 9 Oct | Pre-launch report in Play Console · listing polish from what testers actually say it does |
| 10 Oct | Final feedback sweep · ship **release candidate** · feature freeze |
| 11 Oct | RC verification, no code changes · rollout plan + rollback rule (*halt below 99% crash-free*) |

**Daily, 10 minutes, non-negotiable:** reply to every tester. Silence is why testers drop, and dropping below 12 restarts the 14 days.

---

# LAUNCH · Mon 12 – Fri 16 Oct

| Day | Task |
|---|---|
| **Mon 12** | 14 days complete — verify 12+ continuous and genuine usage · **apply for production access** · submit RC 🚪**P10** |
| **Tue 13** | Review pending, do not resubmit · **upgrade Supabase to Pro (~₹2,200/mo)** · verify backups · usage alerts · support templates |
| **Wed 14** | Buffer for review turnaround |
| **Thu 15** | On approval → **10%** · monitor crash-free, sign-in completion, sync failures, dead-letter table |
| **Fri 16** | Clean 24h → **50%** → **100%** · live restore drill · tag `v1.0.0` 🚀 |

---

# Gates

| | Target | Actual | ✓ |
|---|---|---|---|
| P0 backup restored | Wed 17 Sep | Thu 17 Sep | ✅ |
| P1 RLS proven in CI | Thu 18 Sep | Thu 17 Sep | ✅ |
| P2 sign-in on real device | Thu 18 Sep | Thu 17 Sep | ✅ |
| P3 property test + airplane cycle | Mon 21 Sep | | ⬜ |
| P4 entry form complete | Tue 22 Sep | Thu 25 Sep | ✅ |
| P5 full CRUD | Thu 24 Sep | Thu 25 Sep | ✅ |
| P6 export/import round trip | Fri 25 Sep | | ⬜ |
| P7 signup → deletion | Sat 26 Sep | | ⬜ |
| P8 on closed track | Sun 27 Sep | | ⬜ |
| ⏱ **clock starts** | Mon 28 Sep (tester-gated — see Clock section) | | ⬜ |
| P10 production applied | Mon 12 Oct | | ⬜ |
| 🚀 live at 100% | Fri 16 Oct | | ⬜ |

---

# Schedule update — Thu 17 Sep

Sprint A was allocated 3 days (Wed 16 – Thu 18 Sep) and finished in 2 (done end of day Thu 17). Every date from Sprint B onward is pulled forward one calendar day to match — Sprint B/C's dates move because build work can move; **the Clock's dates are a target, not a promise**, since that phase is gated on tester recruitment, not on how fast anyone builds. If 12 testers aren't installed the moment C8 lands, the clock starts later than shown here, full stop — nothing below changes that mechanic, only the target it's measured against.

---

# The three things that can actually break this

**1. Play Console identity verification.** Needed by Fri 26 Sep. Takes days to weeks and there is no workaround. **If it isn't submitted today or tomorrow, this timeline is fiction.** Submit it, then check it every other day.

**2. Testers.** You need 15 committed by Sat 27 Sep. Start asking this week. Recruiting on the day you need them is how 14 days becomes 21.

**3. Phase 3 verification.** Generating the sync engine is one session. Knowing it's correct is the property test plus the airplane-mode cycle, and if either fails you debug until it doesn't. **This is the only place where going faster costs you the product**, because the failure mode is silent data loss months after launch.

---

# If you finish a sprint early

Don't pull work forward into Phase 3 verification. Instead:

- Recruit more testers (18 gives real margin)
- Start using the app for your own real expenses immediately — you're user zero and you'll find things no test does
- Write the Play listing and screenshots early
- Extra property-test iterations

**You cannot start the clock early.** It starts when the build is on the closed track and 12 people have installed it, and that's the only lever.

---

# Session log

```
## Wed 16 Sep · S1
Blocks done: A1 (repo/Supabase link, done prior session), A2 (GitHub remote pushed,
  keepalive.yml + backup.yml both green after debugging: password-in-URI needed
  URI-safe reset, pg_dump needed the versioned binary path since Ubuntu's default
  pg_dump (16) can't dump a Postgres 17 server)
Gates passed: none yet
Blocked on: nothing
Next: A3 — take a backup, restore into a scratch project, verify by hand (P0)
```

```
## Thu 17 Sep · S1
Blocks done: A3 — installed Docker Desktop + WSL2 (needed for A4 too), dumped
  schema+data from the linked project via `supabase db dump`, spun up a scratch
  project, restored via psql in a throwaway postgres:17 container. Had to scope
  the data dump to --schema public (a full dump pulled in Storage-internal
  tables like buckets_vectors that the plain postgres role can't write to,
  which aborted the single-transaction restore). Verified a probe row restored
  byte-identical (id, note, created_at all matched). Test table dropped from
  real project; scratch project deleted after.
Gates passed: P0 (backup restored, verified by hand)
Blocked on: nothing
Next: A4 — migrations 0001–0003 (profiles, app_config, accounts, entries,
  enums, CHECKs, indexes); `supabase db reset` twice, clean
Also done same session: A4 — wrote migrations 0001-0003 verbatim from
  ledger-plan-v2.md §4.1-4.3 (profiles/app_config, accounts, entries + enums,
  CHECKs, indexes). `supabase start` pulled the full local stack via Docker
  for the first time; `supabase db reset` run twice, both clean. Pushed to
  GitHub.
Next: A5 — 0004_triggers.sql (all six triggers: timestamps, transfer delete
  cascade, transfer pair integrity, entry cap, profile bootstrap, column guard)
Also done same session: A5 — wrote 0004_triggers.sql (six triggers per
  §4.4: stamp x2, cascade delete, transfer integrity, entry cap, profile
  bootstrap; note the profile column-guard trigger actually lives in
  0005_rls.sql per the plan, not here). `db reset` twice clean, then
  behaviorally verified all four named checks with a throwaway psql script
  against local Docker Postgres: client updated_at overwritten, one-legged
  transfer rejected at commit (P0001), cascade delete hit both legs, entry
  cap fired (P0002). Reset again to wipe test data, pushed.
Next: A6 — 0005_rls.sql (RLS enable+force on all three tables, policies,
  profile column-guard trigger). Verify the deliberate omissions by hand:
  no DELETE policy anywhere, no profiles_insert.
Also done same session: A6 — wrote 0005_rls.sql (enable+force RLS, all
  select/insert/update policies, profile column-guard trigger). `db reset`
  twice clean. Verified by hand against local Docker Postgres as the real
  `authenticated` role (not postgres superuser, which bypasses RLS and gave
  false passes on the first attempt — also had to fix `set_config(...,
  is_local)` to false since psql autocommits each top-level statement as
  its own transaction): pg_policies confirms no DELETE policy anywhere and
  no profiles_insert; cross-user select isolation holds; delete blocked;
  profiles insert blocked; column-guard trigger neutralizes a client
  plan/entry_count escalation attempt. Reset again, pushed.
Next: A7 — 0006_rpc.sql (pull_entries/pull_accounts keyset pagination,
  push_entries single-transaction upsert, open_y_ids). Page-boundary test:
  3 rows one txn, limit 2, none skipped. Push 3x -> 1 row. coalesce delete
  guard holds.
Also done same session: A7 — wrote 0006_rpc.sql. Found and fixed a real bug
  in the plan's push_entries SQL, not just a test bug: RETURNS TABLE(
  client_uid, ...) makes client_uid a PL/pgSQL OUT-parameter variable in
  scope for the whole body, colliding with the client_uid column in
  `on conflict (user_id, client_uid)` -> "ambiguous column reference" on
  every call. Fixed with `#variable_conflict use_column`. `db reset` twice
  clean, then verified all three named checks: page-boundary pagination
  correct across 3 same-transaction rows (identical updated_at) with
  limit 2, no skips/dupes; push 3x same client_uid -> exactly 1 row
  (was_insert: t, f, f); coalesce delete guard survives a stale
  non-deleted replay after soft delete. Reset again, pushed.
Next: A8 — 0007_account_deletion.sql (delete_my_account + Edge Function),
  Supabase Google provider wired. Check: sign-in on Supabase's own auth
  page creates a profiles row.
Also done same session: A8 — wrote 0007_account_deletion.sql
  (delete_my_account per §4.7) and a delete-account Edge Function
  (auth:'user' via @supabase/server — plan only diagrams the function,
  no literal code, so this is original; fixed verify_jwt from the
  scaffold's default false to true per Supabase's documented guidance
  for 'user' auth mode). Verified end-to-end against local Docker: signup
  creates a profiles row, authenticated call deletes user+profile+cascade,
  unauthenticated call gets 401. Wired real Google OAuth (GCP client +
  Supabase provider config, both done by user — credential entry isn't
  something I do). Tested the actual check on the LIVE project via
  Supabase's authorize endpoint + real Google sign-in: auth.users row
  created for bibhutibhusanpanda2003@gmail.com, profiles row auto-created
  with display_name from Google. Left that account in place (it's the
  user's real one, not test data). Note: this is NOT the same as P2 (sign-
  in on a real Android device via Credential Manager) — that's still A13.
Next: A9 — pgTAP suite + db.yml CI. Check: break a policy -> CI fails ->
  revert (P1 gate).
Also done same session: A9 — wrote supabase/tests/rls_entries.sql (7
  assertions per plan §10.4: cross-user select isolation, insert-as-
  another-user blocked, delete blocked, transfer cascade, entry cap,
  created_at immutability, tombstones in pull). Corrected one assertion
  against the plan's own illustrative snippet: it used throws_ok for a
  blocked DELETE, but RLS DELETE/SELECT filter silently via USING (0 rows,
  no exception) — only INSERT/UPDATE raise via WITH CHECK, confirmed by
  hand in A6. Wrote db.yml (start local stack -> db reset from scratch ->
  supabase test db --local), pushed, went green on GitHub's own runners.
  Then ran the actual P1 gate drill for real: weakened entries_select to
  using(true), confirmed it failed locally AND on GitHub Actions (byte-
  identical failure), then reverted and confirmed green again on both.
Gates passed: P1 (RLS proven in CI)
Next: A10 — Android project: Gradle, version catalog, convention
  plugins, all 17 modules, Hilt. Check: ./gradlew build green. First
  Android/Kotlin work of the project — everything through A9 has been
  Postgres/SQL/CI.
```

```
## Thu 17 Sep · AM verify
Blocks done: none new — this was a bulletproofing pass over A1-A9 before
  starting A10, per request. Did NOT trust the prior session's hand-verified
  notes at face value; re-ran everything cold against a freshly reset local
  DB today:
  - `git status`/`log`: working tree clean, all 7 migrations + pgTAP suite +
    workflows pushed to origin/main, nothing stray.
  - `supabase db reset`: clean apply of 0001-0007 with no errors.
  - `supabase test db --local`: rls_entries.sql 7/7 assertions pass fresh.
  - `supabase migration list`: local and remote both at 0007, in sync.
  - `supabase functions list` (remote): delete-account is ACTIVE, verify_jwt
    true, matches what A8 deployed.
  - Re-verified A6's two deliberate omissions declaratively via pg_policies
    on the fresh DB: exactly 9 policies exist (select/insert/update on
    accounts+entries, select/update on profiles, select on app_config) —
    no DELETE policy anywhere, no profiles_insert. Confirmed cross-user
    isolation (user B sees 0 of user A's rows) and insert-as-another-user
    blocked by RLS, both live against real auth.uid() context (authenticated
    role + request.jwt.claims), not the postgres superuser.
  - Re-verified A7's two hand-checks with a throwaway psql script (not
    committed — pgTAP only covers RLS, not RPCs, which is a real gap if
    these ever regress silently): push_entries same client_uid 3x -> 1 row,
    was_insert t/f/f; pull_entries keyset pagination across 3 rows sharing
    one transaction's `now()` (so identical updated_at) with limit 2 ->
    page 1 + page 2 together cover all 5 rows, no skips, no dupes, ordering
    falls back to `id` correctly on the timestamp tie.
  - Checked .gitignore (root + supabase/) and `git ls-files` for anything
    secret-shaped: clean. No .env/.keystore/service-role-key ever tracked.
  - Could NOT verify GitHub Actions run status (keepalive/backup/db.yml)
    remotely — no `gh` CLI installed on this machine and the built-in
    browser has no GitHub session for the private repo. They were confirmed
    green in the A2/A9 sessions; worth a manual glance next time you're
    actually in a browser logged into GitHub, but not blocking.
Gates passed: none new (P0/P1 re-confirmed, not re-graded)
Blocked on: nothing
Next: A10 — Android project: Gradle, version catalog, convention
  plugins, all 17 modules, Hilt. Check: ./gradlew build green. First
  Android/Kotlin work of the project — everything through A9 has been
  Postgres/SQL/CI.
```

```
## Thu 17 Sep · PM (A10)
Blocks done: A10 — Android project scaffold. This machine had zero Android
  toolchain (no JDK, no SDK, no Android Studio) going in, so most of this
  session was environment setup, not app code:
  - Installed Android Studio. winget's own download hung/failed silently
    for ~2 hours (its Delivery Optimization layer failed fast, then fell
    back to a plain download that died at almost exactly the 60-minute
    mark, twice) — this machine's connection to Google's CDN drops
    long-lived low-throughput connections around that mark regardless of
    client. Worked around it with a self-resuming curl loop (proactive
    ~55min self-timeout + resume, rather than waiting on the network's own
    abrupt cutoff) once that pattern was identified.
  - Provisioned the SDK (platform-tools, platforms;android-36,
    build-tools;36.0.0) headlessly via cmdline-tools/sdkmanager, since the
    silent Studio install skips the GUI setup wizard. sdkmanager's own
    downloader has no resume/retry and died mid-package twice; switched to
    downloading each package directly via curl against the exact URLs in
    Google's repository manifest (repository2-3.xml), verified every one
    by SHA1/SHA256 against that manifest before trusting it — caught one
    silently-truncated platform-tools.zip this way (passed as "downloaded"
    with exit 0, was 69KB short) and a self-inflicted directory-nesting bug
    from `mv` into a pre-existing empty stub dir.
  - Wrote build-logic/convention (6 plugins: application, library,
    jvm-library for core:model/common per §6.2, compose, hilt, room),
    version catalog, and all 18 modules (11 core + 6 feature + app) per
    §6.1, each a minimal Hilt-wired skeleton — no real code yet, that
    starts at A11.
  - First `./gradlew build` surfaced a real, unplanned version-matrix
    problem, not a typo: AGP 9.2.0 (initial pin) broke on
    `CommonExtension` going non-generic — fixable — but chasing that
    revealed Hilt 2.59+ requires AGP 9+ (2.58 is the last version
    supporting AGP 8.x) and Compose BOM 2026.08.00+ needs AGP 9.1+ and
    compileSdk 37. Rather than do a full AGP 9 DSL migration this early,
    downgraded to AGP 8.13.0 + Hilt 2.58 + Compose BOM 2026.06.01 (Compose
    UI 1.11.4, the latest BOM still under the AGP 9 requirement) — a
    deliberately conservative, mutually-compatible combo, same reasoning
    as the earlier Kotlin 2.3.20 pin (avoid bleeding-edge breakage this
    early, revisit when there's a reason to).
  - A session interruption mid-build also corrupted Gradle's global
    version-cache lock file (`Unexpected lock protocol found in lock
    file`) — cleared `~/.gradle/caches/9.1.0` and build-logic's local
    `.gradle` to recover; unrelated to the version-matrix issue above.
Check: `./gradlew build` green — 1432 tasks, app:assembleRelease and
  app:lintDebug both pass. Committed and pushed.
Gates passed: none (A10 has no named gate, just the inline check)
Blocked on: nothing
Next: A11 — core:model value classes, core:designsystem (TextMuted ->
  #5F5C56, per §6.3's own noted WCAG AA fix), 8 components >=48dp. Check:
  previews render at 1.0x and 2.0x.
```

```
## Thu 17 Sep · PM (A11)
Blocks done: A11 — core:model value classes and core:designsystem.
  - `core:model` (pure Kotlin, no Android import): all 10 files from §6.2 —
    `Money` and `TxnId` value classes exactly as specified (positive-only
    minor units, `[YN][a-z0-9]{6}` regex), `Entry`/`Account`/`UserProfile`
    data classes mirroring the §4 table CHECK constraints in their `init`
    blocks (so an invalid domain object can't be constructed, not just
    rejected later by Postgres), `TxnDirection`/`TxnFrequency` enums,
    `TransferPair` (enforces same group/opposite directions/equal
    amount/same date — matches the `TransferPairTest` cases named in
    §10.1), `SyncState` sealed interface covering the push-loop's own
    states from §5.3 including `SessionExpired`, `OpenYId` (a `TxnId`
    guaranteed to be a Y-id).
  - `core:designsystem` (android-library + compose): `LedgerColors`
    (branding doc's palette; `TextMuted` shipped as `#5F5C56`, the darkened
    value §6.3 and §10.5 both call for, not the `#78756E` the branding doc
    itself gives — that one fails WCAG AA at ~3.9:1), `LedgerTypography`,
    `LedgerShapes`, `LedgerTheme`, `icon/LedgerIcons`, and all 9
    `component/` files (Chip, SegmentedButton, PillButton, TextField,
    BottomSheet, Card, AmountField, Toast, Banner) each with a ≥48dp touch
    target and `@Preview(fontScale = 1.0f/2.0f)` pair.
  - Inter is wired as a downloadable Google Font (provider + cert array),
    which is the "local fallback, never network-only" behavior §6.3 asks
    for for free: Compose renders the system default typeface while Inter
    is unresolved (no Play services, first run before the font's cached),
    swaps in once available. Pulled the cert array's real bytes from
    Google's own `android/compose-samples` repo on GitHub rather than
    typing them from memory — a single wrong byte in that blob fails
    silently at runtime with no build error, so verified against source
    instead of guessing.
  - AmountField deliberately stayed presentational (raw string in, no
    minor-units parsing/formatting inside it) — that logic is
    `FormatAmountUseCase`'s job per §6.6, which doesn't exist yet;
    formatting business rules inside a design-system leaf would jump the
    module boundary the plan itself draws.
  - Added two libs to the version catalog: `androidx-compose-ui-text-
    google-fonts` and `androidx-compose-material-icons-core` (both
    BOM-managed, no separate version string).
  - One real bug caught by the build, not by review: `import
    androidx.compose.foundation.layout.weight` in `LedgerBanner.kt` bound
    to an unrelated *internal* `RowColumnParentData?.weight` property in
    the same package instead of the `RowScope.weight()` modifier — `weight`
    is a scope member, not a top-level import, inside a `Row`/`Column`
    lambda. Removed the import; the member resolves implicitly.
Check: `./gradlew build` green (`core:model:compileKotlin`,
  `core:designsystem:compileDebugKotlin`, `app:assembleRelease`,
  `app:lintDebug` all pass). `core:designsystem:lintDebug` reports only
  35 pre-existing warnings (Correctness:Messages, Typos) — none reference
  any file added this block, so nothing new introduced. Could **not**
  verify the Preview pane rendering visually — this session has CLI/browser
  access only, no Android Studio window to screenshot. Every component has
  a valid `@Preview(fontScale = 1.0f)` / `@Preview(fontScale = 2.0f)` pair
  and the module compiles clean, which is the strongest signal available
  headlessly; open Android Studio's Split/Preview on `core/designsystem`
  next time you're at the machine to actually look at them before trusting
  this gate fully passed.
Gates passed: none new (A11 has no 🚪 gate)
Blocked on: nothing structurally; Preview-pane visual confirmation above
  is a soft TODO, not a blocker for moving on
Next: A12 — core:database entities, DAOs, per-user DB file (`ledger-
  ${userId}.db`); core:network; core:datastore. Check: DAO tests green,
  `openYIds()` correct.
```

```
## Thu 17 Sep · PM (A11 review)
Blocks done: none new — independent correctness review of A11 (requested:
  "how correct has it been made?"). Did not trust the A11 log entry at
  face value; re-derived each claim:
  - Read all 24 files (10 core:model + 14 core:designsystem) against
    §6.2/§6.3 and the actual migration CHECK constraints, not just the
    plan's illustrative snippets.
  - Independently computed the TextMuted contrast ratio (#5F5C56 on
    #F4F1EA): 5.9:1, clears WCAG AA's 4.5:1 — the fix is real, not just a
    darker-looking token.
  - Decoded both font-provider cert blobs and verified with `openssl x509
    -inform DER`: genuine certs (Android debug 2008-2035, Google Inc. prod
    2008-2036), not plausible-looking fabrication — this was the one claim
    in the A11 log explicitly flagged as "fails silently if wrong."
  - Independently ran `core:model:compileKotlin` + `core:designsystem:
    compileDebugKotlin` + `lintDebug` fresh; confirmed exactly 35
    pre-existing warnings with none referencing any A11 file, matching
    the log's claim exactly.
  - Found two real gaps the log didn't mention, both breaking the pattern
    the code itself establishes: `Money`'s init only rejected negative
    values (accepted 0, ignored its own unused MAX_MINOR constant) against
    the DB's `amount_minor > 0 and <= 999999999`; `Entry` never checked
    `entryDate >= 2000-01-01` despite mirroring every other CHECK on the
    same table. Fixed both, rebuilt clean (1437 tasks), committed.
Check: `./gradlew build` green after the fix.
Gates passed: none new
Blocked on: nothing
Next: A12 — core:database entities, DAOs, per-user DB file (`ledger-
  ${userId}.db`); core:network; core:datastore. Check: DAO tests green,
  `openYIds()` correct.
```

```
## Thu 17 Sep · PM (A12)
Blocks done: A12 — core:database, core:network, core:datastore.
  - core:database schema v1: EntryEntity/AccountEntity mirror
    0002/0003_*.sql exactly, no userId column on either (isolation is the
    per-user database FILE, not a row filter — unlike the server's
    RLS-scoped tables). pending_ops/dead_letter deliberately deferred to
    B1's v2 migration, per §6.4's own "migration/ (from schema version 2
    onwards)" note — no point exporting a schema for a table nothing
    writes to yet. EntryDao/AccountDao: the four §6.4 queries verbatim,
    plus upsert/upsertAll (the latter's signature already implied by
    §6.5's addTransfer snippet).
  - LedgerDatabaseFactory: per-user `ledger-${userId}.db`. Didn't just
    assert the isolation claim — wrote a test that opens two real on-disk
    databases and confirms a row written under one userId is genuinely
    absent from the other's file, not just filtered.
  - 14 Robolectric DAO tests green, including openYIds' three load-bearing
    cases (opens on odd Y-id, closes on second leg, reopens if that leg is
    soft-deleted).
  - core:network: Supabase client (postgrest + auth) via Hilt, reading the
    real project URL/publishable key from local.properties (gitignored,
    fetched from the live project via the Supabase MCP tools rather than
    left as a placeholder). DTOs match the RPC JSON shapes from
    0006_rpc.sql exactly. No RPC-calling logic yet — that's core:data/B1.
  - core:datastore: two separate Proto DataStore instances (cursor updates
    every pull, prefs update rarely — one combined store would re-serialize
    prefs on every sync tick), both per-user for the same reason as the
    database. Deliberately does NOT hold the session/token — §7.3 requires
    EncryptedSharedPreferences for that, never plain DataStore, despite the
    architecture diagram's "DataStore (session, cursor, prefs)" line
    reading otherwise at a glance; wiring that is A13's job alongside
    sign-in.
  - Three real version-matrix problems, not typos, found by the build:
    Ktor's OkHttp engine pulls okhttp-android 5.x (needs compileSdk 37+,
    same AGP-9 wall as A10's Compose BOM) — forced to okhttp 4.12.0
    project-wide, since a per-module force in core:network alone didn't
    reach :app; Supabase-kt/Ktor ship Kotlin-2.4-metadata classes that
    Hilt 2.58's bundled kotlin-metadata-jvm (capped at 2.3.0) can't parse
    during classpath aggregation — forced kotlin-metadata-jvm to 2.4.0
    itself instead, since that artifact versions in lockstep with the
    Kotlin compiler and was a much smaller fix than downgrading
    Supabase-kt/Ktor; Robolectric 4.17 needs explicit --add-opens JVM
    flags on JDK 17+ or every test fails before running — added once in
    AndroidLibraryConventionPlugin so future modules don't need to repeat it.
Check: `./gradlew clean build --rerun-tasks` green — 1501/1501 tasks
  actually executed, zero cached. 14/14 DAO tests pass, openYIds() correct.
Gates passed: none new
Blocked on: nothing
Next: A13 — GCP Android OAuth client (needs SHA-1) · Credential Manager ->
  signInWithIdToken. Check: real phone, not emulator (P2 gate). This is
  where the session/EncryptedSharedPreferences piece deferred above
  actually gets built.
```

```
## Thu 17 Sep · PM (A13)
Blocks done: A13 — Credential Manager sign-in through to a real Supabase
  session. 🚪P2 PASSED.
  - Debug-keystore SHA-1 (9C:6E:63:...:35:54) + package name registered as
    an Android-type OAuth client in the same GCP project as A8's Web client
    (user did the console work). Worth remembering: the code takes the WEB
    client id in serverClientId, not the Android one — the Android client is
    what Play Services uses to verify the app's signature, the Web client is
    the audience GoTrue validates the token against. Both the web client id
    and the Supabase publishable key live in gitignored local.properties and
    reach the code as BuildConfig fields; nothing secret-shaped is committed.
  - feature:auth: SignInViewModel/SignInScreen implementing §7.3's state
    table exactly. Nonce per Supabase's documented contract — raw UUID
    SHA-256'd for setNonce, raw value handed to signInWith so the server can
    verify the hash. Got this direction confirmed from Supabase's own docs
    rather than guessing; inverted it would fail silently at runtime.
  - core:data: AuthRepository/SupabaseAuthRepository (first real content in
    that module). core:datastore: SessionStore on EncryptedSharedPreferences
    per §7.3's explicit "never in plain DataStore" — confirmed on-device that
    the shared_prefs file has no readable key names or values (AES256_SIV
    keys, AES256_GCM values, Tink keysets, master key in the Keystore).
  - Also applied §8's manifest hardening while in there: INTERNET +
    ACCESS_NETWORK_STATE only, usesCleartextTraffic=false,
    enableOnBackInvokedCallback, data_extraction_rules.xml (allowBackup=false
    alone doesn't cover device-to-device transfer).
  - One real fix, found by instrumenting rather than by review: the catch
    chain handled GetCredentialException's hierarchy but not
    GoogleIdTokenParsingException, which sits outside it and would have
    killed the coroutine with no state change and no log.
  - Process note worth keeping: I initially called this a BUG and was wrong.
    The first on-device run showed the success screen, I queried for a NEW
    profiles row, found none, and concluded no real sign-in had happened.
    Actually it had — handle_new_user is on-conflict-do-nothing and this
    Google account already had a profiles row from A8, so "no new row" was
    the correct outcome, not evidence of failure. The right check was "does a
    session exist for the expected user", not "was a row created". Cost a
    rebuild cycle; the logging it prompted was worth keeping anyway.
Check: 🚪P2 — real Samsung SM-G990E (Android 16, API 36), not an emulator.
  Native picker -> 1241-char Google ID token -> signInWith returned in 0.8s
  -> auth.users.last_sign_in_at 2026-09-17 17:49:56Z, matching logcat to the
  second. `clean build --rerun-tasks` green beforehand (1519/1519 executed).
Gates passed: P2 (sign-in on a real device)
Blocked on: nothing
Next: SPRINT A is complete except A14 (added after the fact — a scale/
  security review surfaced 4 fixes cheap now, expensive once B1's op log
  writes through this schema). Do A14 — plan §4.8, migration
  `0008_hardening.sql` — BEFORE starting B1. Then: B1 — ⚠️ read §5 of the
  plan in full before writing anything, then PendingOpEntity + OpLog. Ops
  must return in strict seq order.
```

```
## Tue 22 Sep · S1
Blocks done: A14 partially — wrote 0008_hardening.sql (all four §4.8 items:
  composite accounts_id_user_uidx + FK-with-ownership on entries.account_id,
  RLS select-wrapped + role-pinned, maintain_entry_count moved to a
  statement-level trigger, check_transfer_group scoped to user_id) and the
  Coral contrast fix in LedgerColors.kt (#E04848 -> #C62828, matches §6.3's
  own noted 4.98:1 fix). Could NOT run `supabase db reset` or the manual
  ownership-FK drill -- Docker Desktop crashed on startup with a corrupted
  `sailor-ingest.sock` reparse point under
  C:\Users\bibhu\AppData\Local\Docker\run\ that even `fsutil reparsepoint
  query` can't touch (error 1920) after a `wsl --shutdown`; this needs a
  machine reboot to clear, which wasn't taken without asking. Coral fix
  committed (self-contained, compiles clean); 0008_hardening.sql left
  UNCOMMITTED on disk, deliberately, until it can actually be verified --
  do not commit it blind next session, run the A6/A9-style manual drill
  first (insert as user A against user B's account_id -> must be rejected).
Also done same session: B1 through B7, all verified, all committed
  (commits "Sprint B: op log, coalescing, push/pull sync engine, WorkManager
  wiring (B1-B6)" and "B7: sync property test, 1000 iterations"). Per-block:
  - B1: schema v2 (PendingOpEntity/DeadLetterEntity, MIGRATION_1_2 + a real
    MigrationTestHelper test against actual v1 data, not just a clean
    `db reset` -- had to add `testOptions.unitTests.isIncludeAndroidResources
    = true` to core:database, since Robolectric doesn't merge test assets
    without it and MigrationTestHelper reads the prior schema JSON from
    there). OpLog.enqueue's contract note: CANCELLED (insert+delete) means
    the caller must hard-delete the local row, since there's nothing left to
    tombstone.
  - B2/B3: OpCoalescer as a pure decision function (Append/RewritePayload/
    Cancel/DropAndAppend), 8/8 unit tests mapping 1:1 to §5.2's table, plus
    an OpLog-level transfer-pair-atomicity test (two legs enqueued in one
    Room transaction land at adjacent seqs).
  - B4: Pusher. Found and fixed a real design gap the plan doesn't spell
    out: push_entries never accepts a client-supplied `id` (server always
    gen_random_uuid()s it), so a locally-created row's id and the eventual
    server id are different values -- without reconciling the local row's
    id to the server's after a successful push, the next pull would insert
    a SECOND, server-id-keyed copy of the same logical row. Added
    EntryDao/AccountDao.reconcileServerId, called from Pusher on every
    successful push. Also: push_entries is one atomic SQL statement, so a
    single invalid row 4xxs the WHOLE batch -- rather than dead-lettering
    innocent batch-mates, a ClientError on a >1-sized group retries one op
    at a time (recursing through the same code path) to isolate exactly
    which op is bad. 6/6 tests green: 250 ops -> 3 calls, mid-run 5xx leaves
    the rest queued, single 4xx dead-letters and never retries, one bad op
    in a batch is isolated without harming its batch-mates, 401-then-
    successful-retry, 401-that-survives-a-retry -> SessionExpired with the
    op still queued (nothing lost).
  - B5: Puller. Conflict rule implemented literally: a pending local op
    survives the pull (skipped, not overwritten) but the cursor still
    advances past it, so it isn't refetched forever. Cursor only advances
    after that page's Room transaction commits. 6/6 tests green including a
    551-row two-page pagination case.
  - B6: SyncScheduler's settle/ceiling worked out from first principles,
    since §5.6 only gives the constants, not the mechanism: the ceiling
    deadline is computed from the OLDEST pending op's `createdAt` (already
    on PendingOpEntity, no new state needed, and survives process death
    since it's DB-backed not in-memory), and each new op calls
    `enqueueUniqueWork(..., REPLACE, ...)` with `initialDelay =
    min(now+90s, oldest.createdAt+1h) - now`, which is what makes REPLACE
    correctly "restart the settle timer without pushing back the ceiling."
    BATCH_FULL=3 (declared but unused in the plan's own snippet) wired as
    "if >=3 ops already queued, skip the wait, sync now." Wired real
    WorkManager: added Configuration.Provider + HiltWorkerFactory to
    LedgerApplication, and the AndroidManifest provider override to disable
    WorkManager's default androidx.startup auto-init (which would construct
    SyncWorker with no DI). One real XML bug caught by the build: a manifest
    comment using "--" as a dash is illegal inside an XML comment and broke
    manifest merging -- not a Kotlin/Gradle issue at all.
  - B7: 1000-iteration property test, green on the first run, ~25s wall
    clock (Robolectric + in-memory Room per iteration, cheaper than
    expected). Random insert/update/delete sequences per synthetic entity
    against a FakeSyncApi simulating push_entries' real semantics (atomic
    per-call upsert-by-client_uid, deleted_at coalesce-never-undone, keyset
    pull ordering), with injected transient 5xx failures, asserting full
    convergence and zero leftover pending ops.
  Full `./gradlew build` (1528 tasks) green twice, both before and after B7.
Gates passed: none new (P3 needs B8's phone drill, still open)
Blocked on: B8 -- "add -> edit -> delete -> reconnect on a real phone,
  server has no row" needs an actual entry-adding UI to drive, and
  feature:entry is still an empty module (that's C1, not B-phase). The sync
  ENGINE side of this guarantee is what B1-B7 just verified headlessly
  (an unpushed op produces no server row, by construction and by the
  property test); the literal on-device version of this check can't run
  until C1 ships the entry form. Also still blocked on A14's DB-side
  verification (Docker, see above) and on Play Console identity
  verification / tester recruitment, per the "three things that can
  actually break this" section -- worth checking on both since it's now
  Tue 22 Sep and neither has been mentioned as started in this log.
Next: C1 -- addEntry/addTransfer, History PagingSource, grouping, day nets.
  Once addEntry exists, close the loop on B8 for real: real phone, airplane
  mode, add -> edit -> delete -> reconnect, confirm via Supabase MCP tools
  that no row exists until the reconnect push lands. Also: fix Docker
  (reboot needed) and run A14's actual verification before touching
  anything that writes through the hardened schema.
```

```
## Tue 22 Sep · S2
Blocks done: A14, fully closed out. Docker fix took real work: a reboot
  alone did NOT clear it (same crash recurred, byte-identical stack trace,
  same file, timestamps unchanged from before the reboot) -- these are
  AF_UNIX socket reparse points that Windows itself refuses to touch at
  all (even `Get-Acl` fails with error 1920, not just delete), and no
  Windows-side tool (fsutil, takeown, icacls, Remove-Item as admin) could
  reach them. Fix: delete them from the WSL Ubuntu side instead
  (`wsl -d Ubuntu -- rm /mnt/c/...`), where they're just ordinary files --
  that worked immediately. Then hit the SAME bug twice more, each time on
  a different stale socket one layer further into startup
  (`docker-secrets-engine/engine.sock` next, then `sailor-ingest.sock`
  again) -- turned out force-killing Docker's processes after a crash
  (`Stop-Process -Force`) leaves whatever socket THAT run had just created
  stale for the NEXT attempt, so every force-kill was manufacturing a new
  copy of the same problem. Fixed by clearing every file in both
  `Docker\run\` and `docker-secrets-engine\` via WSL in one pass, then
  letting the next launch run without interference. Docker's been stable
  since.
  With Docker up: `supabase db reset` applied 0001-0008 clean,
  `supabase test db --local` 7/7, then the actual A6/A9-style manual drill
  (not just trusting the SQL reads right) -- created two real auth.users
  rows, had user B create an account via RLS as themselves, then
  impersonated user A and tried to insert an entry with user_id = A but
  account_id pointing at B's account. Rejected with foreign_key_violation,
  zero rows created, exactly per §4.8 item 1's intent. Ran the whole drill
  inside one BEGIN/ROLLBACK so nothing was left in the DB afterward.
  Committed and pushed (0008_hardening.sql was sitting uncommitted since
  the previous session specifically to avoid committing unverified SQL --
  that discipline paid off, since it meant nothing had to be reverted).
Gates passed: none new -- A14 has no named 🚪 gate, it's a plan §4.8 item.
  P3 still needs B8's phone drill (unchanged, still blocked on C1).
Blocked on: nothing for A14. B8 still blocked on C1 (feature:entry doesn't
  exist yet) -- unchanged from last session, not touched this session per
  instruction ("leave B8 open, don't jump ahead to C1 just to close it").
Next: B9 -- ParseDateInputUseCase + DateParserTest, §7.5.1's rules, 45+
  cases. Pure Kotlin in core:domain, no dependency on A14 or B8. Every
  error message must be correct; `29.2` must find a real leap day; `31.4`
  must error, not clamp.
```

```
## Thu 24 Sep · S1
Blocks done: B9 -- ParseDateInputUseCase (core:domain, new module: only a
  build.gradle.kts existed before this) + DateParserTest, 48 cases (45+
  required). Implemented §7.5.1's table literally, rule by rule, in the
  table's own order. Two things the table doesn't spell out that the code
  had to resolve:
  - "Not a real date" (e.g. 31 April) has to be checked in two passes: a
    year-independent structural check (is day/month ever possible, using a
    fixed leap year -- 2000 -- as the reference so 29 Feb passes through),
    then a second check once the actual year is known, since 29 Feb is only
    real in SOME years. An explicit non-leap year for 29 Feb (`29.2.25`,
    `29.2.2023`) now correctly errors instead of silently resolving to
    something else.
  - The no-year "most recent past occurrence" branch needed the literal
    8-year search the plan calls for -- added a test pinned to 2103 (a year
    where the nearest 29 Feb is 2096, 7 years back, because 2100 isn't a
    leap year) to prove the window is actually wide enough, not just that a
    4-year gap works.
  Caught one real bug before it ever ran: the first draft of the
  non-digit-part check (`it.any(Char::isDigit).not()`) only flagged a part
  with ZERO digits, so a mixed part like "5a" would have silently passed.
  Fixed to `!it.all(Char::isDigit)` before writing a single test.
  Also caught by lint, not by me: `LocalDate.ofInstant(Instant, ZoneId)` is
  API 34+, above this project's minSdk 26 -- swapped for
  `instant.atZone(zoneId).toLocalDate()`, which has been available since 26.
  core:domain didn't depend on core:common yet (needed for LedgerClock, so
  "today" is injectable and every test is deterministic) -- added it.
  `./gradlew :core:domain:testDebugUnitTest` 48/48 green first run. Full
  `./gradlew build` (1544 tasks) green after the lint fix.
Gates passed: none new -- P4 (entry form complete) needs B10-B13 too.
Blocked on: nothing. Play Console identity verification and tester
  recruitment status still unconfirmed in this log as of today -- per the
  Tue 22 session's note, this needs a direct answer from you, not something
  visible in the repo.
Next: B10 -- DateField composable (core:ui or feature:entry) wired to this
  use case: input filter (digits + `. - / space` only, caret preserved on
  reject), Today/Yesterday quick buttons, hint-line states (inferred to
  last year / >90 days ago), full-rewrite on focus loss for a resolved
  date, unparseable text left exactly as typed. By-hand check per the plan:
  select-all-delete, mid-string edit, partial input preserved.
```

```
## Thu 24 Sep · S2
Blocks done: B10 -- DateField, in feature:entry (§6.7 puts field
  composables under feature/entry/component/, not core:designsystem --
  unlike AmountField, DateField genuinely needs domain awareness of the
  resolved DateFieldState for its hint line, so it doesn't fit the
  design system's "presentational only" rule). Split into three files on
  purpose, each independently testable:
  - DateFieldInputFilter.kt -- pure `(rawText, rawCaret) -> (filteredText,
    filteredCaret)`, no Compose dependency at all. "Caret put back where
    it was" is just "count allowed chars before the caret, in the FULL
    filtered text" -- one line, no special-casing needed for reject-at-end
    vs reject-mid-string, they're the same formula.
  - DateFieldHint.kt -- pure `(DateFieldState, today) -> DateFieldHint`,
    the exact wording from §7.5.1's hint-state table (placeholder /
    warning / error / blank-but-height-reserved).
  - DateField.kt -- the actual composable, wiring both of the above plus
    Today/Yesterday chips and the focus-loss rewrite (via onFocusChanged,
    guarded so it only fires on the focused->unfocused transition, and
    only rewrites when the field currently holds a Resolved date --
    unparseable text is left exactly as typed, per spec).
  The plan's own by-hand check for this block (select-all-delete,
  mid-string edit, partial input preserved) is now a 11-case automated
  suite (DateFieldInputFilterTest) instead of purely manual, since the
  filter turned out to be a pure function -- covers the three named cases
  plus reject-at-end, reject-mid-string, paste-of-mixed-garbage (which
  merges adjacent separators, e.g. "25/Sep/2026" -> "25//2026" -- caught
  a wrong hand-computed expectation in my own first draft of that test
  before running it), and an out-of-range caret. DateFieldHintTest adds
  7 more, including one that caught a wrong weekday (I'd copied the
  plan's illustrative "Mon 3 Mar 2026" literally; the real 3 Mar 2026 is
  a Tuesday -- fixed the TEST, not the code, since the code derives the
  weekday from DateTimeFormatter and was already correct).
  core:designsystem's existing AmountField (built in A11, formatting
  logic deliberately deferred to B11) was read first to confirm the
  presentational-vs-domain split before deciding DateField doesn't fit
  that mold.
  `./gradlew :feature:entry:testDebugUnitTest` 19/19 green. Full
  `./gradlew build` (1558 tasks) green, no lint errors.
Gates passed: none new -- P4 (entry form complete) still needs B11-B13.
Blocked on: nothing for the code. What's NOT done, and can't be from
  here: no emulator or physical device in this environment, so the
  on-screen/IME parts of DateField (does the caret actually behave right
  when a real keyboard is involved, does the focus-loss rewrite look
  right, do the chips read correctly at 2.0x font) are unverified --
  the pure-function tests prove the MATH is right, not that Android
  renders and drives it correctly. Flag this for a real-device pass once
  the app can actually run (C1 or later), same caveat as B8's phone
  drill.
  Also still unconfirmed in this log: Play Console identity verification
  and tester recruitment (carried over from Tue 22 and Thu 24 S1 -- now
  three sessions without an update either way).
Next: B11 -- FormatAmountUseCase + caret-mapping tests, then wiring
  AmountField's real VisualTransformation (currently a placeholder per
  A11's own comment). §7.5.8: max 7 integer digits refused at the
  keystroke, max 2 decimals, Indian digit grouping, caret preserved via
  a correct OffsetMapping both directions, "N lakh" readback ≥ ₹1,00,000.
```

```
## Thu 24 Sep · S3
Blocks done: B11 -- FormatAmountUseCase (core:domain) + AmountFormatterTest
  (32 cases) + the real AmountCard in feature:entry, wiring a Compose
  VisualTransformation to it.
  FormatAmountUseCase does two jobs, kept in one file since §10.1 tests
  them as one suite:
  - applyKeystroke: same character-filter-with-caret-recount technique as
    B10's date filter, plus two things dates didn't need -- refusing the
    WHOLE edit (revert to the previous value) when it would exceed 7
    integer digits or 2 decimals, rather than truncating after the fact;
    and the "0"+"5"->"5" leading-zero collapse, which shifts the caret
    left by exactly how many leading zeros it ate.
  - toDisplay/cleanToDisplayOffset/displayToCleanOffset: en-IN grouping
    (verified against the plan's own "12,34,567" example) and the two
    offset functions that make a correct `OffsetMapping` possible --
    implemented as "count non-comma characters up to here" in both
    directions, which turned out to need no special-casing for the
    decimal point at all (it's just another non-comma character).
  Lakh readback truncates rather than rounds (comment says so explicitly)
  so it never overstates the amount -- no spec guidance beyond the single
  "5 lakh" whole-number example, so this is a documented judgment call,
  not a literal spec requirement.
  Caught one real bug before running anything: first draft referenced
  `filtered.text` when the data class field is `filtered.clean` --
  compiler caught it immediately, not a logic bug, but worth noting since
  B9's bug was a logic mistake the compiler couldn't see and this one it
  could.
  AmountCard.kt (feature:entry) -- unlike DateField, this one instantiates
  its own `FormatAmountUseCase` via `remember` rather than taking a
  precomputed result from the caller, because the use case has zero
  dependencies (no clock, unlike ParseDateInputUseCase), so there's no
  testability cost to owning it locally. Wires the real
  OutlinedTextField(value: TextFieldValue, visualTransformation = ...)
  overload -- the String-only overload Compose also offers can't support
  this, since it never exposes the post-edit caret to onValueChange,
  which the reject-whole-edit and leading-zero-shift logic both need.
  Left core:designsystem's existing AmountField untouched (still the
  A11 placeholder) rather than repointing it at core:domain -- would
  have broken the presentational/domain boundary its own comment states.
  Direction buttons use LedgerSegmentedButton (already existed) with the
  real TxnDirection enum; transfer-mode's "both locked on" state (§7.5.3)
  can't be represented by a single-choice control, so it renders as a
  static label for now -- the actual dual-lock mechanics are B13's
  SelfTransferToggle, not this block's job.
  `./gradlew :core:domain:testDebugUnitTest` 32/32 (AmountFormatterTest)
  + 48/48 (DateParserTest) green. Full `./gradlew build` (1558 tasks)
  green, no lint errors.
Gates passed: none new -- P4 (entry form complete) still needs B12-B13.
Blocked on: nothing for the code. Same on-device caveat as B10: no
  emulator/phone here, so the VisualTransformation's actual on-screen
  caret behaviour (does it really not jump when Android's IME does its
  own thing with the Decimal keyboard) is unverified beyond the pure
  offset-math tests. Play Console verification and tester recruitment
  status: still no update in this log, now four sessions running.
Next: B12 -- AccountChips + the swap rule, TxnIdField (Y/N segmented +
  regenerate), TxnTypeCombo (the fixed list + Custom... free text,
  round-tripping an unrecognized stored type). All three are independent
  of each other and of B9-B11's work so far.
```

```
## Thu 24 Sep · S4
Blocks done: B12 -- AccountChips (+ swap rule), TxnIdField, TxnTypeCombo.
  All three followed the by-now-standard split: pure logic function(s) in
  their own file with their own test, composable wiring on top.
  - GenerateTxnIdUseCase (core:domain) -- takes a `Random` parameter
    (default `Random.Default`) instead of an injected wrapper, since a
    seeded `Random(n)` is already fully deterministic for tests; no need
    for a LedgerClock-style seam here. 7 tests.
  - resolveAccountSwap (feature:entry, pure, zero Compose/Android deps) --
    §7.5.4's swap rule turned out to be 4 lines once written as "which
    side changed, does the new value collide with the OTHER side's
    current value." AccountChips itself is one reusable row (used once
    for a plain picker, twice via TransferAccountChips for From/To),
    filtering out archived accounts, matching "generated from live
    accounts, never disabled."
  - buildYIdDropdownOptions (feature:entry, pure) -- fresh suggestion
    first, then every open Y-id, with the edit-mode "stored id not
    currently open gets inserted at the top" rule from §7.5.5. Caught a
    real bug before it shipped: my own first test asserted the dropdown
    for a BRAND-NEW Y entry (where the current id legitimately equals
    the freshly-generated suggestion, since nothing's been chosen yet)
    -- the function didn't guard against currentTxnId == freshId, so it
    inserted the same id twice. Added that guard; the "stored id not in
    the open list" case still gets its top-of-list slot correctly.
  - TxnTypeCombo -- deliberately has NO separate "is this custom" state.
    `isCustomTxnType(type) = type !in TXN_TYPE_OPTIONS` is a pure
    function of the single stored string, so §7.5.6's round-trip
    requirement ("a user-typed type must round-trip") falls out for
    free -- there's no flag that could drift out of sync with what's
    actually stored. Needed `@OptIn(ExperimentalMaterial3Api::class)`
    for ExposedDropdownMenuBox, and the newer
    `ExposedDropdownMenuAnchorType` (the `MenuAnchorType` typealias the
    IDE reaches for first is already deprecated in this BOM).
  Added `LedgerIcons.Regenerate` (Icons.Filled.Refresh) to
  core:designsystem -- the only icon addition this block needed.
  `./gradlew :core:domain:testDebugUnitTest :feature:entry:testDebugUnitTest`
  all green (7+6+5+4 = 22 new tests; 111 total across both modules
  counting B9-B11). Full `./gradlew build` (1558 tasks) green, no lint
  errors.
Gates passed: none new -- P4 (entry form complete) still needs B13.
Blocked on: nothing for the code. Same on-device caveat as B10/B11 --
  the dropdowns, chip row scrolling, and segmented buttons are unverified
  on an actual screen/IME. Play Console verification and tester
  recruitment: still no update in this log, five sessions running now --
  worth a direct answer before Sprint C's Fri 26 Sep deadline for
  starting verification gets any closer.
Next: B13 -- SelfTransferToggle's 9 state changes (§7.5.3) and
  ValidateEntryDraftUseCase (§7.5.9's validation order, transfer-shape-
  first). This is the block that actually WIRES B9-B12's components
  together into one coherent toggle-driven form state, so it's naturally
  bigger than B9-B12 individually -- closes out P4 (entry form complete)
  once done.
```

```
## Thu 24 Sep · S5  [session resumed after a usage-limit pause]
Blocks done: B13 -- EntryDraft (core:domain, new), ApplySelfTransferToggleUseCase
  (the 9 on / 5 off state changes from §7.5.3), ValidateEntryDraftUseCase
  (§7.5.9's validation order), plus the presentational SelfTransferToggle
  composable.
  EntryDraft is deliberately looser than core:model's `Entry`: dates can
  be null (unparsed), amount is B11's `AmountFieldValue` (can be empty or
  zero), because a draft can be mid-typing in ways a persisted row never
  can. It's the first thing built this session that ties B9-B12's
  separate pieces (AmountFieldValue, TxnId, TxnFrequency, TxnDirection)
  into one shape.
  ApplySelfTransferToggleUseCase treats the toggle as ONE atomic
  transition, not 9 separate field writes a caller could apply out of
  order -- and pushes every PURELY visual consequence (label text
  changing to "From account", which fields are visible, the Y/N segment
  dimming, the amount buttons rendering as locked) back onto the
  composables to derive straight from `draft.selfTransfer`, rather than
  duplicating UI state in the draft. Needed `accounts: List<Account>` as
  a parameter -- the "if From and To collide, silently move To" rule
  can't be decided from the two accounts already in the draft alone,
  it needs to know what else is available to move To to. Tested the
  edge case where only one account exists (a brand-new user): there's
  nowhere to move To, so the collision is left as-is rather than
  crashing -- documented as a real limit, not silently patched over.
  ValidateEntryDraftUseCase's tests spend more than half their count on
  ORDER, not just per-rule correctness -- four tests deliberately break
  two rules at once and assert which single message wins, including the
  exact scenario the plan's own prose calls out: an edit-mode shape
  mismatch that ALSO leaves the name blank (because turning transfer off
  clears it) must report the shape problem, not "Add an expense name".
  Scope note, deliberate: this block builds the LOGIC three components
  need to cooperate, not the full EntryScreen/EntryViewModel that wires
  all of B9-B13's components onto one screen with one save button --
  that's C1, which needs its own real estate (PagingSource, grouping,
  day nets are C1's other half per the plan) and shouldn't be rushed
  into the tail end of this one.
  `./gradlew :core:domain:testDebugUnitTest :feature:entry:testDebugUnitTest`
  green (16+13 = 29 new tests here; 140 total across both modules for
  B9-B13). Full `./gradlew build` (1558 tasks) green, no lint errors.
Gates passed: P4 (entry form complete) -- every field component and the
  validation/transfer-toggle logic B9-B13 set out to build now exists
  and is tested. The gate's own literal check ("entry form complete")
  is arguably not fully met until C1 assembles them into one working
  screen, but B-phase's own scope (per the plan's block list) ends here;
  flagging this distinction rather than silently declaring the gate done
  on a technicality.
Blocked on: nothing for the code. Unchanged from every session today:
  no device/emulator here to verify anything on screen, and Play Console
  verification / tester recruitment status is still unconfirmed in this
  log -- six sessions now without an update either way, and P4's own
  target date (Thu 24 Sep) is today.
Next: C1 -- addEntry/addTransfer, History PagingSource, grouping, day
  nets. This is where EntryScreen/EntryViewModel/EntryUiState/EntryAction
  actually get built and B9-B13's components get wired onto one screen
  for the first time. Once addEntry exists, B8's real-phone airplane-mode
  drill (add -> edit -> delete -> reconnect, confirm via Supabase MCP
  that no row exists until reconnect) becomes runnable -- it's been
  blocked on exactly this since Tue 22 Sep.
```

```
## Thu 24 Sep · S6  [continued after another usage-limit pause]
Blocks done: C1's DATA layer -- addEntry/addTransfer, History
  PagingSource, day-grouping with day nets. Deliberately scoped to the
  repository/domain layer this pass, not the full EntryScreen/
  EntryViewModel UI assembly -- see "Scope note" below.
  Found a real, previously-invisible gap while reading existing code
  before writing any: LedgerDatabaseFactory.kt's own doc comment says
  "the caller ... is responsible for closing the previous instance on
  sign-out / account switch" but nothing in the codebase was that
  caller -- SyncRepository only ever opens-and-closes its OWN short-lived
  connection per background sync, which is wrong for anything the UI
  needs to observe continuously (a PagingSource can't work against a
  connection that closes after one call). Added SessionDatabaseHolder
  (core:data) as the one place that opens a session's database and keeps
  it open, closing and reopening only if the signed-in user actually
  changes. This wasn't named as its own block anywhere in the plan, but
  every read-heavy C-phase feature from here on (History, Owed-to-me,
  Accounts, Settings) needs it, so it's built once now rather than
  invented differently by whichever later block first needed it.
  EntryRepository.addEntry/addTransfer: both write the entity AND
  enqueue its OpLog op inside ONE Room transaction, per OpLog's own
  documented contract (no window where a row exists locally without a
  pending op). addTransfer's two legs share that same transaction, so a
  crash mid-write can't leave a half-committed transfer sitting in the
  local database. Added Entry<->EntryEntity<->EntryPushRow mapper
  functions (EntryMappers.kt) -- the write DIRECTION (domain -> entity ->
  wire payload) didn't exist before this; only the pull direction
  (server DTO -> entity) did, because nothing had ever written a NEW
  entry locally until now.
  HistoryRepository.pagedEntries(): a new EntryDao.pagingSource() query
  (Room-generated PagingSource, added the room-paging + paging-runtime/
  common/compose/testing libraries -- none existed in the project before
  this) mapped through to domain `Entry`. The `pagingSourceFactory`
  lambda re-resolves the CURRENT session database on every Paging3
  invalidation rather than capturing one at Flow-creation time, which
  matters if the signed-in session ever changes under a long-lived
  collector -- documented as a known simplification (an in-flight
  session SWITCH mid-collection isn't specifically handled, since
  sign-out already tears down the screen per §7.16).
  GroupEntriesByDayUseCase (core:domain): folds a flat, pre-sorted entry
  list into day buckets with a day net, transfers excluded from that net
  ("moving your own money is not spending", the same rule the SQL-level
  `netMinorBetween` query and the month summary card both already
  apply). Relies on `groupBy` preserving first-seen key order rather than
  re-sorting, since the input is already newest-day-first, newest-
  entry-first from the paging query -- tested that reliance explicitly,
  not just the net arithmetic.
  Testability note, consistent with B-phase: both EntryRepository and
  HistoryRepository expose top-level `internal` functions
  (`writeEntry`/`writeTransfer`/`pagedEntries(databaseProvider)`) that
  tests call against a real in-memory Room database directly, bypassing
  SessionDatabaseHolder -- which needs a real signed-in session in
  SessionStore's EncryptedSharedPreferences (Android Keystore-backed)
  that isn't worth standing up under Robolectric just to prove a
  transaction is atomic or a query orders correctly.
  `./gradlew :core:domain:testDebugUnitTest :core:data:testDebugUnitTest`
  green (5+4+9 = 18 new tests here; no regressions in B1-B7's existing
  sync tests). Full `./gradlew build` (1558 tasks) green, no lint errors.
Gates passed: none new -- P5 (full CRUD) needs the actual UI screens
  this data layer feeds, which is still ahead.
Blocked on: nothing for the code. Same caveats as every session today:
  no device/emulator to verify on screen, and Play Console verification
  / tester recruitment status is STILL unconfirmed in this log -- seven
  sessions running now, and today (24 Sep) was P5's own original target
  date. This is now the most load-bearing open question in the whole
  plan and needs a direct answer, not another session passing without one.
Scope note, deliberate: C1's own line item ("addEntry / addTransfer ·
  History PagingSource, grouping, day nets") is fully built and tested
  at the data/domain layer. What's NOT built yet is the actual
  EntryScreen/EntryViewModel/EntryUiState/EntryAction that wires B9-B13's
  form components AND this session's repositories onto one real screen a
  user can tap through -- that's real remaining work, not a rounding
  error, and it's next.
Next: wire EntryScreen + EntryViewModel (feature:entry) -- the actual
  screen assembly: EntryUiState/EntryAction, hoisting DateField/
  AmountCard/AccountChips/TxnIdField/TxnTypeCombo/SelfTransferToggle onto
  one form, Save button calling ValidateEntryDraftUseCase then
  EntryRepository.addEntry/addTransfer. Then HistoryScreen/
  HistoryViewModel consuming HistoryRepository.pagedEntries() +
  GroupEntriesByDayUseCase via paging-compose's LazyPagingItems. Once
  addEntry is reachable from an actual screen, B8's real-phone
  airplane-mode drill finally becomes runnable.
```
[SUPERSEDED by the Thu 25 Sep session below -- `TxnIdField` was replaced
  before EntryScreen was ever wired to it, so this "Next" never pointed
  at code that shipped.]

```
## Thu 25 Sep · S1
ui-spec.md changed (owner-driven, not a code finding): entry detail's
  actions are now Delete/Edit split 50/50 with no bottom Close (only the
  existing top-right ✕ closes); Owed to me becomes one list split into
  Open/Closed cards instead of an open-only list; a Y-id with both legs
  gets an Open/Close toggle in the detail sheet. Bigger change: the raw
  Y/N `txn_id` field (B12's `TxnIdField`) is replaced by "Will I get this
  back?" Yes/No -> New/Old, and Frequency is dropped from the form
  entirely (settled 21 Sep per ui-spec.md, missed in this log until now).
  Instructed to fix the txn-id/frequency mismatch against the components
  B12/B13 already built, update the plan docs, and continue Sprint C as
  far as possible without Play Console verification or tester
  recruitment -- both are explicitly the owner's to handle from here,
  not to be asked about again; this log flags ONLY the point where
  either would actually gate the next step.
Blocks done: the rework, plus GroupOwedToMeUseCase (new, not a
  block previously named in the plan -- needed by both the Old picker
  and the Owed-to-me screen, so built once now rather than twice later):
  - EntryDraft (core:domain): dropped `frequency`. ApplySelfTransferToggleUseCase's
    two branches both set it to Monthly unconditionally already, so this
    was a no-op behaviourally -- confirmed by removing the two tests that
    asserted it and nothing else broke. Entry.txnFreq / the server column
    are untouched; every draft-to-Entry write still carries `Monthly`,
    just not from anything the user set.
  - OwedToMeItem + GroupOwedToMeUseCase (core:domain, new): folds live
    Y-prefixed entries into one row per txn_id -- isOpen (odd leg count,
    same rule as the old bare openYIds()), representative name/amount/
    account from the MORE RECENT leg once closed (matches the entry-
    detail sheet's own "opens the close leg by default" rule), openedDate/
    closedDate, and both legs' entry ids for the detail-sheet toggle.
    Handles a >2-leg group (not a shape the app itself produces, but
    tested anyway) by treating it as open, spanning earliest-to-latest,
    rather than crashing. 7 new tests.
  - EntryDao.liveYEntries() (core:database, new query) + HistoryRepository
    .owedToMeEntries() (new, same top-level-function-for-testing pattern
    as pagedEntries()) -- reactive, unpaged (Y-ids are a small fraction
    of the ledger). Left the old openYIds() query and its passing tests
    alone rather than repurposing it, since nothing conflicts.
  - TxnIdField.kt deleted outright, replaced by WillGetBackField.kt:
    Yes/No segmented control, then (Yes only) a New/Old segmented control,
    then (Old only) the picked item's name/amount/date with a dropdown to
    change it. Reuses GenerateTxnIdUseCase and the EXISTING, unchanged
    buildYIdDropdownOptions (its ordering rule -- fresh id first, then
    every open id, edit-mode current-but-closed id inserted at the top --
    turned out to need zero changes, since it only ever dealt in bare
    TxnIds and display was always going to be a separate concern).
  - TxnIdChoice.kt (new, feature:entry): buildTxnIdChoices wraps that
    same ordering with display data from OwedToMeItem, plus an
    editFallback for the one case with no OwedToMeItem to look up --
    editing an entry whose stored Y-id is no longer open. 4 new tests.
  `./gradlew :core:domain:testDebugUnitTest :core:data:testDebugUnitTest
  :feature:entry:testDebugUnitTest` green (11 new tests: 7+4). Full
  `./gradlew build` (1558 tasks) green, no lint errors, no warnings after
  fixing one "no cast needed" note in WillGetBackField.
  Plan docs updated to match: ledger-plan-v2.md §2 (Frequency note),
  §7.5.5 (renamed/rewritten as "Will I get this back?"), §7.5.7 (marked
  REMOVED), §7.8 (Open/Closed cards, GroupOwedToMeUseCase), §7.10 (50/50
  Delete/Edit, no bottom Close, the Open/Close toggle), §7.11 (one-line
  note that "this entry" means whichever leg the toggle selects).
Gates passed: none new.
Blocked on: nothing for this rework. Continuing directly into the rest
  of C1 per instruction, without pausing to ask about Play Console
  verification or testers again -- those are flagged only if/when they
  actually gate the next step, not before.
Next: EntryScreen + EntryViewModel (feature:entry) -- the real screen
  assembly this log has pointed at since Thu 24 S6, now against the
  CURRENT component set (WillGetBackField instead of TxnIdField, no
  frequency chips to hoist). Then HistoryScreen/HistoryViewModel.
```

```
## Thu 25 Sep · S2
Instructed to keep going through Sprint C without pausing to ask about
  Play Console verification or tester recruitment again -- both are the
  owner's to handle, flag only if/when either actually gates the next
  step (neither has, yet: everything built this session is code, no
  device or Play Console needed to write or test it).
Blocks done, in order:
  - C1's remaining half: EntryScreen + EntryViewModel (feature:entry,
    Add mode only -- edit mode is C4's own block). AccountRepository
    (new, core:data) for the account chips' reactive source, following
    EntryRepository's op-log-transaction pattern. ConfirmSentence.kt
    (pure, tested) for §7.5.10's confirm-sheet sentence. MainActivity
    wired to reach it after sign-in (temporary, same spirit as A13's own
    placeholder). No EntryViewModel test -- it orchestrates already-
    tested pure use cases and repository functions against Room/Hilt
    infra; EntryRepositoryTest's own comment already documents why that
    combination isn't worth standing up under Robolectric here. Full
    build green (1560 tasks).
  - C1's other stated half (day nets) was already done Thu 24 S6;
    HistoryScreen/HistoryViewModel is what actually renders it, done
    next in this session alongside C2/C3/C5 since they share one screen
    and one entry-detail sheet -- splitting them into separate diffs
    would have meant touching the same files three times.
  - C2: transfer folding. foldTransfers + HistoryRow (core:domain,
    tested, 6 cases) pairs two legs sharing a transferGroup into one
    row. Deliberately page-local -- it only sees whatever window it's
    handed (one day-group's entries in practice), so a leg split across
    a Paging3 page boundary falls back to two Single rows instead of
    folding. Documented as a known, rare simplification rather than
    solved with a custom RemoteMediator-style page-stitcher, since a
    transfer's two legs share entryDate and a near-identical createdAt
    (same save transaction), so they overwhelmingly land adjacent in the
    newest-first paging order and this almost never actually triggers.
    Month summary card needed two new EntryDao queries (outMinorBetween/
    inMinorBetween, same transfer-exclusion as the existing
    netMinorBetween) -- HistoryRepository.monthOutIn returns a raw
    Pair<Long,Long>, not core:domain's MonthSummary, because core:data
    doesn't depend on core:domain (§6.5/§6.6's own layering); the
    ViewModel wraps it. (First draft got this backwards -- built
    MonthSummary in core:domain and tried to construct it IN the
    repository, which doesn't compile; fixed by moving the wrap to the
    ViewModel, which is also more consistent with how GroupEntriesByDayUseCase
    already leaves grouping to the caller rather than the repository.)
  - C3 (list halves only -- reactive query + badge + entry detail sheet;
    "Y entry appears, its return removes it" per this block's own check
    is proven by C1 S6's property-style reasoning on the reactive query,
    not a new test here): Owed to me as one list split into Open/Closed
    LedgerCards (ui-spec.md's 24-25 Sep change), Closed at 55% alpha,
    badge counts Open only. Entry detail sheet: title + ✕ + optional
    Open/Close toggle (only when OwedToMeItem.closeLegEntryId != null)
    top row, full property list (Frequency dropped, matching §7.5.7),
    Delete/Edit 50/50 bottom row, no separate Close button. Toggling
    sides looks up the OTHER leg from HistoryViewModel's own already-
    loaded owedToMeEntries list rather than a second repository call --
    both legs of any Y-id are already in that list by construction.
  - C5: EntryRepository.deleteEntry -- soft-deletes (never hard-deletes
    a synced row, since the tombstone IS the delete per §5.2), cascades
    to every transferGroup sibling in one transaction, and hard-deletes
    only when OpLog reports CANCELLED (insert+delete, nothing to
    tombstone -- same rule B1 documented, now actually exercised by a
    delete path for the first time). New EntryDao.softDelete; reused
    the existing deleteByClientUid for the CANCELLED branch rather than
    writing a second hard-delete query. buildDeleteConfirmMessage
    (feature:history, tested, 3 cases) covers §7.11's three dialog
    variants. 4 new EntryRepository tests: synced/unsynced/transfer-
    cascade/missing-id.
  - Cross-cutting: Money.toCleanRupeeString() (core:model) pulled out of
    WillGetBackField's private formatMoney once HistoryScreen/
    EntryDetailSheet/DeleteConfirmMessage needed the same minor-units-
    to-display-string conversion a third and fourth time.
  - Caught and fixed before committing, not after: (1) a TxnId test
    fixture using "Y3k9d2" -- copied verbatim from the plan's own prose
    example, which is only 5 suffix chars, not TxnId's required 6, so
    the regex rejected it; the PLAN's illustrative id was never meant to
    be a literal, valid instance. (2) `Modifier.weight(1f)` inside
    MainActivity's temporary tab switch failed to compile with "Cannot
    access ... internal in file" -- an explicit
    `import androidx.compose.foundation.layout.weight` resolves to an
    unrelated INTERNAL `RowColumnParentData.weight` property of the same
    simple name, not the public ColumnScope/RowScope extension; removing
    the import (letting the implicit ColumnScope receiver resolve it,
    which needs no import at all) fixed it. (3) MonthSummary.kt shipped
    as genuinely dead code in the first pass of this session's commit --
    written, then routed around by the Pair<Long,Long> fix above, never
    reconnected. Caught by re-grepping for its own usages before writing
    this log entry, not by any tool; fixed by having HistoryUiState hold
    one MonthSummary instead of two raw Longs, which also deleted the
    redundant monthNetMinor computed property.
  `./gradlew build` green (1576 tasks) after every chunk above; the
  MonthSummary fix was folded into the same commit via --amend rather
  than left as a visible "oops" commit, since it was caught before
  anything downstream depended on the dead version.
Gates passed: none of the named 🚪 gates -- P5 ("full CRUD") needs C4
  (edit) too, which this session deliberately left as a stub
  (HistoryAction.EditTapped does nothing yet) rather than rushing into
  the same diff as everything above.
Known, documented gaps (see also the commit message):
  - Entry detail's Status line ("In sync" / "on this device" / "edited")
    needs a per-row pending-ops lookup that doesn't exist yet. Omitted
    from the property list rather than faked with a placeholder value.
  - §7.11's 5-second Undo toast after delete is NOT implemented --
    deleteEntry commits immediately, no undo window. This is a real gap
    against the plan's own text, not a simplification call; flagging it
    explicitly rather than letting "3 confirm variants" in C5's own
    checklist read as fully done.
  - Stats tab: not built, not scheduled anywhere -- see the note just
    added above Sprint C's own block table.
  - Same on-device caveats as every B-phase session: no emulator/phone
    here, so scrolling, the bottom sheet's real gesture behaviour, and
    the 48dp/2.0x-font checks are unverified beyond a clean Compose
    compile.
Blocked on: nothing code-related. Play Console verification and tester
  recruitment status: still the owner's to handle per this session's own
  instruction -- not re-asked here, but repeating the same unconfirmed-
  status note this log has carried since Tue 22 would be dishonest by
  omission, so it stays until an update actually lands one way or the
  other.
Next: C4 -- edit mode. Banner, frozen-original snapshot taken at the
  moment editing starts, the transfer-shape refusal message
  (ValidateEntryDraftUseCase already has this rule, unused until a
  caller sets editModeOriginalIsTransfer), editing both legs of a
  transfer together, Cancel resetting the form. Wires HistoryAction
  .EditTapped through to EntryScreen with a pre-filled draft -- the
  first real caller of WillGetBackField's editFallback parameter and
  buildTxnIdChoices' edit-mode branch, both built Thu 25 S1 but unused
  until now. Then C5's remaining piece (5s undo toast) and C6 (Accounts
  screen + first-run seeding -- also the natural home for a first Stats
  attempt, per the gap noted above, once accounts seeding actually
  exists to read balances from).
```

```
## Thu 25 Sep · S3
Instructed: "Continue into C4." Nothing else changed since S2 -- Play
  Console verification and tester recruitment still the owner's to
  handle, not re-raised here.
Blocks done: C4, in full per its own checklist (banner, frozen
  original, shape refusal, transfer both-leg):
  - EntryRepository.updateEntry/updateTransfer -- enqueue Op.Update
    against a row carrying the ORIGINAL id/clientUid/createdAt, never a
    second insert (@Upsert keys on id, so it overwrites in place).
    Entry.toEntity gained an updatedAt parameter (default: createdAt,
    so every existing ADD call site is unaffected) so an edited row
    keeps its original createdAt -- editing an old expense doesn't jump
    it to the top of today's day card in Recent. 4 new
    EntryRepositoryTest cases: overwrite-not-duplicate, createdAt
    preserved, transfer both-leg, transfer-group-required guard.
  - EditDraft.kt (feature:entry, pure, 6 tests): buildEditDraft turns
    the tapped leg -- already whichever side the Open/Close toggle
    selects, for a settled Y-id -- plus its resolved sibling (transfers
    only) into a starting EntryDraft and an EditingOriginal (Plain or
    Transfer) frozen at that moment. Caught one real bug before it
    shipped: the first draft used `sibling!!` after an `if (!isTransfer)
    entry else ...` check the compiler couldn't see through as a null
    guard on `sibling` specifically -- harmless (two unnecessary-
    assertion warnings, not a crash risk, since the branch WAS
    correctly gated), but rewritten as `if (sibling == null || ...)
    entry else sibling` so there's no assertion to warn about at all.
  - EditRequestStore (core:data, @Singleton) -- the hand-off between
    HistoryViewModel and EntryViewModel, two separate Hilt ViewModels
    with no navigation graph yet to pass an argument through (C9's
    job). EntryViewModel observes it reactively via the same 3-way
    combine() pattern as accounts/openItems, not just once at init --
    both ViewModels can outlive a single History-to-Edit round trip,
    so init-only would only work the FIRST time a user edited anything
    in a given app session.
  - EntryScreen: edit banner (txnId · date · amount + Cancel) above the
    form, "Save Changes" / "Save these changes?" copy swap, a one-shot
    Toast for "Edit cancelled — nothing changed" via a new
    SharedFlow<String> `events` property -- deliberately NOT part of
    EntryUiState, which would otherwise re-show the toast on every
    recomposition rather than once per Cancel tap.
  - HistoryViewModel.EditTapped: resolves the sibling leg for a
    transfer (fresh lookup via EntryRepository.transferLegs, since
    resolveToAccount's earlier lookup only kept the sibling's Account,
    not its full Entry), writes to EditRequestStore, and closes the
    detail sheet -- the form takes over from here. MainActivity/
    HistoryRoute: a plain callback wrapper switches the temporary tab
    to Add on the same tap, kept as a separate UI-only concern from the
    actual data hand-off.
  - ValidateEntryDraftUseCase's shape-refusal rule (built B13, dead
    code until now -- editModeOriginalIsTransfer was always null) is
    exercised for the first time by a real caller.
  `./gradlew build` green (1576 tasks) on the first full run after this
  block -- no compile-error/test-failure cycle this time, unlike S1/S2's
  TxnId-fixture and Modifier.weight mistakes.
  Plan doc updated: ledger-plan-v2.md §7.6 gained one implementation
  paragraph naming the four pieces above; the rest of §7.6's prose
  already matched what got built, so nothing else needed correcting.
Gates passed: P4 (entry form complete) and P5 (full CRUD) both marked
  done today -- Create/addEntry, Read/pagedEntries, Update/updateEntry,
  Delete/deleteEntry all exist and are tested. Marking these honestly:
  neither gate's own row in this table carries a ⚠️ real-device
  requirement (unlike P3/P6/P8, which explicitly do), so "done" here
  means code-complete-and-tested, the same bar P0-P2 were held to --
  it does NOT mean a phone has run through add/edit/delete/reconnect
  yet. That's still B8's job, still blocked on C1's UI existing long
  enough to actually drive it, which is no longer the blocker -- next
  session or a real-device pass could close B8 now if one is available.
Known gaps, unchanged from S2's list except where noted:
  - §7.11's 5-second Undo toast after delete: still not implemented.
  - Entry detail's Status line: still omitted (needs a per-row pending-
    ops lookup that doesn't exist yet).
  - Stats tab: still not built, still not scheduled anywhere -- still
    flagged, not resolved.
  - Same on-device caveats as every prior session: no emulator/phone
    here.
Blocked on: nothing code-related. Play Console verification and tester
  recruitment: still the owner's to handle, still unconfirmed in this
  log -- not re-asked, carried forward per instruction.
Next: C6 -- Accounts screen, first-run seeding (Domestic/International/
  Cash), archived handling. Also the natural first home for a Stats
  attempt (flagged above Sprint C's block table) once seeding exists to
  read balances from. Then C7 (Settings) and C8 (export/import), or B8's
  real-phone drill if a device becomes available before then.
```

```
## Thu 25 Sep · S4
Instructed: "Continue into C6." Nothing else changed -- same standing
  instruction on Play Console/testers as every session today.
Blocks done: C6, per its own checklist (Accounts screen, first-run
  seeding, archived handling, rename collision refused readably):
  - AccountDao gained countLive/countByNameExcluding/rename/setArchived
    /updateSortOrder. AccountRepository gained renameAccount/setArchived
    /reorder/allAccounts/ensureSeeded, each write sharing one Room
    transaction with its Op enqueue, same shape as every entry write
    path this whole session has followed. 12 new AccountDao/
    AccountRepository tests.
  - validateAccountName (core:domain, pure, 8 tests): the same
    "checked before the repository ever runs" pattern as
    ValidateEntryDraftUseCase and buildDeleteConfirmMessage --
    §7.12's 1-40-char + case-insensitive-collision rule.
  - ensureSeeded (§2's first-run seeding) is its own double-seed guard:
    check-then-insert inside ONE transaction, so calling it from BOTH
    AccountsViewModel.init and EntryViewModel.init (added to the
    latter this session) is safe rather than a race. Needed from both:
    Add Payment is the app's default tab, so seeding only when the
    Accounts tab loads would leave a brand-new user stuck on "Add an
    account first" unless they detour there first -- a bad first
    impression for a screen ui-spec.md and the plan both treat as the
    app's home.
  - New feature:accounts module (was an empty scaffold, same starting
    point as feature:entry/history were): AccountsScreen/
    AccountsViewModel/AccountsUiState/AccountsAction. Inline rename,
    archive/unarchive (55% alpha, matching Owed to me's Closed-card
    convention from S2), an add-account row, reorder via
    LedgerIcons.MoveUp/MoveDown (new, material-icons-core has no
    Archive/Unarchive icon so those actions are plain TextButtons
    instead of adding an extended-icons dependency for two icons).
  - Real gap found and flagged, not silently patched over: §7.12's own
    prose never describes an "add a new account" action -- only
    rename/archive/unarchive of accounts that ALREADY exist. Taken
    literally, the three seeded accounts would be a permanent ceiling,
    which directly contradicts §3's "N accounts, generalized" premise
    and its own explicit "4+ accounts" tile-collapse rule (there is
    nothing to collapse without a 4th ever being possible). Built the
    add-account row anyway rather than leaving the app unable to do
    something its own spec elsewhere assumes it can do; documented in
    ledger-plan-v2.md as filling an apparent gap, not as a deviation
    from anyone's stated intent -- same treatment as the Stats-tab gap
    from S2/S3.
  - Drag-to-reorder shipped as explicit up/down buttons, not a drag
    gesture. This project has no reorderable-list dependency, and a
    correct drag gesture (offset tracking, animated swaps, a drag
    handle) is real, separable work, not a one-line addition -- the
    persisted result (sortOrder) is identical either way. Documented
    as a follow-up in ledger-plan-v2.md, not a silently narrowed
    requirement.
  `./gradlew build` green (1584 tasks) on the first full run.
  ledger-plan-v2.md §7.12 updated with the gap note and an
  implementation paragraph.
Gates passed: none newly named -- C6 has no 🚪 gate of its own in this
  table.
Known gaps, unchanged from S3's list:
  - §7.11's 5-second Undo toast: still not implemented.
  - Entry detail's Status line: still omitted.
  - Stats tab: still not built. C6 was flagged as its natural home
    (account balances now genuinely exist to read), but "Continue into
    C6" was read literally as C6's own checklist, not an invitation to
    fold in a whole separate screen unasked -- raised again below
    rather than assumed.
  - Same on-device caveats as every prior session: no emulator/phone
    here, so the up/down reorder buttons, the inline rename field, and
    archived-row dimming are unverified beyond a clean Compose compile.
Blocked on: nothing code-related. Play Console verification and tester
  recruitment: still the owner's to handle, still unconfirmed in this
  log.
Next: C7 (Settings) or C8 (export/import) are the remaining Sprint C
  blocks with no unresolved dependency on anything not yet built.
  Stats (still unscheduled anywhere, see the note above Sprint C's
  block table) is also buildable now that C6 gives it real balances to
  read, if that's preferred over strictly following the C-numbering.
```

```
## Thu 25 Sep · S5
Instructed: "Continue into C7." Same standing instruction on Play
  Console/testers as every session today.
Most important thing this session, found before any Settings-screen
  UI code was written: SyncScheduler -- the trigger machinery B6 built
  back on Tue 22 Sep, with its own six named trigger points -- has
  NEVER been called from anywhere in the app. EntryRepository and
  AccountRepository enqueue pending ops correctly (every test since C1
  proves that), but nothing downstream of "op enqueued" ever told
  WorkManager to actually run SyncWorker. Checked by grepping for
  every call site of onColdStart/onForegrounded/onOpEnqueued/
  onManualSyncRequested/onSessionRestored across the whole repo:
  SyncScheduler.kt and SyncWorker.kt were the only hits. In concrete
  terms -- every entry, transfer, edit, delete, account rename/archive
  built C1 through C6 has been writing correctly to the LOCAL database
  and queuing a correct pending op, and would have sat in that queue
  forever, never reaching Supabase, in the actual running app. The
  1,000-iteration property test (B7) didn't catch this because it
  drives OpLog/Pusher/Puller directly, bypassing SyncScheduler
  entirely by design -- it was never testing "does a write trigger a
  sync," only "if a sync runs, does it converge." Worth being direct
  about the implication: if this had gone uncaught into B8's real-
  phone airplane-mode drill (once a device is available), the drill
  would have failed -- not because sync is broken, but because sync
  was never being ASKED to run.
Blocks done:
  - The fix: EntryRepository/AccountRepository's every write method
    now calls SyncScheduler.onOpEnqueued(currentSyncMode) right after
    its own transaction commits (never inside withTransaction --
    WorkManager/DataStore I/O has no business blocking a Room
    transaction). deleteEntry and ensureSeeded only notify when they
    actually queued/seeded something (both already tracked that as a
    Boolean for other reasons -- CANCELLED-vs-QUEUED, already-seeded-
    vs-fresh -- so this was a one-line addition each, not new
    plumbing).
  - UserPrefsRepository (new): reads/writes the UserPrefs proto
    DataStore that already existed (sync_mode) but was never actually
    read by anything either -- SyncScheduler.onOpEnqueued always took
    an explicit mode PARAMETER, and nothing ever supplied one from the
    stored preference. Added two new proto fields for §7.13's Security
    toggles while in there, named so proto3's zero-value default
    matches the SPEC's stated default rather than fighting it
    (show_amounts_in_app_switcher, not hide_amounts -- "hide, default
    on" is show=false, its own unset value). SyncMode proto<->domain
    mapping made internal specifically so it could be unit tested
    without standing up a real DataStore file.
  - SyncStatusRepository (new): queue count, dead-letter count, send-
    now -- the reactive reads §7.13's Sync section needs.
  - AuthRepository.currentEmail() -- added because SessionStore
    deliberately never persisted email (§7.3's own design), so
    "signed-in Google address" in Settings had literally nowhere to
    read from otherwise. Small, justified interface addition, not
    scope creep -- implemented in SupabaseAuthRepository, and the
    existing FakeAuthRepository test double (used by Sprint B's sync
    tests) needed the same override added to keep compiling, caught
    immediately by a full build rather than left for later.
  - New feature:settings module (was an empty scaffold): all six
    §7.13 sections rendered. Sync (mode selector + queue summary +
    send now), Accounts (nav link), Security (two toggles -- "hide
    amounts" is the one actually ENFORCED, FLAG_SECURE applied
    lifecycle-scoped in MainActivity via repeatOnLifecycle; "require
    biometric" persists but gates nothing, since the gate is Boot-
    routing territory and C9 hasn't built Boot routing), About (live
    version via PackageManager -- no cross-module BuildConfig
    juggling needed), Account (email, sign-out with the real §7.16
    guard dialog, honest pending count, "Send them first" / "Sign out
    anyway").
  - Sections explicitly NOT built, shown as disabled/informational
    rather than silently missing: Backup (Save/Restore -- C8's job),
    Delete account (C11's job, 7 steps + a public page that don't
    exist), the biometric GATE itself (C9's job), Privacy/Terms/
    Licence links (C13's job, no URLs exist yet), "last sync time" (no
    field tracks it -- flagged as a real small gap, not folded in as
    an afterthought to avoid a rushed proto-schema addition).
  `./gradlew build` green (1592 tasks) after one fix: the pre-existing
  FakeAuthRepository test double didn't implement the new
  AuthRepository.currentEmail() method, caught immediately by the
  first full build, not left to a later surprise.
  ledger-plan-v2.md §7.13 updated with the gap-found-and-fixed
  paragraph and an implementation paragraph.
Gates passed: none newly named -- C7 has no 🚪 gate of its own. P3
  (property test + airplane cycle) stays ⬜, unaffected by this fix
  either way -- B7's own property test never depended on the trigger
  wiring, and B8 is still blocked on a real device, not on code.
Known gaps, unchanged from S4's list except where noted above (backup,
  delete account, biometric gate, links, last-sync-time all now
  explicitly itemized rather than one line each):
  - Stats tab: still not built, still not scheduled anywhere.
  - Entry detail's Status line, §7.11's 5s undo: still not implemented.
  - Same on-device caveats as every prior session.
Blocked on: nothing code-related. Play Console verification and tester
  recruitment: still the owner's to handle, still unconfirmed in this
  log.
Next: C8 (export/import via SAF) is the remaining Sprint C block with
  no unresolved dependency. Stats remains buildable whenever preferred
  over strict C-numbering. onColdStart/onForegrounded/onSessionRestored
  (this session's fix covers only trigger 3) still need real wiring --
  natural home is C9's boot/lifecycle work, noted in ledger-plan-v2.md
  §7.13 rather than attempted here.
```

```
## Thu 25 Sep · S6
Instructed: "Continue into C8." Same standing instruction on Play
  Console/testers.
Blocks done: C8, per its own checklist (export/import via SAF, userId
  mismatch rejection, nightly nudge):
  - BackupRepository (new): exportTo(uri)/importFrom(uri) own the SAF
    file I/O; the Composable (feature:settings) owns the picker itself
    (CreateDocument/OpenDocument ActivityResultContracts) and hands
    over only a Uri -- same "UI picks, repository acts" split every
    write path this sprint has used. buildExportData/applyImport are
    top-level functions tested against a real in-memory Room database,
    not through SAF at all, matching this session's own established
    pattern. BackupRepositoryTest proves P6's own literal check two
    ways: export -> close the db -> open a FRESH in-memory db (a wipe,
    same end state as uninstall/reinstall) -> import -> the re-export
    matches the original by clientUid set; and importing the identical
    export twice adds zero rows the second time.
  - Real bug caught before running anything, not after: the first
    draft of applyImport's op-enqueue used THIS FILE's own
    ExportedEntry/ExportedAccount JSON as the pending op's payload,
    not the actual wire shape (EntryPushRow/AccountPushRow via the
    existing toPushRow() mappers). Had this shipped, every imported
    row's eventual push to Supabase would have sent a payload
    push_entries doesn't recognize -- a 4xx dead-letter for every
    single imported row, the exact failure mode §5.3's dead-letter
    path exists to catch, just self-inflicted instead of server-side.
    Caught by re-reading the diff against how every OTHER write path
    in the app builds its payload before writing a single test.
  - shouldShowNightlyNudge (core:domain, pure, 8 tests): §7.14's
    window/cutoff/snooze rules, wall-clock LOCAL time (not UTC) since
    22:00 is a local-evening concept, not a UTC one. The "last 22:00
    cutoff" needed its own reasoning: at 8am the relevant cutoff is
    LAST NIGHT's 22:00 (a day earlier), not today's, which hasn't
    happened yet -- tested explicitly, not just asserted in a comment.
  - UserPrefs gained two more fields (last_backup_at_epoch_millis,
    nudge_snoozed_until_epoch_millis, both 0-means-unset by the same
    proto3-default reasoning C7's two boolean fields used).
  - Scope cut, flagged in ledger-plan-v2.md rather than left implicit:
    the nudge ships as an IN-APP banner inside Settings only. No
    Android system notification, no POST_NOTIFICATIONS request, no
    WorkManager job firing it at 22:00 with the app closed -- the
    plan's own "WorkManager can now actually fire this at 22:00"
    sentence is still an open promise. The banner is correct and
    tested for exactly the case it covers (Settings happens to be on
    screen during the window); it does not cover "outside the app,"
    which is most of what a NIGHTLY reminder is actually for.
  - New EntryDao/AccountDao.allLive() (one-shot, live rows only --
    tombstones excluded, since re-importing after a reinstall
    shouldn't resurrect something that was deliberately deleted) and
    EntryDao.hasAnyLiveEntries() (reactive, the nudge's own
    precondition). 4 new DAO tests.
  `./gradlew build` green (1592 tasks) after the payload-shape fix
  above, caught and corrected before the first test run, not after a
  failure.
  ledger-plan-v2.md §7.14 updated: an implementation paragraph plus the
  scope-cut paragraph naming exactly what the nudge does and doesn't
  do yet.
Gates passed: none marked done in the table. P6 (export/import round
  trip) carries its own ⚠️ real-device requirement in this table's own
  text ("Export -> uninstall -> reinstall -> import") -- unlike P4/P5,
  which this log marked done Thu 25 S3 because their own rows carry no
  such marker. The LOGIC P6 actually gates is now proven (see
  BackupRepositoryTest above, which is the same round trip in
  substance: write -> fresh database -> import -> compare), but "close
  the app and open a fresh in-memory db" is not the same claim as "a
  real phone survives an actual uninstall," so this stays ⬜ until that
  runs for real. Distinguishing these honestly, not rounding up.
Known gaps, unchanged from S5's list except where updated above (nudge
  now itemized: banner works, notification doesn't exist):
  - Stats tab: still not built, still not scheduled anywhere.
  - Entry detail's Status line, §7.11's 5s undo: still not implemented.
  - Same on-device caveats as every prior session.
Blocked on: nothing code-related. Play Console verification and tester
  recruitment: still the owner's to handle, still unconfirmed in this
  log.
Next: every Sprint C block except C9 (Onboarding + boot routing) and
  C10/C11 (SignIn states, delete account) is now done or in the
  documented-gap state above. Reasonable next moves, in no forced
  order: C9 (the real 3-tab nav + boot routing this whole session's
  MainActivity has been standing in for), Stats (unscheduled, flagged
  since S2, buildable since C6), or the nightly nudge's still-missing
  notification half now that C8's banner/logic half exists to build it
  on top of.
```

```
## Thu 25 Sep · S7
Instructed: "Continue into C9." Same standing instruction on Play
  Console/testers.
Blocks done: C9, per its own checklist (Onboarding 3 panes with the
  pane-2 export button, versioned consent, Boot routing all 6 rows) --
  plus the real NavHost/tab plumbing every prior session's "Next" note
  called out as bundled with C9, and a slice of C10's own territory
  (Restoring, SessionExpired) that boot routing's own rows structurally
  require to exist before they can be routed to:
  - `decideBootRoute` (core:data, pure, 9 tests) is §7.1's six-row
    table as one function -- `BootRepository` gathers the impure inputs
    (SessionStore, a live `refreshSession()` attempt, a new
    `ConnectivityChecker` using `NET_CAPABILITY_VALIDATED` rather than
    "any radio is up", a new `AppConfigRepository` reading
    `app_config.min_supported_build`, `EntryDao.hasAnyLiveEntries()`)
    and calls it. Forced-update and "has local data" both fail closed
    to "don't know, proceed" on any error -- a boot-time bug in one
    check must not lock every user out.
  - `LedgerNavHost` (app module) is the first real NavHost in this
    repo -- `core:navigation`'s `LedgerDestinations` (plain route
    strings, no NavType args) was sitting unused since its own
    scaffolding; every screen still takes plain callbacks
    (`onFinished`, `onSignedIn`, ...), matching every existing
    `XxxRoute` composable, so the NavController itself never leaks
    into a feature module.
  - `feature:onboarding` (previously an empty scaffold) got its first
    source: three panes (What it does / The Google dependency + a
    disabled, explained export button / versioned consent), Next/Skip
    on panes 1-2, no skip on pane 3. Consent is recorded locally
    first (`ConsentStore`, a new device-scoped -- not per-user --
    proto DataStore, since Onboarding runs before any session exists
    to key a per-user file on) and synced to
    `profiles.consent_version`/`consent_at` only after sign-in
    succeeds (`ProfileRepository.syncConsentToServerIfNeeded`,
    best-effort, retried on every later sign-in until it lands).
  - `RestoringRoute`/`ForcedUpdateScreen` (app module, new): the
    former calls a new `SyncRepository.restore(onProgress)` --
    pull-only, no push, since an empty local DB (its own precondition)
    has nothing pending to send -- with `Puller.pull` now threading a
    running row count out for §7.4's "determinate progress." Failure
    offers Retry or "Continue offline" (lands on Home, same as any
    other offline boot).
  - `BiometricGate` (app module, new): C7's own note said "Require
    biometric to open" persists but gates nothing, "the gate belongs
    in §7.1's Boot routing, which is C9's job" -- it does now, wrapping
    Restoring/Home. Fails OPEN (skips the gate) whenever
    `BiometricManager.canAuthenticate()` isn't `BIOMETRIC_SUCCESS`
    despite the toggle being on (e.g. every fingerprint removed after
    enabling it) -- an optional app lock permanently stranding someone
    out of their own ledger would be a worse failure than the lock
    doing nothing. `MainActivity` is now a `FragmentActivity`
    (`BiometricPrompt` needs one; strict widening of `ComponentActivity`,
    no behavior change for anything already depending on the narrower
    type). `USE_BIOMETRIC` added to the manifest, deferred-permission
    comment updated to say why it's no longer deferred.
  - Trigger 6 (`SyncScheduler.onSessionRestored`), C7's other flagged
    gap, is wired: called after every successful sign-in, fresh or
    recovery, rather than threading a "was this a recovery" flag
    through just to skip a harmless enqueue on the fresh path. Trigger
    1 (`onColdStart`) is wired in `MainActivity.onCreate`. Trigger 2
    (`onForegrounded`) is deliberately NOT wired -- §5.5's own text
    makes ">60s since last pull" the caller's job, which needs a
    last-pull timestamp nothing tracks yet; firing on every foreground
    instead would be a real regression (sync spam), not a neutral
    simplification, so it stays an open, named gap rather than a fake
    fix.
  - Real bug caught before it could ship, not after: `SignInRoute`
    called `onSignedIn()` directly in the composable body on every
    recomposition where `uiState is Success` -- harmless at A13, when
    it only flipped a local boolean, but this session wires it to
    `navController.navigate(...)`, and an ungated call fires on every
    recomposition, not once. Fixed with `LaunchedEffect(uiState)`
    before it was ever wired into the graph, caught by reasoning about
    what changed under it, not by a crash.
  - `androidx.core:core-splashscreen` (1.0.1 and 1.2.0, both tried) is
    NOT used, despite being the obvious library for §7.1's "no UI
    beyond the system splash": its own `installSplashScreen()` failed
    to link under this project's Kotlin 2.3.20 / Build Tools API
    compiler path with "Unresolved reference," reproduced identically
    across both versions and confirmed via `javap` that the method is
    genuinely present in the AAR's bytecode -- androidx.biometric (pure
    Java) linked fine, so this looks like a real Kotlin-compiled-AAR
    interop gap in this specific toolchain combination, not a misuse.
    Worth retrying against a future AGP/Kotlin bump; not fought
    further here. `MainActivity` instead holds the launch window's own
    background (`Theme.App.Starting`, a plain color, no library) via a
    `ViewTreeObserver.OnPreDrawListener` -- the same technique the
    compat library uses internally -- gated on `BootViewModel.route`,
    which works identically on every API level back to minSdk. No
    animated icon either way: this project has no launcher icon asset
    yet, unrelated to this block.
  Interpretive decisions, flagged rather than left implicit:
  - §7.1's own table has exactly six rows and none of them is
    "onboarding already done, skip to sign-in" -- so `decideBootRoute`
    takes that literally: no stored session always means Onboarding,
    even right after a sign-out. What doesn't repeat is the CONSENT
    NOTICE itself: pane 3 only forces a fresh "Agree and continue" when
    `ConsentStore`'s locally-recorded version is behind
    `CURRENT_CONSENT_VERSION`, so a returning device still sees the two
    quick informational panes but doesn't re-read the same legal text
    it already agreed to.
  - Pane 2's export button is real UI, present per §7.2's own
    requirement ("offered here, not buried in settings"), but disabled
    with an explanatory line rather than wired to a no-op: exporting
    needs a signed-in per-user database, and Onboarding runs before any
    session exists in this architecture, so there is structurally
    nothing to export at this point in the flow. Same "disabled,
    explained, not hidden" pattern C7 used for Backup before C8 built
    it.
  - Pane 3's privacy-policy line is plain text, not a link: no policy
    URL exists yet anywhere in this project (Phase 9's own gate is
    "both policy URLs resolve"), and inventing one would be a broken
    link dressed as a real one.
  - Home stayed the four tabs A13 already had (Add/History/Accounts/
    Settings) behind the new NavHost, not ui-spec.md's three-tab
    (Add Payment/History/Stats) shape with Settings behind a gear icon
    -- Stats has no screen anywhere in this repo (unscheduled since S2)
    and building one was not part of C9's own checklist line; reshaping
    the tab bar around a screen that doesn't exist yet would be
    premature.
  `./gradlew build` green (1604 tasks, all tests including
  `DecideBootRouteTest`'s explicit "airplane mode + expired token"
  case, lint, assembleDebug and assembleRelease).
  ledger-plan-v2.md's §7.0/§7.1/§7.2 updated with what's actually built
  and what's still a named gap.
Gates passed: none marked done in the table, on the same honesty basis
  as P6 in S6's entry. C9's own gate ("Airplane + expired token still
  opens the ledger") is proven at the LOGIC level --
  `decideBootRoute`'s own test asserts exactly that scenario -- but
  "the pure function returns Home" is not the same claim as "a real
  phone in real airplane mode with a genuinely expired token opens the
  app," and this stays ⬜ until that runs on a device. Distinguishing
  these honestly, not rounding up.
Known gaps, unchanged from S6's list except where updated above:
  - Stats tab: still not built, still not scheduled anywhere. Home's
    tab bar still doesn't match ui-spec.md's three-tab shape as a
    direct consequence.
  - Entry detail's Status line, §7.11's 5s undo: still not implemented.
  - SyncScheduler trigger 2 (`onForegrounded`, ">60s since last pull"):
    still not wired -- needs a last-pull timestamp this session didn't
    add, named above rather than faked with an every-foreground call.
  - `androidx.core:core-splashscreen` couldn't be linked against this
    toolchain (see above) -- the app has a held launch background
    instead of the library's animated-icon exit transition. Revisit if
    a future AGP/Kotlin bump changes the outcome.
  - C10's remaining scope (SignIn's states were already built at A13;
    Restoring and SessionExpired are now built here) is really just
    "two-user DB isolation": sign in as A, then B, then A, and confirm
    A's data is intact. Not attempted this session.
  - Same on-device caveats as every prior session (this session added
    one: the airplane-mode boot gate above).
Blocked on: nothing code-related. Play Console verification and tester
  recruitment: still the owner's to handle, still unconfirmed in this
  log.
Next: C10 is now nearly done as a side effect of this session --
  "two-user DB isolation" is what's actually left under that name. C11
  (delete account) is the last Sprint C block untouched. Stats remains
  unscheduled and buildable whenever preferred over strict C-numbering.
  onForegrounded's ">60s since last pull" tracking is a real, small,
  named gap whenever sync polish is next in line.
```

```
## Thu 25 Sep · S8
Instructed: "Continue into C10." Same standing instruction on Play
  Console/testers.
Blocks done: C10, closing the one piece S7 left open (SignIn's 7
  states, Restoring and SessionExpired were already built at A13/C9):
  - `SessionDatabaseHolder.database()` (core:data) split into the
    public no-arg entry point and an `internal fun database(userId:
    String)` overload carrying the actual reopen/close decision --
    same reasoning `UserPrefsRepositoryMappingTest` already established
    for `toDomain()/toProto()`: `SessionStore` is
    `EncryptedSharedPreferences`, which needs a real Android Keystore
    Robolectric doesn't provide, so a test can't drive this through
    `sessionStore.read()` the way production code does. `SessionStore`
    itself gained one small change to make this possible at all:
    `prefs` is now `by lazy` instead of an eager property initializer,
    so a throwaway `SessionStore` can be constructed under Robolectric
    (to satisfy `SessionDatabaseHolder`'s constructor) without ever
    touching Keystore, as long as nothing actually calls
    `read()`/`write()`/`clear()` on it -- which this test's calls to
    the internal `userId` overload never do.
  - `SessionDatabaseHolderTest` (new, core:data, 3 tests) drives the
    literal gate directly: sign in as A (write an account), switch to
    B (write a different account, confirm A's is genuinely absent),
    switch back to A -- via a THIRD, freshly-reopened connection, not
    the cached first one (`assertNotSame`) -- and confirm A's row is
    exactly as written. Two smaller tests cover the cheaper claims the
    big one assumes: same userId reuses the cached connection, a
    userId change closes the old one and opens a new one.
  - `LedgerDatabaseFactoryTest` (core:database) gained the same claim
    one layer down, without `SessionDatabaseHolder` in the loop at
    all: open A, write, close; open B, close; reopen A; the row is
    still there. `SessionDatabaseHolderTest` proves the app's actual
    reopen-on-switch seam is correct; this proves the file-persistence
    guarantee that seam depends on -- a closed Room file genuinely
    survives on disk, not just "isn't visible from the other user's
    connection" (already covered by S-whatever's earlier test in the
    same file).
  `./gradlew build` green (1604 tasks) on the second run -- the first
  hit `Unexpected failure during lint analysis` on
  `BackupRepositoryTest.kt`, an untouched file; re-running `:core:data
  :lintDebug` alone succeeded immediately, confirming a flaky AGP/Lint
  K2-analyzer crash rather than anything this session's changes caused.
  Noted here rather than silently rerun-and-forgotten, since a build
  log entry claiming "green" should say when that took two tries.
Gates passed: C10's own -- "Sign in as A, then B, then A → A's data
  intact" -- **proven at the automated-test level**, both through the
  actual production reopen seam (`SessionDatabaseHolderTest`) and the
  underlying file guarantee it relies on (`LedgerDatabaseFactoryTest`).
  Unlike P6/C9's own gates, this table's row carries no explicit
  real-device marker (⚠️), and the claim itself -- two Room files on
  one device's filesystem don't interfere -- doesn't have an
  "only a real uninstall proves it" gap the way SAF export/import or
  airplane mode do, so this one is marked done on the automated proof
  alone.
Known gaps, unchanged from S7's list:
  - Stats tab: still not built, still not scheduled anywhere.
  - Entry detail's Status line, §7.11's 5s undo: still not implemented.
  - SyncScheduler trigger 2 (`onForegrounded`): still not wired, same
    reason as S7.
  - `androidx.core:core-splashscreen` still can't be linked against
    this toolchain (S7's note stands).
  - Same on-device caveats as every prior session -- S7's airplane-mode
    boot gate is still unverified on a real phone; nothing this session
    touches that.
Blocked on: nothing code-related. Play Console verification and tester
  recruitment: still the owner's to handle, still unconfirmed in this
  log.
Next: C11 (delete account, 7 steps + public web page) is the last
  untouched Sprint C block. Stats remains unscheduled and buildable
  whenever preferred over strict C-numbering. onForegrounded's ">60s
  since last pull" tracking is still open whenever sync polish is next.
```

```
## Thu 25 Sep · S9
Instructed: "Continue into C11." Same standing instruction on Play
  Console/testers.
Blocks done: C11, per its own checklist -- all 7 in-app steps plus the
  public web page:
  - `delete_my_account()` (0007_account_deletion.sql) and its
    `delete-account` edge function already existed from an earlier
    session with zero Android-side caller -- confirmed by survey before
    writing anything, so this session adds exactly what was missing
    rather than a second copy of server-side work already done. It's
    `SECURITY DEFINER`, trusts whatever `auth.uid()` the caller's JWT
    says, with no re-authentication check of its own -- meaning step
    4's Credential Manager prompt is a client-side UX gate ONLY, not
    something the server relies on.
  - `AuthRepository.deleteAccount()` (new) calls that RPC directly via
    `supabase.postgrest.rpc(...)`, matching every other write path in
    this codebase (push_entries, pull_entries) rather than going
    through the edge function's own HTTP endpoint -- the Android app
    already has the full Supabase Kotlin SDK, so there's no reason to
    round-trip through a wrapper built for browsers that don't.
  - Step 4's re-authentication deliberately does NOT reuse
    `signInWithGoogleIdToken` -- that exchanges the credential with
    Supabase and would silently overwrite the ambient session with
    whatever account Credential Manager returns, which is exactly
    wrong if the person picks a DIFFERENT account than the one being
    deleted. Instead `decodeJwtEmail` (new, `feature:settings`, pure,
    6 tests) reads the returned ID token's own `email` claim locally
    -- no network call, no session mutation -- and compares it against
    `AuthRepository.currentEmail()`. This matches exactly how much
    identity proof the server itself requires (none beyond the ambient
    JWT already in hand).
  - `AccountDeletionRepository` (new, core:data) is steps 5-6: call the
    RPC, then close `SessionDatabaseHolder`, `context.deleteDatabase()`
    the Room file, delete the two per-user DataStore files (new
    `LedgerDataStoreFactory.deleteUserData(userId)`), then
    `sessionStore.clear()` last and unconditionally -- so a partial
    local-wipe failure still leaves the device at "no stored session"
    on the next boot (§7.1 row 1 -> Onboarding) regardless of how far
    it got. Deliberately does NOT delete `consent_state.pb` -- that's
    device-scoped, and a later sign-in on the same device shouldn't be
    forced through the consent pane again over a DIFFERENT account
    being deleted from it.
  - `DeleteAccountScreen`/`DeleteAccountViewModel` (new, feature:settings)
    is its own screen, not a Settings dialog -- reached from
    AccountSection's now-real "Delete account" row via a plain
    callback (same shape as `onAccountsRequested`), which is why
    `SettingsAction.DeleteAccountTapped` (present since C7, always a
    no-op) is deleted rather than wired up. Step 2's backup nudge
    reuses `SignOutGuardDialog`'s exact shape (two escape paths: "Save
    a backup" / "Continue without backing up") rather than inventing a
    new dialog pattern. Step 3's "type DELETE" is the first
    typed-confirmation pattern in this codebase -- a plain `AlertDialog`
    + `OutlinedTextField`, matching every other destructive dialog
    here (`EntryDetailSheet`'s delete confirm, the sign-out guard) 
    rather than a bottom sheet (this codebase's bottom sheets are for
    data entry/detail, not confirmations). Step 7's "Confirmation
    screen" is this same screen's own `Deleted` state, not a toast --
    `LedgerNavHost` only navigates to Onboarding once the person taps
    Continue on it.
  - **Public web page** (§11.3's other requirement, "Play requires the
    web route for any app with accounts"): a new
    `supabase/functions/delete-account-page` edge function serves a
    self-contained HTML+JS page from the SAME Supabase project the app
    already uses -- `https://<project-ref>.supabase.co/functions/v1
    /delete-account-page` is a genuinely reachable public URL today,
    with no separate domain/hosting decision needed. It signs in with
    Google Identity Services' web SDK (the browser counterpart to
    Credential Manager), exchanges the credential via `supabase-js`'s
    `signInWithIdToken`, then POSTs to the SAME `delete-account`
    function the app calls -- one deletion implementation, two front
    doors. `verify_jwt = false` on this function in `config.toml`
    (added alongside `delete-account`'s own entry) since the entire
    point of a public URL is reachability with no Authorization header
    to begin with. **Not manually verified against a live browser --
    Docker Desktop's daemon isn't running in this environment
    (`docker info` confirms), so `supabase start`/`functions serve`
    wasn't attempted; starting a full local Postgres+GoTrue+Studio
    stack just to eyeball one page felt like the wrong tradeoff against
    session time.** The nonce scheme (raw UUID, SHA-256 hex hash given
    to Google, raw value given to Supabase) mirrors
    `SignInViewModel.signIn`'s already-proven logic exactly, which is
    the most confidence available without a live round trip.
  `./gradlew build` green (1614 tasks) after fixing one break the new
  `AuthRepository.deleteAccount()` method caused: `FakeAuthRepository`
  (core:data test double, used by the sync property test) stopped
  compiling until it implemented the new interface member too --
  caught by the build, not missed.
  ledger-plan-v2.md's §7.17/§11.3 updated with what's built and the
  page's own unverified status.
Gates passed: none of C11's own steps carry an explicit table marker,
  but the same honesty standard applies as every prior gate in this
  log: the in-app 7-step flow is real, compiled, and its one purely
  logical piece (`decodeJwtEmail`) is tested; the public web page is
  real code, deployable, but **unverified in an actual browser** --
  flagged rather than claimed working, same as P6/C9's own real-device
  gaps.
Known gaps, unchanged from S8's list except where updated above:
  - Stats tab: still not built, still not scheduled anywhere.
  - Entry detail's Status line, §7.11's 5s undo: still not implemented.
  - SyncScheduler trigger 2 (`onForegrounded`): still not wired.
  - `androidx.core:core-splashscreen` still can't be linked against
    this toolchain (S7's note stands).
  - The public web deletion page is unverified live (this session's
    own note above) -- needs an actual browser pass against a deployed
    (or `supabase start`-local, once Docker Desktop is running) copy
    before this is truly done, not just written.
  - Same on-device caveats as every prior session, plus: this session's
    delete-account flow (Credential Manager re-auth, the local wipe,
    the server RPC) has not been run on a real device either.
Blocked on: nothing code-related. Play Console verification and tester
  recruitment: still the owner's to handle, still unconfirmed in this
  log.
Next: C1 through C11 -- every FEATURE block in Sprint C's own table --
  now has a "done" entry in this log, Stats aside (unscheduled since
  S2, its own flagged open item, not a C-block). **Sprint C itself is
  not finished**: C12 (Keystore + Play App Signing enrolment + R8 +
  Sentry), C13 (privacy policy, ToS, the deletion URL going properly
  live, Play listing, the Data Safety form) and C14 (signed AAB ->
  internal track -> promote to closed, 🚪P8) are still untouched --
  packaging/release work, not app features, but still Sprint C's own
  table, not a later sprint. Worth doing before those: a manual device
  pass through C8's export/import round trip (P6), C9's airplane-mode
  boot gate, and C11's delete-account flow end to end plus the web
  page in an actual browser -- verifying what's already built costs
  less than discovering a gap during C14's Play submission.
```

```
## Thu 25 Sep · S10 — security review + start of C12
Instructed: after a spoken-language security review request, "fix
  those three [worth-fixing findings] then... what's left, we can go
  from C12 to C14, whatever we can do now."
Correction, stated plainly rather than buried: the review's own first
  pass wrongly flagged `check_transfer_group()` as missing its
  `user_id` scope -- 0008_hardening.sql had already fixed that. Caught
  by re-reading the actual current function body (via the Supabase
  MCP's own tools against the LIVE database) before writing a fix,
  not by re-trusting a manual grep of the migration files alone.
Blocks done: the security review's real findings, checked against the
  live project via `get_advisors`/`execute_sql`, not just the migration
  files on disk (the files describe INTENT; the live grants are what
  actually decide who can call what) --
  - `0009_function_hardening.sql` (written, NOT yet applied -- see
    "Blocked on"): pins `search_path` on the 7 functions that never
    got it (a real privilege-escalation vector on the SECURITY DEFINER
    ones, cheap hygiene on the rest); revokes EXECUTE on the 6 pure
    trigger functions from both `anon` and `authenticated` entirely
    (nothing outside the trigger mechanism should call these, and
    Postgres invokes triggers without checking EXECUTE on the trigger
    function -- this closes the surface, doesn't touch the triggers'
    own behavior); and revokes EXECUTE on `pull_entries`/
    `pull_accounts`/`push_entries`/`open_y_ids`/`delete_my_account`
    from `anon` specifically, re-granting to `authenticated` only.
  - **Real gap in an existing migration, found live:** 0007's own
    `revoke all on function public.delete_my_account() from public`
    does not do what it reads as -- confirmed via
    `has_function_privilege('anon', ..., 'EXECUTE')` returning `true`
    for it, right now, in production. `PUBLIC` is "every role" as a
    pseudo-role; Supabase's own project bootstrap grants EXECUTE to
    `anon`/`authenticated` DIRECTLY, not through PUBLIC, so revoking
    from PUBLIC never touched those grants. Not currently exploitable
    for account takeover -- `auth.uid()` is null under `anon`, so the
    function's own `not_authenticated` check already stops it -- but
    it's real, undisclosed surface directly contradicting what that
    migration's own comment says it accomplished, on every function in
    this project, not just this one.
  - `maintain_entry_count()`'s residual race: 0008 already narrowed it
    from a per-row to a per-statement trigger, which helps but doesn't
    close it -- the check-then-write was still two separate statements
    with no row lock between them, so two genuinely concurrent
    `push_entries` calls for the same user could both read the
    pre-update count and both squeak past the cap. Folded the cap
    check into the UPDATE's own WHERE clause instead, so the row lock
    the UPDATE itself takes is what serializes concurrent statements
    for the same user -- the decision and the write now happen in one
    locked statement, not two with a window between them.
  - R8, per §11.2 ("R8 full mode on"): `isMinifyEnabled = true`,
    `isShrinkResources = true` on the release build type (was `false`
    since A1). `app/proguard-rules.pro` (new) carries kotlinx
    .serialization's own documented keep rules (scoped to this app's
    own package plus `io.github.jan.supabase.**`, since supabase-kt's
    own consumer rules aren't confirmed to cover everything) plus
    defensive keeps for Credential Manager/Google Identity's runtime
    type resolution -- Room and Hilt ship their own consumer rules,
    nothing added for those. `mapping.txt` now generates at
    `app/build/outputs/mapping/release/` (§11.2's "uploaded to Sentry
    on every release" still needs C12's Sentry setup to actually send
    it anywhere). `:app:assembleRelease` succeeds -- ~130 repeated
    "R8: An error occurred when parsing kotlin metadata" warnings, the
    same Kotlin-2.3.20-vs-this-AGP's-bundled-R8 mismatch category as
    S7's core-splashscreen finding, not a new problem this introduced.
    R8's fallback when it can't read Kotlin metadata is conservative
    (treat the class as plain Java, not an aggressive strip), so this
    is very likely harmless, but **not verified on a real device** --
    flagged rather than assumed.
  `./gradlew build` green (1610 tasks) after the R8 change, confirming
  nothing else regressed.
Gates passed: none of this session's own fixes carry a table gate --
  this is hardening work, not a Sprint C block. The SQL fix is written,
  reviewed against the live database's actual current state, and ready,
  but explicitly NOT a "done" gate until it's actually applied (see
  below) -- a written-but-unapplied migration fixes nothing.
Known gaps, unchanged from S9's list, plus:
  - **`0009_function_hardening.sql` is not yet live.** Claude Code's
    own safety layer refused to apply it directly (classified as a
    "Production Deploy," correctly -- this is a real live database,
    not a sandbox), so the fix exists only as a file in this repo
    until the owner runs it themselves (`supabase db push`, or asks
    for it to be applied with explicit approval).
  - R8's Kotlin-metadata warnings are unverified on a real device --
    added to the pre-existing "not verified on a real device" list
    this log already carries for C8/C9/C11.
Blocked on: `0009_function_hardening.sql` needs the owner's explicit
  go-ahead to actually reach the live database -- this is not a
  code-review item, it's a live production permissions change, and
  the harness is correctly refusing to make that call unilaterally.
  Play Console verification and tester recruitment: still the owner's
  to handle -- as of this session, verification documents are
  uploaded and the Android-device step is done; phone-number
  verification is the one remaining step, blocked on Google's own
  review coming back first.
Next: C12's remaining pieces -- Keystore generation + Play App Signing
  enrolment (needs the owner: a credential only they should hold) and
  Sentry (needs the owner: creating the account/DSN is theirs, wiring
  the SDK once given a DSN is not) -- plus applying 0009 once approved.
  C13/C14 after that, per ledger-plan-v2.md §12's own phase order.
```

```
## Thu 25 Sep · S11 — 0009 applied, and it needed a follow-up fix
Instructed: "Yes, go ahead and push it."
Blocks done: `0009_function_hardening.sql` applied to the live
  database via the Supabase MCP tools, immediately re-verified against
  `pg_proc.proacl` rather than just trusting `success: true` --
  **and the re-check caught a real bug in 0009 itself**, found and
  fixed in the same session before calling this done:
  - `search_path` pinning (0009's item 1) landed correctly on all 7
    functions -- confirmed via `pg_proc.proconfig`.
  - The REVOKE statements (0009's item 2) landed correctly for exactly
    ONE function (`delete_my_account`) and silently did nothing for
    the other ten. `revoke execute ... from anon, authenticated`
    revokes a NAMED role's own grant -- but `anon` never held an
    individual grant on any of these ten functions to begin with; its
    access rode entirely on a separate `PUBLIC` grant (Postgres's own
    ACL model unions every applicable entry -- direct role, role
    membership, and PUBLIC -- so revoking a role that was never the
    actual grant source changes nothing). Confirmed by reading
    `pg_proc.proacl` directly: every one of the ten still carried a
    bare `=X/postgres` entry (`=` with no role name is PUBLIC) after
    0009 supposedly revoked it. This is the EXACT bug 0009 itself was
    written to fix in 0007's `delete_my_account` -- caught here
    because this session re-queried live state after applying the fix
    instead of stopping at "the migration succeeded."
  - `0010_revoke_public_execute.sql` (new): `revoke execute ...
    from public` (not `from anon`/`authenticated` by name) on the same
    ten functions. Re-verified after applying: all ten now show
    `anon_can_execute = false`; the four sync RPCs plus
    `delete_my_account` correctly keep `authenticated_can_execute =
    true` via their own separate direct grant (confirmed still present
    in `proacl`, untouched by revoking PUBLIC); the six pure trigger
    functions now have no execute grant for anyone but `postgres`/
    `service_role`, as intended.
  - Supabase's own security advisor re-run after both migrations:
    every `function_search_path_mutable` and
    `anon_security_definer_function_executable` finding is gone, and
    `authenticated_security_definer_function_executable` is down to
    exactly one -- `delete_my_account`, which is supposed to be
    callable by `authenticated` (that is the entire feature) and is
    correctly left alone rather than "fixed" into uselessness.
    `auth_leaked_password_protection` remains and is not applicable --
    this project has no password-based sign-in for HaveIBeenPwned
    checking to protect.
Gates passed: the security review's own findings are now genuinely
  live and verified, not just "migration applied" -- re-checked via
  `pg_proc.proacl`/`has_function_privilege` and the advisor a second
  time after the correction, not assumed from the first apply's
  success response.
Known gaps: unchanged from S10's list, minus the now-closed migration
  item. Everything else (Keystore/Play App Signing, Sentry, C13/C14,
  real-device verification of R8 and this session's own permission
  changes) still stands as S10 described it.
Blocked on: nothing from this fix. Same owner items as S10 (Play
  Console verification in progress, tester recruitment).
Next: same as S10 -- Keystore/Play App Signing and Sentry need the
  owner; C13/C14 follow.
```

```
## Sat 26 Sep · S12 — signing key, Sentry, public delete-account page
Instructed: "Let's set up the signing key", "Let's set up Sentry next",
  "let's do the Google Cloud Console setting next".
Blocks done:
  - Release signing: upload keystore generated BY THE OWNER at
    C:\Users\bibhu\keystores\ledger-upload-key.jks; passwords live only
    in gitignored keystore.properties. app/build.gradle.kts signs
    release builds when that file exists, stays unsigned otherwise.
    Verified with apksigner (CN=Bibhuti.Panda, OU=Ledger).
  - Sentry (C12): sentry-android 8.58.0 + Gradle plugin 6.23.0. DSN in
    local.properties -> BuildConfig.SENTRY_DSN; SentryAndroid.init in
    LedgerApplication with sendDefaultPii=false and a beforeSend that
    strips amount/expenseName/moreInfo (§11.5). No tracing. Org
    `ledger-1k4`, project `ledger`, US region.
  - Sentry mapping upload: org:ci auth token in gitignored
    sentry.properties (created by the owner). Hit a Windows bug (path
    with spaces) -- fixed via cli.executable, see KNOWN-ISSUES.md.
    Verified: clean assembleRelease uploads mapping.txt automatically.
  - KNOWN-ISSUES.md created (new, beside this file): environment and
    tooling errors with their fixes. Check it before re-diagnosing.
  - Public delete-account page: the Supabase Edge Function version
    CANNOT work (Supabase forces text/plain + sandbox CSP on HTML).
    Moved to new PUBLIC repo dumbopanda/Ledger-Pages (local:
    Ledger APK/ledger-pages), GitHub Pages from main /(root):
    https://dumbopanda.github.io/Ledger-Pages/delete-account/
    Redesigned to match the app (colours, Inter, cards, pill buttons,
    3-step progress, backup reminder, "Not you?"). Session kept in
    memory only. Google OAuth web client origins now:
    https://dumbopanda.github.io (supabase.co origin removed by owner).
    Old function's source + config.toml entry removed from app repo.
Gates passed: signed APK verified; release build + R8 + Sentry green;
  mapping upload confirmed; page live, served as text/html, Google
  button renders, no console errors, checked at phone width.
Known gaps:
  - Delete-account page NOT tested end to end (sign in + real delete).
    Needs the owner with a spare Google account.
  - Old `delete-account-page` function still deployed on Supabase --
    owner to delete in the dashboard (MCP can't delete functions).
  - No public contact email on the page yet.
  - Sentry not yet seen receiving a real event from a device.
  - Local commits in ledger-app not yet pushed (Ledger-Pages is pushed).
Next: owner deletes the old Supabase function and tests the page with a
  spare account; then Privacy Policy + Terms pages (same Ledger-Pages
  repo), Play listing + Data Safety form, internal testing upload.
```

```
## Sat 26 Sep · S13 — full project audit: security scan, gap scan, links wired
Instructed: "let's discuss what we did... scan from the very first till what
  we have done and find any vulnerabilities... find out if we miss something
  that we plan to do eventually."
Blocks done:
  - Settings -> About and onboarding's consent pane now link to the live
    Privacy Policy / Terms / Support email (core:ui gained a small shared
    Context.openUrl/openEmail helper). Both feature modules compile clean.
  - Git audit, both repos: `ledger-app` and `ledger-pages` both fully
    clean and fully pushed -- nothing uncommitted, nothing unpushed.
  - Secret-scan audit, both repos' FULL history (not just current files):
    no keystore, no *.properties secret file, no token/password-shaped
    string ever committed to either repo. `ledger-pages` (the PUBLIC one)
    checked for the first time -- clean, only the 4 expected page files
    ever existed in it.
  - Live Supabase security advisor re-run: same two findings as S11,
    both already reviewed and confirmed intentional/not-applicable
    (delete_my_account's SECURITY DEFINER is the feature itself;
    leaked-password-protection doesn't apply, no password auth exists).
    No new findings.
Gap scan (ledger-plan-v2.md + this whole log, S1 through S12) -- the
  project's own Gates table (line ~167) is the authoritative checklist;
  cross-checked against it rather than re-deriving one:
  - P0 backup restored, P1 RLS in CI, P2 sign-in on a real device, P4
    entry form, P5 full CRUD: all ✅, done weeks ago.
  - **P3 (property test + airplane cycle): the property test itself
    passed long ago (B7, green first run) -- only the physical
    airplane-mode device cycle is outstanding.**
  - **P6 (export/import round trip), P7 (signup -> deletion), P8 (on
    closed track): none done yet.**
  - **The single largest remaining gap, structural and project-wide:**
    this Claude Code environment has never had a physical device or
    emulator available (noted as far back as S-something/BUILD-LOG line
    845). Every "verified" claim in this entire log is verified at the
    logic/unit-test level only. Nothing has ever been run on an actual
    phone: C8's export/import, C9's airplane-mode boot gate, C11's
    delete-account flow (Credential Manager re-auth + local wipe +
    server RPC), the delete-account web page's real sign-in-and-delete
    round trip (only checked for correct rendering/no console errors
    today, not a real account), R8's ~100 Kotlin-metadata warnings'
    real-world effect, TalkBack, 48dp touch targets, 2.0x font scale,
    contrast, low-end device behavior, process death mid-form, wrong
    clock, and Sentry actually receiving one real event. This is not a
    new problem introduced today -- it is the accumulated, previously-
    flagged-piecemeal "not verified on a real device" note from
    S7/S8/S9/S12 finally gathered into one place.
  - Smaller, named, still-open items (all previously flagged, unchanged):
    Stats tab (unscheduled since S2, a deliberate scope note not a
    forgotten task), Entry detail's 5s undo (§7.11), SyncScheduler's
    onForegrounded trigger (§5.5 trigger 2, deliberately not wired --
    firing on every foreground would be sync spam, a real regression,
    not a neutral gap).
  - Play Console: phone-number verification resubmitted today with the
    corrected address, owner expects approval today or tomorrow --
    nothing for this session to do about Google's own review queue.
    15-20 testers lined up and ready to install once a build reaches
    the closed track.
Blocked on: nothing code-related. Everything above needs either the
  owner's own phone, or Play Console's review queue, or both.
Next, in the order that unblocks the most: (1) owner does a real-device
  pass -- P3's airplane cycle, P6's export/import, P7's full signup-to-
  deletion including the web page with a spare Google account, and a
  quick look at Sentry's dashboard after forcing one test crash; (2)
  Play Store listing content + Data Safety form (now unblocked -- both
  policy URLs resolve); (3) signed AAB uploaded to internal testing ->
  promote to closed once P6/P7 are confirmed -- that's what starts the
  14-day tester clock (P8).
```

```
## Sat 26 Sep · S13 (cont.) — launcher icon, Play listing content drafted
Split work: owner takes the real-device pass (P3/P6/P7), this session
  takes what doesn't need a phone.
Blocks done:
  - **Found and fixed a real gap:** the app had NO launcher icon at all
    -- AndroidManifest.xml never declared android:icon, no mipmap/
    drawable icon resources existed anywhere in the repo. Added an
    adaptive icon (minSdk 26, no legacy PNG fallback needed): Forest
    background, the same Lime "L" mark used on the public web pages.
    Hit one XML error along the way (a comment containing "--", which
    XML forbids) -- fixed, then `:app:assembleDebug` green.
  - `PLAY-LISTING.md` created (new, parent dir, same non-git reference-
    doc pattern as BUILD-LOG/ledger-plan-v2/KNOWN-ISSUES): short + full
    store description, the complete Data Safety form answers (matches
    §11.3 exactly), content rating guidance, target audience, financial-
    features declaration, and instructions for exporting the Play
    Store's required 512x512 PNG icon from the new vector assets via
    Android Studio's Image Asset Studio.
Known gaps, unchanged from S13's first half except:
  - Screenshots and the 1024x500 feature graphic still don't exist --
    screenshots need the owner's device (can capture during the P6/P7
    pass); the feature graphic is a design task, not code, left as a
    written spec in PLAY-LISTING.md rather than guessed at.
  - The 512x512 PNG icon itself isn't exported yet -- the vector source
    is done and instructions are written, but nobody's run Image Asset
    Studio yet.
Next: owner's real-device pass (P3/P6/P7) plus grabbing screenshots
  along the way; separately, exporting the PNG icon and making the
  feature graphic; then the actual Play Console form-filling using
  PLAY-LISTING.md as the source of truth.
```

```
## Sat 26 Sep · S13 (cont.) — owner's real icon swapped in, India-only distribution
Instructed: "I made this app for my own self... let's just keep it
  restricted to India... I have a better [icon] actually."
Blocks done:
  - Replaced the earlier AI-drafted placeholder launcher icon with the
    owner's own existing design (a lime rupee-symbol circle on dark
    #121614 -- exact pixel match to core:designsystem's CanvasDark and
    Lime, confirmed via PowerShell pixel sampling before touching
    anything). Source PNG had white corners (built for a light webpage
    background); corrected to a full-bleed dark square via chroma-key
    (any near-white pixel -> #121614) so Android's per-launcher
    adaptive-icon masking doesn't show corner artifacts. Wired in as
    the real foreground bitmap (mipmap-xxxhdpi) + a matching solid-
    color background vector; :app:assembleDebug green. Also exported
    the flat 512x512 Play Store hi-res icon (store-assets/, parent
    dir, ready to upload as-is).
  - PLAY-LISTING.md: added a "Country availability — India only"
    section with the exact Play Console path (Grow -> Store presence
    -> Countries/regions), and the reasoning (avoid taking on GDPR/
    other-regime compliance for an app built primarily for personal
    use). Noted this needs setting on BOTH the production listing and
    the closed-testing track separately, or non-India testers can't
    install. App icon row in the graphics table updated to "Done."
Known gaps: unchanged otherwise -- feature graphic and screenshots
  still outstanding, same as before.
Next: same as before (owner's real-device pass; feature graphic still
  needs a design pass). Open question for the owner: whether to also
  restyle the public Ledger-Pages web pages' small brand mark/favicon
  to match this same rupee icon for consistency -- not done yet,
  asked rather than assumed since those pages are already live.
```

```
## Sun 27 Sep · S14 — email sign-in backend (6-digit code), reverses D5
Instructed: add email sign-in next to Google, as a 6-digit one-time code,
  with 3-5 min expiry, one code per email per 60s, lockout after 3-5 wrong
  guesses, and the code invalid from the instant it's verified.
Why custom, not Supabase Auth's built-in email OTP: built-in has expiry and
  a resend interval, but no per-code wrong-guess counter (only a per-IP
  verify limit). So codes are issued/checked in our own table and Auth is
  used only to mint the session afterward.
Blocks done (all live on objcwocrbflwywckltbe):
  - 0011_email_otp.sql: email_otps table (HMAC of code only, keyed by the
    OTP_PEPPER function secret), otp_issue / otp_check / otp_discard.
    Rules: 5 min expiry; 60s cooldown; 5/hour + 10/day per email; 20/hour
    per IP; new code kills older unused ones; 5th wrong guess kills the
    code; success stamps consumed_at under a row lock. Execute granted to
    service_role only -- proacl verified live (no PUBLIC entry, per 0010).
  - supabase/tests/email_otp.sql (pgTAP, runs in db.yml CI). Same checks
    run live as a rolled-back DO block: all passed, table left empty.
  - Edge Function email-otp (verify_jwt=false; public by design), actions
    send / verify. Verify mints a session via admin createUser +
    generateLink(magiclink) + verifyOtp(token_hash). Sends via Resend or
    Brevo HTTP API. Live-tested: invalid_email 400, wrong_code/no_code
    401, 405 on GET, send -> 503 email_unavailable (no provider key yet)
    and a failed send does NOT burn the cooldown (row discarded).
Not yet verified: the success path (session minting) -- needs a real
  email delivered, i.e. an email provider key.
Blocked on owner: (1) email provider account + key -> `supabase secrets set
  BREVO_API_KEY=... EMAIL_FROM=...` (or RESEND_API_KEY; Resend needs a
  verified domain to mail anyone but yourself); (2) dashboard Auth ->
  Email OTP expiry 3600 -> 300 so Auth's own public /otp path can't hand
  out 1-hour codes.
Still to do (not started): Android side -- email entry + code screen,
  importSession, delete-account re-auth for email users (currently Google
  Credential Manager only), consent/Privacy Policy text ("Google only",
  "Google account ID"), and the branded onboarding / setup / tour redesign.
```

```
## Sun 27 Sep · S14 (cont.) — email sender wired to owner's own Gmail, live end-to-end
Owner declined Brevo/Resend (an unfamiliar third-party dashboard) after it
  was explained what an "email service" does; asked whether a cheap domain
  changes the calculus (explained SPF/DKIM/DMARC and Gmail's own 500/day cap
  vs a dedicated sender -- deferred until Instagram-driven growth actually
  needs it). Settled on Gmail SMTP via the owner's own address, which
  already sends and receives -- no new account, no cost.
  Correction mid-conversation: theledgerapp.support@gmail.com is the
  PUBLIC contact address already printed on the Privacy Policy/Terms/footer
  and the Play listing's Contact email row -- not the Sentry account (that's
  the owner's personal email, unrelated). Using it for OTP sends is
  actually the right choice, not just a safe one: incoming (support
  questions) and outgoing (OTP codes) don't interact, and the "from" name
  now matches the address people already see as this app's contact point.
Blocks done:
  - email-otp function rewritten to send via Gmail SMTP (nodemailer,
    smtp.gmail.com:465) instead of Resend/Brevo -- GMAIL_USER +
    GMAIL_APP_PASSWORD secrets (owner generated a 2FA app password,
    scoped to mail only, revocable independently of the real Google
    password -- explained why that distinction matters before asking for
    it). Both secrets set on objcwocrbflwywckltbe; redeployed.
  - Full live round trip confirmed working, not just simulated:
    send -> real email arrived in theledgerapp.support@gmail.com's own
    inbox -> owner read back the 6-digit code -> verify returned a real
    Supabase session (access_token/refresh_token/user.id) -> re-verifying
    the SAME code afterward correctly returned no_code (confirmed via
    email_otps.consumed_at = true in the table). This is the first fully
    live-confirmed success path for C13 -- everything before this was
    rule checks (cooldown/caps/lockout/expiry) via rolled-back SQL, never
    an actual delivered email or minted session.
  - The one real user row this created (theledgerapp.support@gmail.com)
    was left in place rather than cleaned up -- it's an address the owner
    genuinely controls, so it's a legitimate account, not test debris.
Next: Android side -- email + code entry screens, wiring the returned
  session into SessionStore/Supabase client the same way Google sign-in
  does, delete-account re-auth for email-only users (current flow is
  Google Credential Manager only), and the consent/Privacy Policy text
  ("Google only", "Google account ID") which is now inaccurate. Branded
  onboarding/tour redesign deliberately deferred until this sign-in work
  is done, per owner's own instruction.
```

```
## Sun 27 Sep · S14 (cont.) — email sign-in wired into the Android app
Blocks done (all compile + existing unit tests green: core:network,
  core:data, feature:auth, feature:settings, feature:onboarding, app):
  - core:network: Functions plugin installed on the SupabaseClient
    (SupabaseModule); EmailOtpApi/SupabaseEmailOtpApi call the email-otp
    Edge Function via supabase.functions.invoke and decode its JSON body
    uniformly on both success and error status codes. DTOs in dto/
    EmailOtpDto.kt matching index.ts's shapes exactly.
  - core:data: AuthRepository gains sendEmailCode/verifyEmailCode (domain-
    level EmailCodeSendOutcome/EmailCodeVerifyOutcome, not the network
    DTOs -- same boundary SyncRepository/SyncOutcome already draws against
    SyncApi) and currentAuthProvider() (reads the session's own
    app_metadata.provider: "google" | "email" | null). verifyEmailCode's
    Success path calls supabase.auth.importAuthToken(accessToken,
    refreshToken) to adopt the Edge Function's already-minted session as
    this client's own ambient one, then writes SessionStore -- same end
    state signInWithGoogleIdToken leaves.
  - feature:auth: EmailSignInScreen/ViewModel/UiState, a self-contained
    two-step flow (enter email -> enter code) layered over SignInScreen
    as a local toggle (same "internal pane, no new nav route" shape
    Onboarding already uses), reached via a new "Continue with email"
    text button under the existing Google button. Handles every server
    rejection reason (cooldown/too_many/email_unavailable on send;
    wrong_code with remaining-attempts count/expired/locked/no_code on
    verify) with its own message, plus a client-side resend countdown
    (cosmetic only -- the real 60s cooldown is still server-enforced).
  - feature:settings: DeleteAccountViewModel's re-authentication (§7.17
    step 4) now branches on currentAuthProvider() -- an email-sign-in
    account has no Google credential to re-prove, so it sends a fresh
    code to the ALREADY-signed-in address (fixed, never typed) and
    verifying it IS the re-auth proof before delete() runs, via a new
    DeleteAccountUiState.EmailReauth + EmailReauthContent composable.
    Google accounts are unaffected -- unchanged Credential Manager path.
  - Text fixes for the now-inaccurate "Google only" claims: Onboarding's
    dependency pane ("Sign-in is Google only" -> "There's no password
    recovery", now covering both), its consent pane ("email address and
    Google account ID" -> "email address, and a Google account ID if you
    sign in with Google"), and DeleteAccountScreen's confirm copy ("your
    Google sign-in link" -> "your sign-in").
Not yet done: the public Privacy Policy/Terms pages (ledger-pages/) still
  say Google-only and haven't been touched -- same inaccuracy, different
  surface, deliberately deferred to keep this session's scope to the
  in-app flow. PLAY-LISTING.md's Data Safety answers may also need a
  second look once email sign-in ships (still only collects email, so
  likely unaffected, but not re-verified against the actual form). No
  real device/emulator run yet -- everything here is compile + unit-test
  verified only, same standing gap noted throughout this whole project.
  Branded onboarding/tour redesign remains deliberately deferred per the
  owner's own instruction until this sign-in work is done.
```

```
## Sun 27 Sep · S15 — first real-device test, caught and fixed a genuine bug
Owner: "we can test on the emulators first." Ran full email sign-in on a
  real running AVD (emulator-5554, uninstall + fresh debug install), driven
  end to end (typed email, tapped Send, owner read the real code from
  theledgerapp.support@gmail.com's actual inbox and supplied it).
Screenshots came back solid black at first -- not a bug, FLAG_SECURE
  ("Hide amounts in the app switcher," on by default) also blocks
  screencap as a side effect. Verified the real screen content via
  `uiautomator dump`'s accessibility tree instead (unaffected by
  FLAG_SECURE) for the rest of the session.
**Found a real, live bug on the first verify attempt** -- logcat showed
  `SupabaseAuthRepository: importAuthToken after verifyEmailCode failed
  / IllegalArgumentException: importAuthToken returned without raising,
  but no session is present`. Root cause, confirmed by reading
  supabase-kt 3.8.0's actual source (Auth.kt/AuthImpl.kt, not guessed):
  `Auth.importAuthToken(accessToken, refreshToken)`'s convenience wrapper
  hardcodes `expiresIn=0L` on the UserSession it builds. `importSession`
  treats an already-expired-looking session as needing an immediate
  refresh and hands off to `recreateSessionJob` -- a coroutine launched on
  Auth's own `authScope`, joined only when `initializing=true`, which
  this call path never sets. So `importAuthToken` returns normally while
  the actual session-populating work happens moments later, off-thread --
  `currentSessionOrNull()` read immediately after comes back null. (The
  background job did eventually succeed on its own via the refresh token,
  which is why the FIRST attempt's app process still ended up signed in a
  few seconds later, landing on account setup on a later cold start --
  the bug was the timing, not a fundamental failure.)
**Fix:** stopped using `importAuthToken` for this path. The email-otp
  Edge Function's `expires_in` is now read through the whole chain
  (EmailOtpVerifyOkDto -> EmailOtpVerifyOutcome.Success -> here) and used
  to build a real `UserSession` directly, then `importSession(session,
  autoRefresh = true)` -- with a genuine non-zero expiry, importSession's
  own threshold check takes its synchronous "session is fresh enough"
  branch instead, so the session (and `sessionStore.write`) lands before
  the suspend call returns, no race. `retrieveUser(accessToken)` (a
  plain, synchronously-awaited call, unlike importAuthToken's fire-and-
  forget path) supplies the UserInfo `importSession` needs.
**Re-verified live after the fix**, not just recompiled: rebuilt,
  reinstalled, signed out, signed back in with email fresh. This time
  logcat shows zero exceptions and zero delay -- landed straight on Home
  with the account already there. `core:network`/`core:data` compile
  clean, `core:data` unit tests green.
This is the first genuine bug this whole project's Claude Code sessions
  have caught on an actual running Android process (every prior "verified"
  claim in BUILD-LOG, going back months, was compile/unit-test-level
  only -- see S13's own gap note). Confirms real-device/emulator testing
  is worth doing before every claim of "done," not just for UI polish.
Not yet tested this session: Google sign-in on this build (unchanged by
  this fix, but not re-exercised), delete-account's email re-auth path,
  the full onboarding tour from a truly fresh install (0-account, 0-
  session state), Owed-to-me/History/Stats screens, dark/light rendering
  (screenshots still blocked by FLAG_SECURE -- would need to toggle
  "Hide amounts in app switcher" off first, or use `uiautomator dump`
  text verification only, as done here).
```

```
## Sun 27 Sep · S16 — profile details (name/age/location/occupation) + full re-verification
Owner: since email sign-in gives only an address (no name, unlike Google),
  collect a short profile after first sign-in -- name, age, location
  ("where they're operating from"), occupation from ~15 presets -- so
  personalization/greeting works for email users too. Then re-verify
  Google sign-in, email delete-account, and a truly fresh install, since
  S15 only covered email sign-in itself.
Blocks done:
  - 0012_profile_details.sql: `profiles` gains age (smallint, 13-120),
    location (text, <=80 chars), occupation (text, one of 15 fixed
    presets incl. "Other" -- no free-text elaboration, per owner's own
    "that is enough"). Ordinary user-owned RLS columns, same as the
    existing display_name -- protect_profile_columns() only locks
    plan/entitlement_expires_at/entry_count, untouched here.
  - core:model: UserProfile gains age/location/occupation + a
    hasProfileDetails flag (true once displayName is set); ProfileOccupations
    holds the fixed 15-item list, one source of truth for both the DB
    check constraint and the app's dropdown.
  - core:network/core:data: ProfileDetailsFetchDto/UpdateDto,
    ProfileRepository.fetchProfileDetails()/saveProfileDetails(). A failed
    save doesn't block progress into the app (no offline queue built for
    this, deliberately) -- since hasProfileDetails reads false again until
    the write lands, the next cold boot naturally re-asks, which doubles
    as the retry mechanism.
  - AuthRepository.nameHintFromProvider(): reads Supabase's own
    auth.users.raw_user_meta_data (GoTrue already stores Google's ID-token
    name claim there automatically, no sign-in code changes needed) so a
    Google user's name field comes prefilled; an email user's starts blank.
  - New ProfileSetupScreen/ViewModel (app module), wired into
    LedgerNavHost between Restoring and Account setup as PROFILE_SETUP --
    same once-per-new-account, skip-if-already-answered shape Account
    setup itself already uses. Only the name is required; age/location/
    occupation stay optional.
**Re-verified everything live on the emulator, not just this new feature:**
  - Fresh install (full uninstall, not just -r) -> Onboarding pane 1,
    confirmed clean.
  - **Google sign-in**, real account picker (`try.bibhuti1@gmail.com`,
    already has years of real prior data -- Domestic/International/Cash/
    SBI Credit Card). Correctly skipped BOTH new gates (profile already
    has a display_name, accounts already exist) and landed straight on
    Home with all real data intact -- proves the new gates don't disturb
    a returning account.
  - **Email sign-in** on the same theledgerapp.support@gmail.com account
    used in S15 (no display_name yet) -- this time correctly SHOWED
    ProfileSetupScreen (blank, no Google hint) instead of skipping.
    Filled name "Aloo", age 19, location "Hyderabad", occupation
    "Student", continued -- landed on Home. Verified directly in the
    database (not just the UI): all four values present and correct on
    `profiles`.
  - **Email delete-account re-auth**: tapped Delete -> backup nudge ->
    typed DELETE -> correctly showed "Verify it's you" (a fresh code to
    the signed-in address) instead of a Google picker, confirming
    currentAuthProvider()-based branching (C13) works live, not just in
    theory. Verified the code, deletion completed, confirmed via a direct
    database query that the auth.users row is actually gone (0 rows), and
    the app landed back on Onboarding pane 1 with no leftover state.
  - `core:data` unit tests still green after all of today's changes.
Screenshots of our own app stay blocked by FLAG_SECURE throughout (as
  found in S15) -- every check above was done via `uiautomator dump`'s
  accessibility tree plus, for the Google account-picker step specifically
  (a different app's window, not covered by our FLAG_SECURE), real
  screenshots, which also incidentally confirmed the real launcher icon
  renders correctly.
Not yet touched: the public Privacy Policy page doesn't mention
  age/location/occupation collection yet (same category of gap as S14's
  still-outstanding Google-only wording); PLAY-LISTING.md's Data Safety
  form doesn't declare these either. Branded onboarding/tour redesign
  still deliberately deferred, per the owner's own standing instruction.

```
## Sun 27 Sep · S17 — first-run flow redesign (onboarding + sign in/up + code), built and verified live
Designed on the "Ledger Sign In" design canvas first (6 artboards, critique +
  Ogilvy copy pass, owner approved "build it all"), then implemented 1:1.
Story/copy: "Every rupee, remembered. Every IOU, too." -> "Saved the moment
  you tap Save." -> "Your money is nobody's business." -> Sign in ("Welcome
  back.") / Sign up ("Start your Ledger.", 3 value rows) -> "Check your inbox."
  No "Free" claim (profiles.plan has 'supporter'); consent stays "Agree and
  continue"; the no-recovery warning kept, reworded as "your spare key".
Blocks done:
  - core:designsystem: SystemChrome/SystemChromeController + SystemChromeEffect
    (per-screen colour behind status/nav bars, owner-keyed so the outgoing
    screen's dispose can't clobber the incoming screen's claim -- that exact
    bug was hit live: cream status bar under white icons after
    Onboarding -> Sign in); LedgerHeroSheet (dark hero + cream sheet that
    always reaches the bottom, scrolls when it doesn't fit); LedgerBrandMark;
    LedgerLineIcons (outline set from the canvas's own path data);
    DisplayHeadline / LimeButton / IconBadge / PageDots; 4 new colour tokens.
  - app: MainActivity root paints SystemChrome top/bottom and flips status-bar
    icon colour; everything else keeps the cream default.
  - feature:onboarding: 3 panes rewritten (tilted History day card with an
    "Owed to you" IOU, the save/backup rows, consent as 4 icon rows + policy
    link); BackHandler steps back a pane; AnimatedContent between panes.
  - feature:auth: one AuthScreen for Sign in / Sign up (small pill toggle,
    Role.Tab, 48 tall) + CodeStep (6 boxes over one hidden BasicTextField:
    type/paste/autofill ContentType.SmsOtpCode; 6th digit auto-submits);
    Google unchanged underneath, restyled as white outline with the official
    G. EmailSignInScreen.kt deleted. EmailSignInViewModel: resend stays on
    the code screen (new `resending` flag), changeEmail(), auto-verify.
    Error copy de-hyphenated ("That's not it. 4 tries left."), same in
    DeleteAccountViewModel.
Verified live on emulator-5554 (fresh install): all 6 screens screenshotted
  and matched the canvas; real code sent; wrong code -> red boxes + message +
  focus returns (second live fix: focus was lost after a failed check).
  All debug unit tests green.
Screenshots: `adb exec-out screencap` is always black here (FLAG_SECURE) but
  `adb emu screenrecord screenshot <dir>` captures the emulator framebuffer
  regardless -- use that. (A temporary FLAG_SECURE bypass was tried first,
  found unnecessary, and reverted -- confirmed setFlags(FLAG_SECURE) present.)
Test hiccup: adb typing produced "ttheledgerapp.support@gmail.com" once, so
  one OTP email went to that mistyped address; no account created (accounts
  only exist after a verified code).
Not done / noticed: system splash is cream, so a first launch flashes cream ->
  dark onboarding; ProfileSetup and AccountSetup (after sign-in) are still the
  old plain style; Privacy Policy page + Data Safety still don't mention
  email sign-in or age/location/occupation; nothing committed.

## Sun 27 Sep · S18 — app tour, 2-minute resend, account naming as a sheet over Home
Owner asks, in order: a dim-and-spotlight tour for new users BEFORE naming
  accounts; resend cooldown 60s -> 2 min; "which accounts" as a popup over a
  dimmed real Home, not a full screen; then a second round of feedback on
  sign-up, the profile page and the account sheet.
Blocks done:
  - AppTourScreen/ViewModel: 4 panes (Add Payment, History, Stats, profile)
    on crafted mockups, spotlight = lime glow, the rest dimmed. Runs once,
    gated on "no accounts yet". Skip tour lands on HOME.
  - 0013_otp_cooldown_2min.sql applied live: otp_issue() 60s -> 2 min, and
    resend_in_s 120. pgTAP test updated. App fallback copy 60 -> 120.
    Verified live: the code screen showed "Resend in 1:56".
  - ACCOUNT_SETUP route removed. HOME now renders HomeScreen + AccountSetupRoute
    together; the setup is a dark sheet sliding up to ~73% height, so Home's
    "Add an account first" line stays readable above it. One field to start,
    suggestion chips (Cash in hand, Savings account, Salary account, Credit
    card, UPI wallet, Crypto) that wrap and toggle, tap outside = keyboard
    away, explainer rows (one is enough, two unlock self transfer, each keeps
    its own balance). Sets system bars to match while open.
  - Auth: Sign up is the default and the left tab (session-expired return
    still opens on Sign in). Value card removed. Sign up asks name + email;
    the name is held in memory (AuthRepository.rememberSignUpName) and
    ProfileSetup greets by it instead of asking again.
  - ProfileSetup rebuilt in the first-run style ("Good to have you, <name>."),
    occupation as wrapping chips, "Skip for now". New shared FirstRunField /
    FirstRunChip in core:designsystem.
Real bug found live: a brand-new account never saw the account popup. The
  Add Payment ViewModel (and Accounts) still called ensureSeeded() as a
  fallback, which now raced the popup's own "any accounts?" check on the same
  screen and won, silently creating Domestic/International/Cash. Both calls
  removed (the popup blocks Home, so the fallback can't be needed).
Also caught live: the "UPI Wallet" chip wrapping one letter per line (row had
  no room); chips now wrap as a FlowRow.
Verified live: new +setup2/+setup3/+setup4 accounts through sign-up, profile,
  tour and the sheet; accounts saved and shown on Home.

## Mon 28 Sep · S19 — full stress test as a real user, then fixes
Ran every screen on the signed-in +setup4 account (entries, edits, deletes,
  transfers, loans, stats, accounts, settings, airplane mode, rotation, kill
  and restore, 1.5x text). 32 findings; full list with status in
  STRESS-TEST-2026-09-28.md. The ones that mattered:
  - "Send now" did nothing in Smart mode: enqueueNow() used KEEP on the same
    unique work as Smart's 90s delayed job. Now REPLACE when waiting,
    APPEND_OR_REPLACE when running. Verified: worker starts on tap.
  - Home/Settings never noticed the network coming back, so Send stayed
    disabled ("Waiting for signal"). ConnectivityChecker.onlineFlow() is a
    live NetworkCallback flow. Verified with airplane mode on/off.
  - App lock was biometric-only and failed open on PIN-only phones. Now
    BIOMETRIC_WEAK | DEVICE_CREDENTIAL, relocks after 60s in the background,
    and the switch refuses (with a toast) when the phone has no screen lock.
  - Restore from file: unparseable values could crash, and out-of-range rows
    would never sync. Rows are now checked against the server's CHECKs and
    skipped with a count.
  - Self transfers appeared under Owed to me > Closed (shared Y id). Excluded.
  - Archiving every account left Add Payment with no way forward. The last
    active account can't be archived now.
  - Draft kept an archived account id (no chip lit). Re-validated on change.
  - Before 5:30 am the date defaults to yesterday by design (looked like a
    stale date to me too); a line under the date now says so.
  - Smaller: blocked field scrolls into view, keyboard closes after save,
    notes clamp to 2 lines, account name capped at 40, tabs auto-shrink and
    account rows stack at large text sizes.
Copy pass (Ogilvy, no em dashes or "--" left in any UI string):
  "N entries saved on this device -- waiting for company" -> "1 change not
  backed up yet. Going up shortly." + "Back up now" (it counted account ops
  as entries); "Backup · never backed up" (everything was synced) -> "Your own
  copy · no file saved yet"; confirm sentence "your Savings account account"
  and "two rows" gone; Transaction ID -> Reference, dropped from banner/toast;
  "leg category" -> "Category, money out/in"; Stats "Total have" -> "Across
  accounts", "Average a day" -> "Daily average". Tests updated to match.
Not tested on purpose: sign out/in and Delete account (owner asked to stay on
  the signed-in account).
Git: working tree committed in two logical commits (first-run/email sign-in,
  stress-test fixes) and pushed with the 5 already-waiting commits to
  github.com/dumbopanda/Ledger-APK (private). Diff scanned for secrets first.

## Mon 28 Sep · S20 — opening balances, loans out of Stats
Owner's calls on the two open product questions from S19.
  - Opening balance: stored as one ordinary entry per account (name and
    category "Opening balance", Credit for money in it, Debit for owed on
    it), dated on the account's earliest entry so every month's balance is
    right. No schema or server change; syncs and backs up like any entry.
    EntryRepository.setOpeningBalance()/openingBalances(), 3 new EntryDao
    queries. Set from the first-run sheet ("balance now" beside each name)
    or Settings > Accounts > Balance ("Money in it" / "Owed on it"), shown
    as "Started with ₹2,000" / "Started owing ₹500". Kept out of the Add
    Payment name list, and hidden from History (still in balances).
  - Stats: loan legs (Y id, not a transfer) and opening balances no longer
    count toward Total spend, Money in, Biggest expense or the daily figures;
    balances still include both. "Entries" counts a transfer once.
Verified live on +setup4: Cash ₹2,000 opening -> ₹1,855; Savings owing ₹500 ->
  -₹400; Total spend ₹45 and Money in ₹0 with Riya's ₹450 loan and repayment
  excluded; no opening-balance rows in History. All debug unit tests green.
Git: committed and pushed after each step (12890e5, 2572ba1).
Still open: Privacy Policy page and Data Safety form don't mention email
  sign-in or age/location/occupation; cream system splash flashes before the
  dark onboarding; Stats' Money in/spend ignore loans by design now, so a
  loan-heavy month can look quiet (Owed to me shows them).
```
