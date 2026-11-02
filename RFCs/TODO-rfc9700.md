# RFC 9700 (OAuth 2.0 Security BCP)

Legend: `[x]` met · `[~]` partially met · `[ ]` not met · `[-]` not applicable / out of scope (checked against code on `develop`)

Files: `AC` = [OAuth2AuthCodeFlow.java](../src/main/java/net/nikomitk/ssoauth2/flows/OAuth2AuthCodeFlow.java), `EP` = [OAuth2Endpoint.java](../src/main/java/net/nikomitk/ssoauth2/dto/OAuth2Endpoint.java)

## Redirect-based flow protection (§2.1)
- [x] Only one fixed redirect URI is used; exact matching is up to the AS (port is the only variable part)
- [x] No open redirector in the loopback handler
- [ ] **CSRF protection (MUST)** – no `state`; PKCE is optional
  - [ ] PKCE-as-CSRF only if AS support is verified – `EP.codeChallengeMethodsSupported` is parsed but never consulted
  - [ ] Always send `state` and PKCE
  - [-] `state` carrying application data – no `state`
  - [ ] Invalidate `state` after first use
- [ ] **Mix-up defense** (needed when talking to several AS)
  - [ ] `iss` response parameter (RFC 9207) validation – not implemented; `authorization_response_iss_parameter_supported` not parsed
  - [~] Issuer stored per authorization request – `OAuth2Endpoint.issuer` is held by the flow instance (one AS per flow), but not bound/compared to the response
  - [~] Distinct redirect URI per issuer – each flow has its own random port; not an explicit check
  - [x] Issuer identifies the endpoint tuple – the flow holds the full `OAuth2Endpoint`
- [~] `http` only for loopback redirects – redirect is loopback `http` (fine), but **no check that authorization/token/well-known endpoints use `https`**

## Authorization code grant (§2.1.1)
- [~] **Public clients MUST use PKCE** – supported, but opt-out via `null` (Javadoc: "not recommended")
- [x] PKCE challenge is transaction-specific – new verifier on each `buildUrl` (`AC:223`)
- [~] `S256` – supported and recommended in README; `PLAIN` is allowed and not rejected
- [x] `code_verifier` sent in the token request
- [ ] Detect PKCE support of the AS before relying on it
- [x] OIDC `nonce` is not used as a PKCE substitute
- [-] Validate `nonce` in ID token – no ID token handling at all (library doesn't parse ID tokens)

## Implicit grant (§2.1.2)
- [x] `response_type=token` not implemented

## Token replay prevention (§2.2)
- [~] **Refresh tokens: rotation** – a new refresh token replaces the old one (`AC:167`); but when the AS returns no new token the stored one becomes `null` (breaks non-rotating servers)
  - [ ] Atomic / thread-safe refresh – `refreshAccessToken()` and the fields are unsynchronized; concurrent refreshes can use an already-rotated token
  - [ ] `invalid_grant` on refresh → drop tokens / require re-authorization – not handled
  - [ ] (Optional) DPoP (RFC 9449)
  - [-] (Optional) mTLS

## Access token privilege restriction (§2.3)
- [x] Minimal scope – caller-controlled
- [ ] (Optional) Resource indicators (RFC 8707)
- [ ] Documentation that tokens must only be sent to their intended resource servers

## Grants that must not be used (§2.4)
- [ ] **ROPC grant is implemented** (`OAuth2UserPasswordFlow`, README lists it) – violates "MUST NOT be used". Only mitigated by a Javadoc/README warning; consider removing or marking `@Deprecated`

## Client authentication (§2.5)
- [x] No client authentication, no embedded secrets

## Metadata & configuration (§2.6)
- [x] Uses AS metadata (well-known endpoint) for `authorization_endpoint`, `token_endpoint`, `issuer`, `code_challenge_methods_supported`
  - [ ] `issuer` in the metadata is not validated against the URL it was fetched from
  - [ ] No manual endpoint configuration – `OAuth2Endpoint` can only be created via `queryWellKnownEndpoint`
- [~] TLS: JVM defaults (certificate validation on), but no enforced minimum protocol and no `https` requirement
- [ ] Reject `http` for authorization, token and metadata endpoints

## Attack-specific client requirements (§4)
- [x] §4.1.3 No wildcard/pattern redirect URIs
- [~] §4.2.4 Redirect page: no third-party resources or links ✔, but no `Referrer-Policy: no-referrer` header (`Util.sendResponse` sends no headers); no `state` to invalidate
- [x] §4.3.2 Library never puts access tokens in URIs
- [~] §4.4.2 Mix-up – see above
- [~] §4.5 Code injection – PKCE only if enabled
- [ ] §4.7.1 CSRF – see above
- [~] §4.8 PKCE downgrade – `code_challenge` is sent when `pkce != null`, but PKCE can be disabled and AS support isn't detected
- [x] §4.11.1 No open redirectors
- [~] §4.14 Refresh token protection – tokens held in memory only; no handling for server-side expiry/revocation (`invalid_grant`)
- [~] §4.9.3 Tokens as secrets – not logged and not persisted, but `OAuth2AccessToken` is a `record` whose generated `toString()` prints the token; `OAuth2RequestException` embeds the raw response body in its message
