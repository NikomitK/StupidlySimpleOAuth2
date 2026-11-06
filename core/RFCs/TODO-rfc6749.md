# RFC 6749 (OAuth 2.0)

Legend: `[x]` met · `[~]` partially met · `[ ]` not met · `[-]` not applicable / out of scope (checked against code on `develop`)

Files: `AC` = [OAuth2AuthCodeFlow.java](../src/main/java/net/nikomitk/ssoauth2/flows/OAuth2AuthCodeFlow.java), `PubClientUtil` = [Util.java](../src/main/java/net/nikomitk/ssoauth2/util/Util.java), `UB` = [UrlBuilder.java](../src/main/java/net/nikomitk/ssoauth2/util/UrlBuilder.java)

## Client registration & identity (§2)

- [x] Client is configured as a **public** client type; library never uses a secret (only `clientID` is a constructor arg)
- [x] `client_id` is configurable and treated as an opaque string, no size assumptions
- [x] `client_id` is never used alone to authenticate the client (no client authentication at all)
- [~] Library only uses registered redirect URIs – fixed path `/oauth2/redirect` on `127.0.0.1`, only the port varies (`AC:207-211`); the user must register that path, which is not documented as such
- [-] Client-auth with multiple methods per request – no client authentication implemented

## Endpoints (§3)

- [ ] Authorization endpoint: **retain existing query component** – `UrlBuilder` always appends `"?"` (`UB:14`), producing `…?a=b?response_type=…` if the endpoint already has a query. No fragment check either
- [x] Token endpoint: used as-is via `URI.create`, nothing appended
- [~] Form-urlencoded, no duplicate parameters – token requests use `HttpFormBodyBuilder` (map, so unique, UTF-8). `UrlBuilder` does not encode keys and throws `NullPointerException` for a `null` value (e.g. `scope == null`, `UB:22`)
- [x] Unrecognized parameters ignored – Jackson `FAIL_ON_UNKNOWN_PROPERTIES=false` (`OAuth2Flow:26`); redirect handler only reads `code`
- [x] Token requests use POST
- [~] TLS: `java.net.http.HttpClient` validates certificates/host names by default, but **`https` is never enforced** on the authorization/token/well-known endpoints
- [x] Redirect URI is absolute, no fragment
- [x] Redirect page has no third-party content (plain text response, `AC:192`)

## Scope (§3.3)

- [ ] `scope` grammar validation on input
- [ ] Parse `scope` from token response – `OAuth2TokenResponse` has no `scope` field, granted scope is lost
- [ ] Treat missing `scope` as identical to requested – not applicable until the above exists
- [x] Minimal scope – scope is entirely caller-controlled

## Authorization Code Grant (§4.1)

### Authorization request (§4.1.1)

- [x] `response_type=code`
- [x] `client_id`
- [x] `redirect_uri` (always sent, `AC:218`)
- [~] `scope` – sent, but `null` scope crashes (see above)
- [ ] **`state`** – not generated or sent at all
- [x] Resource owner directed to the URL via external user agent (`OAuth2BrowserFlow` / `getAuthUri()`)
- [-] No sensitive data in `state` / `scope` – no `state`

### Authorization response (§4.1.2)

- [~] Parse `code` – works, but `Util.getQueryParameter` (`Util:46-59`) does not URL-decode, `NullPointerException` if the request has no query string, and `ArrayIndexOutOfBoundsException` for `code=` (empty) and wrong value for values containing `=`; on such exceptions the future is never completed so the caller only gets the 120 s timeout
- [ ] **Validate `state`**
- [~] Authorization code used only once – server is stopped after the first redirect, but `requestAccessToken()` can be called again with the same `futureAccessCode`
- [x] No assumptions about code length

### Error response (§4.1.2.1)

- [ ] Parse `error`, `error_description`, `error_uri` from the redirect – any request without `code` yields a generic HTTP 500 + `OAuth2FlowInterruptedException("No access code found…")`
- [ ] Map error codes
- [ ] Validate `state` on error responses
- [~] Surface errors in a typed way – generic exception; additionally `requestAccessToken()` converts any `ExecutionException`/timeout into `OAuth2FlowNotInitializedException`, which hides the real cause

