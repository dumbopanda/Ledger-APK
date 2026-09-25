// Setup type definitions for built-in Supabase Runtime APIs
import "@supabase/functions-js/edge-runtime.d.ts";

// §11.3 / §7.17: "In-app (§7.17) and a public web URL. Play requires the web route for any app
// with accounts. Google-only sign-in does not exempt you." This serves that page directly from
// the same Supabase project the app already uses -- no separate domain or hosting decision
// needed to have a genuinely reachable public URL today (Play just needs one, not a branded
// one). It signs in with the same Google Sign-In flow the app uses (Google Identity Services'
// web SDK, the browser counterpart to Credential Manager), then POSTs to this project's own
// existing `delete-account` function with the resulting session's access token -- reusing
// `delete_my_account()` (0007_account_deletion.sql) rather than a second copy of it.
//
// Deliberately NOT `withSupabase(...)` like `delete-account/index.ts`: this page must be
// reachable with no Authorization header at all (that's the whole point of a public URL) --
// the auth happens later, client-side, once the person signs in through the page itself.
//
// GOOGLE_WEB_CLIENT_ID below is hardcoded, not read from an env var: it is the same public,
// non-secret OAuth web client ID already embedded in the distributed Android APK's own
// BuildConfig (feature:auth, feature:settings) -- there is nothing here that isn't already
// public. SUPABASE_URL/SUPABASE_ANON_KEY use the ambient env vars every Supabase Edge Function
// already has, rather than a third hardcoded copy of values that live in this repo's own
// local.properties (gitignored, dev-machine-only) and the Android app's BuildConfig.
const GOOGLE_WEB_CLIENT_ID = "141141696760-mq9uhebnj33uiddj1puevvdnigcuvg43.apps.googleusercontent.com";

function renderPage(supabaseUrl: string, supabaseAnonKey: string): string {
  return `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>Delete your Ledger account</title>
<script src="https://accounts.google.com/gsi/client" async defer></script>
<script type="module">
  import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

  const SUPABASE_URL = "${supabaseUrl}";
  const SUPABASE_ANON_KEY = "${supabaseAnonKey}";
  const GOOGLE_CLIENT_ID = "${GOOGLE_WEB_CLIENT_ID}";

  const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

  const statusEl = document.getElementById("status");
  const signInSection = document.getElementById("sign-in-section");
  const confirmSection = document.getElementById("confirm-section");
  const emailEl = document.getElementById("signed-in-email");
  const confirmInput = document.getElementById("confirm-input");
  const deleteButton = document.getElementById("delete-button");

  let accessToken = null;
  let rawNonce = null;

  function setStatus(message, isError) {
    statusEl.textContent = message || "";
    statusEl.className = isError ? "status error" : "status";
  }

  async function sha256Hex(value) {
    const data = new TextEncoder().encode(value);
    const digest = await crypto.subtle.digest("SHA-256", data);
    return Array.from(new Uint8Array(digest)).map((b) => b.toString(16).padStart(2, "0")).join("");
  }

  async function handleCredentialResponse(response) {
    setStatus("Signing you in...", false);
    try {
      const { data, error } = await supabase.auth.signInWithIdToken({
        provider: "google",
        token: response.credential,
        nonce: rawNonce,
      });
      if (error) throw error;
      accessToken = data.session.access_token;
      emailEl.textContent = data.user.email || "your account";
      signInSection.hidden = true;
      confirmSection.hidden = false;
      setStatus("", false);
    } catch (err) {
      setStatus("Couldn't sign you in: " + err.message, true);
    }
  }

  async function init() {
    rawNonce = crypto.randomUUID();
    const hashedNonce = await sha256Hex(rawNonce);
    google.accounts.id.initialize({
      client_id: GOOGLE_CLIENT_ID,
      nonce: hashedNonce,
      callback: handleCredentialResponse,
    });
    google.accounts.id.renderButton(document.getElementById("g_id_signin"), { theme: "outline", size: "large" });
  }

  confirmInput.addEventListener("input", () => {
    deleteButton.disabled = confirmInput.value !== "DELETE";
  });

  deleteButton.addEventListener("click", async () => {
    deleteButton.disabled = true;
    setStatus("Deleting your account...", false);
    try {
      const response = await fetch(SUPABASE_URL + "/functions/v1/delete-account", {
        method: "POST",
        headers: { Authorization: "Bearer " + accessToken },
      });
      const body = await response.json();
      if (!response.ok) throw new Error(body.error || "The server refused the request");
      confirmSection.hidden = true;
      document.getElementById("done-section").hidden = false;
    } catch (err) {
      setStatus("Couldn't delete your account: " + err.message, true);
      deleteButton.disabled = confirmInput.value !== "DELETE";
    }
  });

  window.addEventListener("load", init);
</script>
<style>
  body { font-family: system-ui, sans-serif; max-width: 480px; margin: 48px auto; padding: 0 16px; color: #1c1b1a; }
  h1 { font-size: 1.4rem; }
  .status { min-height: 1.2em; margin-top: 12px; }
  .status.error { color: #c62828; }
  input { font-size: 1rem; padding: 8px; width: 100%; box-sizing: border-box; margin-top: 12px; }
  button { font-size: 1rem; padding: 10px 20px; margin-top: 16px; background: #c62828; color: white; border: none; border-radius: 24px; }
  button:disabled { background: #ccc; }
  ul { line-height: 1.6; }
</style>
</head>
<body>
  <h1>Delete your Ledger account</h1>
  <p>This deletes your entries, your accounts, your profile, and your Google sign-in link. It happens immediately and cannot be undone.</p>
  <div id="sign-in-section">
    <p>Sign in with the Google account you use with Ledger to continue.</p>
    <div id="g_id_signin"></div>
  </div>
  <div id="confirm-section" hidden>
    <p>Signed in as <strong id="signed-in-email"></strong>.</p>
    <p>Type <strong>DELETE</strong> to confirm. This is permanent.</p>
    <input id="confirm-input" type="text" autocomplete="off" />
    <br />
    <button id="delete-button" disabled>Delete my account</button>
  </div>
  <div id="done-section" hidden>
    <p>Your account has been deleted.</p>
  </div>
  <p id="status" class="status"></p>
</body>
</html>`;
}

export default {
  fetch: (_req: Request) => {
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
    return new Response(renderPage(supabaseUrl, supabaseAnonKey), {
      headers: { "content-type": "text/html; charset=utf-8" },
    });
  },
};

/* To invoke locally:

  1. Run `supabase start`
  2. Open http://127.0.0.1:54321/functions/v1/delete-account-page in a browser

  NOT manually verified against a live browser/Google/Supabase round trip in this session --
  see BUILD-LOG's C11 entry. The nonce scheme (raw UUID, SHA-256 hex hash given to Google,
  raw value given to Supabase) mirrors the Android app's own SignInViewModel exactly.

*/
