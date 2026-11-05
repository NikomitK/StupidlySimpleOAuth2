# RFC 8252 (OAuth 2.0 for Native Apps)

Legend: `[x]` met · `[~]` partially met · `[ ]` not met · `[-]` not applicable / out of scope (checked against code on `develop`)

Files: `AC` = [OAuth2AuthCodeFlow.java](../src/main/java/net/nikomitk/ssoauth2/flows/OAuth2AuthCodeFlow.java), `Util` = [Util.java](../src/main/java/net/nikomitk/ssoauth2/util/Util.java)

## Client type & registration (§8.4, §8.5)

- [x] Public client, no client secret used
- [x] Doesn't require a statically embedded client secret
- [x] Complete redirect URI with fixed path (`/oauth2/redirect`); only the port varies
- [ ] Platform-specific identity info (package/bundle name) – not supported (optional)

## External user-agent (§4, §5, §6, §8.12)

- [x] External user-agent (system browser) used – `OAuth2BrowserFlow` → `Util.openBrowser`
- [x] No embedded user-agent / web-view
- [~] Opens default browser: `Desktop.browse`, fallback `xdg-open` on Linux only; the `xdg-open` exit code isn't checked
- [~] Pluggable browser-launcher SPI – none, but `OAuth2AuthCodeFlow.getAuthUri()` lets callers open the URL themselves
- [~] Headless handling – `OAuth2BrowserFlow` throws a bare `RuntimeException` wrapping `UnsupportedOperationException`; the URL is available via `getAuthUri()` but the server is already started and isn't stopped on this failure

## Authorization request (§6)

- [x] Authorization code grant with a receivable (loopback) redirect URI
- [~] **MUST implement PKCE** – implemented (`PKCE` enum) but optional: `pkce == null` disables it
- [x] Implicit flow not used

## Receiving the authorization response (§7)

### Loopback interface redirect (§7.3, §8.3)

- [~] Redirect URI form – `http://<InetAddress.getLoopbackAddress().getHostAddress()>:<port>/oauth2/redirect` (`AC:207-211`). Normally `127.0.0.1`; if the JVM prefers IPv6 the host would be rendered as an unbracketed `0:0:0:0:0:0:0:1`, which is an invalid URI
- [x] Ephemeral port from the OS (`startFlow()` binds port `0`), and explicit port via `startFlow(int)`
- [ ] Try binding both IPv4 and IPv6 – only one loopback address
- [x] Bound to loopback only (`Util.createHttpServer`)
- [x] IP literal rather than `localhost`
- [~] Port open only during the request – closed after the first redirect (`AC:199`) and on timeout/interruption in `requestAccessToken()` (`AC:107`), but it stays open indefinitely if the flow is started and `requestAccessToken()` is never called, or if `Util.openBrowser` fails
- [ ] No `SO_REUSEADDR`/`SO_REUSEPORT` (Linux/macOS), `SO_EXCLUSIVEADDRUSE` (Windows) – socket options aren't configurable via `com.sun.net.httpserver.HttpServer` (JDK `ServerSocket` defaults apply); would require a custom socket server
- [~] Path must match exactly – `createContext("/oauth2/redirect")` is a **prefix** match, so e.g. `/oauth2/redirect/anything` also triggers the handler
- [x] "You can close this window" page without third-party content

### Private-use URI scheme redirect (§7.1)

- [ ] Not supported
- [ ] Not supported (reverse-domain scheme validation)
- [ ] No API for the host app to pass an incoming redirect URI to the library

### Claimed `https` redirect (§7.2)

- [ ] Not supported

## Security (§8)

- [~] **PKCE** – verifier from `SecureRandom`, 32 bytes → 43 chars, `S256` supported and sent with the token request; but optional, and `PLAIN` is allowed (Javadoc warns about it)
- [ ] **`state`** – not generated, not validated
- [~] Mix-up mitigation (§8.10) – each flow instance talks to exactly one AS and has its own loopback server/redirect URI (`lastRedirectUri` is stored); no explicit check that the response arrived on the matching URI, and nothing across multiple flows
- [~] Per-request session state kept together – held in flow instance fields (`lastCodeVerifier`, `lastRedirectUri`, …), but calling `startFlow()` twice overwrites them without cleanup
- [x] No fake external user-agents
- [x] No hard-coded shared secret

