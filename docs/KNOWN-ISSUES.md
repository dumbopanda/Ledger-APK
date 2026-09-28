# KNOWN ISSUES — Ledger

Environment/tooling errors hit on this machine that aren't bugs in the app's own code — things that will
reappear if the same conditions repeat (a fresh machine, a tool upgrade, a clean checkout). Companion to
`BUILD-LOG.md` (the *when*) and `ledger-plan-v2.md` (the *what*): this is the *"we already fought this,
here's the fix"*.

**Start of every session that hits a build/tooling error:** check here first before re-diagnosing from scratch.
**Whenever a new one is solved:** add an entry below, same format.

---

## Supabase Edge Functions cannot serve an interactive HTML page — content-type and CSP get force-overridden

**When it happens:** any Edge Function whose `fetch` handler returns HTML (`Response(html, { headers: {
"content-type": "text/html" } })`), when opened directly in a browser.

**Root cause:** confirmed live 2026-09-26 by deploying `delete-account-page` and checking the real response
headers with `curl -sD -`. Supabase's gateway silently rewrites the response to `Content-Type: text/plain`
and injects `Content-Security-Policy: default-src 'none'; sandbox` regardless of what the function code
sets — this is a deliberate platform policy (Edge Functions share the `*.supabase.co` domain across every
Supabase customer, so Supabase won't let one function turn that shared, trusted domain into an open web
host / phishing surface). Not overridable from function code; not something misconfigured on our end.
Confirmed via public discussion threads (getsentry-style known issue, not unique to this project) —
Supabase's own docs describe Edge Functions as being for APIs/data, not page hosting.

**What this means for this project:** the public "delete your account" page (§7.17/§11.3 — required by
Play for any app with sign-in) cannot live on Supabase as originally planned. Moved to GitHub Pages
instead, in a separate **public** repo `dumbopanda/Ledger-Pages` (local copy: `Ledger APK/ledger-pages/`),
file `delete-account/index.html`, served at `https://dumbopanda.github.io/Ledger-Pages/delete-account/`.
Separate repo because free GitHub Pages requires a public repo, and the app repo (`Ledger-APK`) stays
private. Privacy Policy / Terms pages should go in the same public repo. It's a plain
static file, hardcoding the same already-public `SUPABASE_URL`/`SUPABASE_ANON_KEY`/`GOOGLE_CLIENT_ID`
values the Android app itself ships with. It still calls the **existing** `delete-account` Supabase Edge
Function to actually perform the deletion — that part is untouched and unaffected, since it returns JSON
via POST, not HTML via GET, which is exactly the normal way Edge Functions are meant to be used.

**If a future page-hosting idea comes up that involves serving HTML/JS from a Supabase Edge Function
directly:** don't — go straight to a static host (GitHub Pages, Cloudflare Pages, Netlify, Vercel) and
call Supabase only for its actual API/data endpoints.

**Cleanup — done, 26 Sep 2026:** its source folder and `config.toml` entry were removed from the app
repo, the `https://objcwocrbflwywckltbe.supabase.co` origin was removed from the Google OAuth web client
(the only origin needed now is `https://dumbopanda.github.io`), and the deployed function itself was
deleted by the owner in the Supabase dashboard (Edge Functions → `delete-account-page` → delete — the
Supabase MCP tools available here can deploy functions but cannot delete them). Confirmed gone via
`list_edge_functions`: only `delete-account` remains.

---

## Sentry mapping upload fails on Windows: `'D:\[02]' is not recognized as an internal or external command`

**When it happens:** running `:app:assembleRelease` (or `:app:uploadSentryProguardMappingsRelease` directly)
on a Windows machine, once `sentry.properties` has a valid `auth.token` in place.

**Root cause:** the Sentry Android Gradle Plugin extracts its own bundled `sentry-cli.exe` into the
project's build folder (`app/build/tmp/sentry-cli-<version>.exe`) and then invokes it without properly
quoting the path on Windows. Our project lives at `D:\[02] GROWTH\Ledger APK\...` — a path with spaces
— so Windows cuts the command off at the first space and tries to run `D:\[02]` as a program. This is a
plugin bug, not something wrong with the token, the DSN, or the project setup. Any path with a space
anywhere in it (not just this one's brackets) would trigger it the same way.

**Fix — keep a copy of the tool somewhere without spaces in the path, and point `sentry.properties` at it:**

```bash
mkdir -p "/c/Users/bibhu/sentry-cli"
cp "/d/[02] GROWTH/Ledger APK/ledger-app/app/build/tmp/sentry-cli-3.8.0.exe" "/c/Users/bibhu/sentry-cli/sentry-cli.exe"
```

Then add this line to `ledger-app/sentry.properties` (gitignored, machine-local — same file that holds
`defaults.org` / `defaults.project` / `auth.token`):

```
cli.executable=C:/Users/bibhu/sentry-cli/sentry-cli.exe
```

(Forward slashes, not backslashes — Java's `.properties` file format treats a backslash as an escape
character, so a raw `C:\Users\...` path would be misread.)

**If this ever needs redoing** (new machine, `sentry.properties` recreated from scratch, plugin version
bump changes the bundled exe's filename): the exe re-appears at `app/build/tmp/sentry-cli-*.exe` after
any build attempt (even a failed one) — copy that one, same as above.

**Confirmed fixed:** 2026-09-26 — verified with a full `clean :app:assembleRelease`, upload succeeded
automatically with no manual task invocation needed (`UPLOADED <uuid> (Proguard mapping)` in the log).

---

## R8: "An error occurred when parsing kotlin metadata" (dozens of warnings during release build)

**When it happens:** every `:app:assembleRelease` since R8/shrinking was turned on, ~100 occurrences per
build.

**Root cause:** AGP 8.13.0's bundled R8 version predates Kotlin 2.3.20 (the version this project builds
with) — R8 can't parse the newer Kotlin metadata format on some classes, so it falls back to a more
conservative (safe, just less optimal) shrinking strategy for those specific classes.

**Status: known, accepted, non-fatal.** Every release build so far (including with Sentry added) has
completed successfully and produced a working, correctly-signed APK despite these warnings. Not something
to "fix" by chasing each one down — only worth revisiting if a future AGP/R8 upgrade changes the picture,
or if an actual runtime crash is ever traced back to a specific over-conservatively-shrunk class.

**If a NEW class of R8/build warning shows up** that looks similar to this, don't assume it's the same
harmless category by default — check whether the release APK still builds, installs, and runs correctly
before writing it off.

---

## My own recurring mistake: XML comments containing "--" break the build

**When it happens:** any time I (Claude) write an AndroidManifest.xml or vector-drawable
comment using "--" as a dash/separator (a natural habit in prose). XML forbids "--" anywhere
inside a `<!-- -->` comment, not just at the very start/end.

**Hit twice in one session (26 Sep 2026):** once in `ic_launcher_background.xml`
("...Kotlin color constants -- adaptive icons can't...", caught by a resource-compile error),
then again in `AndroidManifest.xml`'s Sentry auto-init comment ("...ever runs -- and crashes
immediately...", caught by a manifest-merger error). Same root cause both times, not yet
internalized after the first fix.

**Fix:** don't use "--" in XML comments -- use a comma, a period, or just a line break instead.
**For future sessions:** if an XML resource or manifest fails to parse/merge with no obviously
wrong tag, check every comment for a stray "--" before looking anywhere else.
