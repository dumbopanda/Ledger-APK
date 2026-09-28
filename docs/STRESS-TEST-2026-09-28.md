# Stress test, 28 Sept 2026

Account used: `theledgerapp.support+setup4@gmail.com` (already signed in, never signed out).
Device: Android emulator, debug build. Every item below was seen live, not inferred.

## Fixed

| # | Severity | What was wrong | Fix |
|---|---|---|---|
| 1 | High | **"Send now" did nothing** in Smart mode. The immediate sync used `KEEP` on the same WorkManager job as Smart's 90-second delayed sync, so the delayed one always won. | `SyncScheduler.enqueueNow()` replaces a waiting job; if one is running it queues right after it. Verified: sync starts the moment you tap. |
| 2 | High | **Offline bar never cleared.** After the network came back, Home kept saying "offline" with a disabled button, so you couldn't send at all. Online state was a one-off snapshot. | `ConnectivityChecker.onlineFlow()` follows the network live; Home and Settings both use it. Verified with airplane mode on and off. |
| 3 | High (security) | **App lock did nothing on phones with only a PIN.** The setting says "fingerprint or screen lock", but the lock accepted biometrics only and silently skipped itself otherwise. It also never re-locked after the app was backgrounded. | Accepts fingerprint or the phone's PIN/pattern; re-locks after a minute away; refuses to switch on (with a reason) when the phone has no screen lock. |
| 4 | Medium (robustness) | **Restoring an edited or damaged backup could crash the app** (unparseable dates), or insert rows the server will always refuse. | Every restored row is checked against the server's own limits; bad rows are skipped and counted ("Skipped 2 that didn't look right."). |
| 5 | Medium | **Self transfers showed under "Owed to me > Closed"** as if they were settled loans. | Transfers are excluded from Owed to me. |
| 6 | Medium | **Archiving every account left a dead end**: Add Payment said "add an account first" with no way to do it from there. | Archiving your last active account is refused with a reason. |
| 7 | Medium | Add Payment kept pointing at an archived account (no chip lit, save went to a hidden account). | Selection is re-checked whenever the account list changes. |
| 8 | Medium | Before 5:30 am new entries default to yesterday, **by design**, but nothing said so; it looked like a stale-date bug. | A line under the date explains it and says to tap Today. |
| 9 | Low | Failed save left the amount field (now red) scrolled off screen. | The field is scrolled into view once the keyboard is up. |
| 10 | Low | After saving, the keyboard stayed up on Notes. | Focus clears on save. |
| 11 | Low | History printed the whole note (up to 300 characters) under each row. | Clamped to two lines. |
| 12 | Low | "Self Transfer" was offered as a name for ordinary payments. | Hidden unless the transfer switch is on. |
| 13 | Low | Account name field let you type past 40 characters, then errored. | Capped at 40. |
| 14 | Low | At 1.5x text size the first tab read "Add Payme"; account names wrapped one letter per line. | Tab labels shrink to fit; account buttons sit on their own line. |

## Copy fixed

- **"N entries saved on this device — waiting for company"** is now "1 change not backed up yet. Going up shortly." with a **Back up now** button. It counted accounts and deletions as "entries", so it now says "changes". Offline: "… You're offline." with a disabled **Offline** button.
- Settings: "Waiting to be sent" is now **Backing up to your account**, "Nothing waiting" is **All backed up**, "Batched" is **Smart**, with a plain explanation of each mode.
- Settings "Backup · never backed up" read as if data were at risk, though everything was already synced. It's now **Your own copy · no file saved yet**, with a **Save file** button. The Home nag is now "Keep a copy in your own files too?"
- Confirm sheet: "from your Savings account **account**" (the word was always appended), "recorded as **two rows**", "**leaves your**… **from** Dinner with Riya" are all rewritten ("₹100.00 moves from Cash in hand to Savings account… It's your own money moving, so it doesn't count as spending or income.").
- "Transaction ID" is now **Reference**; the edit banner and toasts no longer lead with the raw ID.
- "Edit entry" (which really meant go back) is now **Go back**.
- "Sent leg category / Received leg category" are now **Category, money out / money in**.
- "New — I am lending or paying out now / Old — this is money coming back" are now **A new loan I'm giving / Money coming back to me**.
- Every em dash, en dash and `--` in user-facing text is gone (tour, date hints, validation, delete account, detail sheet, Settings). Empty values show words ("Untitled", "None yet", "Not signed in") instead of a bare dash.
- Stats: "Total have" is now **Across accounts**, "Average a day" is **Daily average**.

## Your call (not changed)

1. **No opening balance.** Accounts start at ₹0, so after a normal Bank to Cash transfer Stats shows Savings at **−₹500**. Options: an "opening balance" field on each account, or a first entry prompt.
2. **Loans count as spending.** The ₹450 lent to Riya shows in "Total spend" and as "Biggest expense", and her repayment as "Money in". Arguably a loan that came back is neither.
3. "Entries 5" in Stats counts a transfer as two.
4. Landscape works but the header takes most of the screen; most finance apps lock portrait.

## Not tested (on purpose)

Sign out, sign in, and Delete account. You asked me to stay on the signed-in account, and delete needs a fresh email code.
