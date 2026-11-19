# Roadmap

Here are some of the things I have planned for the future of this library.

- [ ] confidential-client module
- [ ] more grant types for the public-client module
  - [ ] device code
- [x] automatically refresh if expired
- [ ] properly expose more customization possibilities
  - [x] used httpclient
  - [ ] logging
    - [ ] decide if slf4j or java system logger
  - [x] user agent
  - [x] timeouts
- [ ] token revocation
  - [ ] automatically on reset()
  - [ ] externally callable method in flow
- [ ] persistence across restarts
  - [ ] interface for persisting refresh tokens/other secrets/flow-state.
  - [ ] implementation of that interface in extra library
  - [ ] method for restoring flows from persisted state
- [ ] redirect customization
  - [ ] customizable html for redirect page
  - [ ] customizable redirect urls
- [ ] possibility to provide auth code from consuming projects web server
- [x] proper oauth2 error handling
  - [x] handling of error codes in http requests towards auth server
  - [x] handling of error codes in redirect
- [ ] oidc support
- [ ] more testing
  - [ ] mutation testing
  - [ ] better coverage
  - [ ] maybe test-containers (or similar) setup with real authorization server(s)
