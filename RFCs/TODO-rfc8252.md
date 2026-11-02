# RFC 8252 (OAuth 2.0 for Native Apps)

## Client type & registration (§8.4, §8.5)
- [ ] Treat the native app as a **public client** (no client secret used as proof of identity)
- [ ] Don't require / rely on a statically embedded client secret (NOT RECOMMENDED); if the AS insists, still treat the client as public
- [ ] Register the **complete** redirect URI with the AS (incl. path); library uses exactly the registered URI (loopback: only the port may differ)
- [ ] Support/allow supplying extra platform-specific identity info where the AS wants it (package/bundle name) (MAY)

## External user-agent (§4, §5, §6, §8.12)
- [ ] **MUST use an external user-agent (system browser)** for the authorization request
- [ ] **MUST NOT use embedded user-agents / web-views** (no Swing/JavaFX WebView login)
- [ ] Open the authorization URI in the default browser (Java: `Desktop.browse` / `xdg-open` / `open` / `start` fallbacks)
- [ ] Allow a pluggable "browser launcher" SPI (in-app browser tab/other external UA = MAY; for platforms that have them it is RECOMMENDED)
- [ ] Handle environments where no browser can be launched (headless) with a clear error / fallback hook (e.g. print URL)

## Authorization request (§6)
- [ ] Use the **authorization code grant** (RFC 6749 §4.1) with a redirect URI the app can receive
- [ ] **MUST implement PKCE (RFC 7636)** for public native clients
- [ ] Do **not** use the implicit flow (NOT RECOMMENDED, §8.2)

## Receiving the authorization response – redirect URI options (§7)
### Loopback interface redirect (§7.3, §8.3) – primary option for desktop Java apps
- [ ] Redirect URI form `http://127.0.0.1:{port}/{path}` (IPv4) and `http://[::1]:{port}/{path}` (IPv6)
- [ ] Obtain a **free ephemeral port from the OS at request time** (bind port 0) and put it into `redirect_uri`
- [ ] Don't assume IPv4 or IPv6 support; **try binding both** and use whichever is available (RECOMMENDED)
- [ ] Bind **only to the loopback interface** (never 0.0.0.0)
- [ ] Prefer the **IP literal** over `localhost` (`localhost` NOT RECOMMENDED)
- [ ] **Open the port only when starting the authorization request and close it once the response is returned** (or on error/timeout/cancel)
- [ ] Don't set `SO_REUSEADDR`/`SO_REUSEPORT` (Linux/macOS) so other apps can't bind the same socket; on Windows set `SO_EXCLUSIVEADDRUSE` (`StandardSocketOptions`/channel config in Java)
- [ ] Path must exactly match what is registered
- [ ] Return a user-friendly "you can close this window" page without third-party content

### Private-use URI scheme redirect (§7.1) – mainly mobile/desktop apps that can register schemes
- [ ] Support redirect URIs of the form `com.example.app:/oauth2redirect/provider` (scheme = **reverse domain name** under the app's control; **only a single slash** after the scheme; scheme contains a `.`)
- [ ] Validate / reject schemes that aren't reverse-domain based (no period) on the client side
- [ ] Provide a way for the host app to hand the incoming URI to the library (library cannot register OS-level schemes itself)

### Claimed `https` redirect (§7.2)
- [ ] Support `https://app.example.com/path` redirect URIs where the host app forwards the claimed URL to the library (SHOULD be preferred where the platform supports it)

## Security (§8)
- [ ] **PKCE** (§8.1): generate `code_verifier` with a CSPRNG (43–128 chars, unreserved charset), `code_challenge_method=S256` (see RFC 7636), send `code_verifier` with the token request
- [ ] **`state`** (§8.9): high-entropy secure random value in every authorization request; **reject any response whose state doesn't match a pending request**
- [ ] **Mix-up mitigation** (§8.10): if the app talks to more than one AS, use a **unique redirect URI per AS** (e.g., differing path) and **store the redirect URI with the pending session data**; **reject responses received on a redirect URI that doesn't exactly match the one in the outgoing request** (MUST)
- [ ] Keep per-request session state (state, verifier, redirect URI, AS) together and expire/clean it up
- [ ] Don't let the "fake external user-agent" pattern slip in (no custom browser windows posing as external UAs) (§8.7)
- [ ] Don't implement client impersonation workarounds: never hard-code a shared client secret to "authenticate" (§8.5, §8.6)