### Access token request (§4.1.3)

- [x] POST with `grant_type=authorization_code`
- [x] `code`
- [x] `redirect_uri` identical to the authorization request (`lastRedirectUri`)
- [x] `client_id`
- [x] Content-Type `application/x-www-form-urlencoded`
- [~] PKCE `code_verifier` – sent only if `pkce != null`
- [~] Note: `scope` is also sent in the authorization_code token request (`AC:112`); not defined for this request by RFC 6749 §4.1.3 (harmless on most servers, but not spec-conformant)

## Implicit grant (§4.2)

- [x] Not implemented (README doesn't state it as a deliberate non-goal, though)

## Resource Owner Password Credentials (§4.3)

- [ ] **Implemented** (`OAuth2UserPasswordFlow`) although RFC 9700 §2.4 says MUST NOT be used; documented in Javadoc/README as "should not be used"

## Client Credentials (§4.4)

- [-] Not implemented (confidential only)

## Extension grants (§4.5)

- [~] `OAuth2Flow` is an extensible abstract base, but there is no generic `grant_type`/token-request extension point

## Access token response (§5.1)

- [x] Accepts HTTP 200 with JSON body (`OAuth2RequestException.check`)
- [~] Parses `access_token`, `token_type`, `expires_in`, `refresh_token`; `token_type` is stored but never checked; `scope` is not parsed
- [ ] Missing `expires_in` – `int expiresIn` defaults to `0`, so the token is considered expired immediately; no configurable default
- [x] Unrecognized members ignored
- [x] No size assumptions
- [x] Absolute expiry computed from time of receipt (`receivedAt + expiresIn`, `OAuth2Flow:57`)

## Error response (§5.2)

- [~] Non-200 is detected, but the JSON body is not parsed: `OAuth2RequestException` carries status code and raw body only; no `error`/`error_description`/`error_uri` fields
- [ ] Mapping of error codes
- [ ] `invalid_grant` on refresh treated as "re-authorization required"
- [-] Unknown error members – body is not parsed

## Refreshing an access token (§6)

- [x] POST `grant_type=refresh_token`, `refresh_token`, `scope`, `client_id`
- [x] Requested scope does not exceed the original (re-sends the same scope)
- [x] `client_id` included
- [x] If a new refresh token is returned, the old one is replaced (`AC:167`)
- [ ] **If no new refresh token is returned, keep the old one** – `AC:167` unconditionally assigns `getRefreshToken()`, which is `null` when the server doesn't rotate; the next refresh then throws `OAuth2FlowNotInitializedException`
- [~] Error handling – generic (see §5.2), no special `invalid_grant` handling
- [x] Response may omit `scope`

## Using access tokens (§7)

- [ ] Don't use tokens whose `token_type` isn't understood – not checked
- [-] `Authorization: Bearer` helper – library only returns the token (`getAccessToken()`); callers build requests themselves
- [x] Token is never sent in a URI by the library
- [-] Resource-server errors → refresh/re-auth – library doesn't talk to resource servers
- [-] Extension point for other token types

## Security considerations that bind the client (§10)

- [~] §10.3 Access token confidentiality – kept in memory only, never persisted; but TLS isn't enforced and `OAuth2AccessToken` is a `record`, so `toString()` prints the token
- [~] §10.4 Refresh token confidentiality – private in-memory field, no storage abstraction; TLS not enforced
- [~] §10.5 Code single use / TLS – see above
- [x] §10.6 `redirect_uri` included in token request
- [~] §10.8 Never in the clear – no scheme validation of endpoints
- [x] §10.9 TLS certificate verification – JVM defaults, no trust-all option
- [ ] §10.10 `state` generation – no `state` (PKCE verifier does use `SecureRandom`)
- [ ] §10.12 **CSRF protection of the redirection endpoint** – none unless PKCE is enabled and the server enforces it
- [~] §10.14 Input validation of redirect / token responses – see parsing issues above
- [x] §10.15 No open redirector – handler never redirects

