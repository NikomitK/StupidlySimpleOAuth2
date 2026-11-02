# RFC 9700 (OAuth 2.0 Security BCP) 

## Redirect-based flow protection (§2.1)
- [ ] Use only **exact, pre-registered redirect URIs** (compare by simple string comparison, RFC 3986 §6.2.1); only the **port** may vary for loopback redirects (§2.1, §4.1.3)
- [ ] Client **MUST NOT expose open redirectors** (e.g. the loopback/redirect handler must never forward the browser to a URL taken from a query parameter) (§2.1, §4.11.1)
- [ ] **CSRF protection is MUST** (§2.1, §4.7.1)
  - [ ] Use PKCE for CSRF protection **only if** the AS is known to support PKCE (check metadata `code_challenge_methods_supported` or configuration), **otherwise `state` MUST be used**
  - [ ] Simplest compliant approach: always send `state` **and** PKCE; one-time-use `state`, securely bound to the user agent/session
  - [ ] If `state` carries application data and integrity matters, protect it against tampering/swapping (sign/encrypt or bind to session)
  - [ ] **Invalidate `state` after first use** at the redirect endpoint (SHOULD, §4.2.4)
- [ ] **Mix-up defense is REQUIRED when the client can talk to more than one AS** (§2.1, §4.4)
  - [ ] Preferred (SHOULD): validate the **`iss` authorization response parameter** per RFC 9207 (compare to the stored issuer, **abort on mismatch**); also handle servers that don't send `iss` per the AS metadata flag `authorization_response_iss_parameter_supported`
  - [ ] **Store, per authorization request, the issuer the request was sent to and bind it to the user agent / session** (MUST for both defenses)
  - [ ] Fallback (MAY, SHOULD only if nothing else): **distinct redirect URI per issuer** and verify the response arrived on the matching one (see RFC 8252 §8.10)
  - [ ] Don't rely on only storing the AS URL; issuer must identify the (authorization endpoint, token endpoint) tuple
- [ ] Authorization responses are never transmitted over unencrypted channels; **`http` redirect URIs only for loopback** (§2.6)

## Authorization code grant (§2.1.1)
- [ ] **Public clients MUST use PKCE (RFC 7636)** (§2.1.1, §4.5.3.1)
- [ ] PKCE challenge **MUST be transaction-specific** (fresh `code_verifier` per authorization request) and bound to the client/user agent that started the transaction
- [ ] Use **`code_challenge_method=S256`** (SHOULD; never `plain` unless the AS leaves no option)
- [ ] Send `code_verifier` in the token request
- [ ] **Detect PKCE support of the AS** before relying on it (metadata `code_challenge_methods_supported`, RFC 8414, or configuration); refuse/warn if unsupported
- [ ] Don't use the OIDC `nonce` as a substitute for PKCE (doesn't protect public clients) (§4.5.3.2)
- [ ] If OIDC is supported: validate `nonce` in the ID token (from the token endpoint) and don't use tokens before it's checked (§4.5.3.2)

## Implicit grant & other token-in-redirect flows (§2.1.2)
- [ ] **Do not implement** `response_type=token` (SHOULD NOT) – use `code`

## Token replay prevention (§2.2)
- [ ] **Refresh tokens for public clients MUST be sender-constrained OR use refresh token rotation** (§2.2.2, §4.14.2)
  - [ ] Rotation: **replace the stored refresh token with the new one returned by every refresh** (RFC 6749 §6 MUST), and make this **atomic / thread-safe** (concurrent refreshes must not use an already-rotated token, and the new token must be persisted before the old is discarded/the access token handed out)
  - [ ] Handle `invalid_grant` on refresh (rotation breach detected / token revoked) → drop stored tokens and require re-authorization
  - [ ] (Optional/Future) DPoP (RFC 9449) support for sender-constraining access and refresh tokens (§2.2.1 SHOULD for AS/RS; DPoP fits public clients)
  - [ ] (Optional) mTLS (RFC 8705) is not realistic for typical public clients

## Access token privilege restriction (§2.3)
- [ ] Request **minimal scope** necessary (SHOULD)
- [ ] (Optional) Support Resource Indicators (`resource` parameter, RFC 8707) for audience restriction; (RFC 9396 `authorization_details` optional)
- [ ] Only send tokens to the resource servers they were meant for (don't send an access token to arbitrary URLs; callers' responsibility, document it) (§4.9.1)

## Grants that must not be used (§2.4)
- [ ] **Do NOT implement the Resource Owner Password Credentials grant** (MUST NOT be used)

## Client authentication (§2.5)
- [ ] Public clients don't authenticate; do **not** embed shared secrets as authentication. (Confidential-client methods like `private_key_jwt`/mTLS are out of scope)

## Metadata & configuration (§2.6)
- [ ] **Use OAuth Authorization Server Metadata (RFC 8414)** to configure endpoints when available (RECOMMENDED): `issuer`, `authorization_endpoint`, `token_endpoint`, `code_challenge_methods_supported`, `authorization_response_iss_parameter_supported`
  - [ ] Validate the `issuer` in the metadata document equals the URL it was retrieved from (RFC 8414)
  - [ ] Support manual endpoint configuration as fallback
- [ ] **Use TLS** (BCP 195: TLS ≥ 1.2, no 1.0/1.1) for all connections to AS and RS; validate certificates (RECOMMENDED end-to-end TLS to the RS)
- [ ] Reject `http` for authorization, token, and metadata endpoints (loopback redirect is the only exception)

## Attack-specific client requirements (§4)
- [ ] §4.1.3: Never use pattern/wildcard redirect URIs; exact matching only
- [ ] §4.2.4: Pages rendered at the redirect endpoint (loopback server response page) **SHOULD NOT include third-party resources or external links**; set `Referrer-Policy: no-referrer`; invalidate `state` after use
- [ ] §4.3.2: **Clients MUST NOT pass access tokens in a URI query parameter** (RFC 6750 §2.3) – use `Authorization: Bearer` header only
- [ ] §4.4.2: Mix-up countermeasures (see above)
- [ ] §4.5: Authorization code injection countermeasure = PKCE (see above)
- [ ] §4.7.1: CSRF countermeasures (see above)
- [ ] §4.8: PKCE downgrade (AS side), but client must **always send `code_challenge`** and never make PKCE optional / not detect AS lacking PKCE support
- [ ] §4.11.1: No open redirectors in the client's redirect handler
- [ ] §4.14: Refresh token protection – rotation handling, secure storage, tolerate AS-side inactivity expiry / revocation (password change, logout) → surface "re-authentication required"
- [ ] §4.9.3 (clients): Treat access tokens as sensitive secrets – **do not log them, do not store/transmit in plaintext** (secure storage SPI, no tokens in `toString`/logs/exception messages)

