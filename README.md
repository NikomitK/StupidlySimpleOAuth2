[![Maven Central Version](https://img.shields.io/maven-central/v/net.nikomitk/ssoauth2-client)](https://central.sonatype.com/artifact/net.nikomitk/ssoauth2-client)
# Stupidly Simple OAuth2

This is my personal OAuth2 library for use in locally installed java applications.
It is designed to be as simple as possible. 
The only dependency is on [FasterXML/jackson](https://github.com/FasterXML/jackson) for JSON parsing.

## Why this exists

I wanted to enable an application I developed to use OAuth2 and I couldn't find a slim and easy to use library.
That's why I created my own.

## Usage

1. Include the dependency in your pom (or the equivalent for your build tool):
```xml
<dependency>
    <groupId>net.nikomitk</groupId>
    <artifactId>ssoauth2-client</artifactId>
    <version>0.1</version>
</dependency>
```

2. Create an instance of a flow and initialize it:

```java
import net.nikomitk.ssoauth2.OAuth2Flow;
import net.nikomitk.ssoauth2.flows.OAuth2BrowserFlow;
import net.nikomitk.ssoauth2.security.PKCE;

OAuth2Flow flow = new OAuth2BrowserFlow("https://sso.example.com/.well-known/openid-configuration", "client-id", "openid profile email", PKCE.S256);
flow.startFlow(); // Initialization
```

3. Execute the needed steps as often as you need (you can store the flow instance in order to periodically refresh your token:

```java
flow.requestAccessToken(); // Request the access token from the authorization server

flow.refreshAccessToken(); // Refresh the access token in a way suitable for your chosen flow

flow.getAccessToken(); // Getter for the access token stored in the flow
```

For simpler use, you can also chain these methods:

```java
flow.requestAccessToken().refreshAccessToken().getAccessToken(); // Getter for the access token stored in the flow
```

## Available flows

Currently, the following flows are available:

 - OAuth2AuthCodeFlow ([Section 4.1 of RFC6749](https://datatracker.ietf.org/doc/html/rfc6749#section-4.1), optionally with [PKCE](https://datatracker.ietf.org/doc/html/rfc7636))
   - Starts an http server on a random port (or the one passed to `startFlow(int port)`) with redirection url of "http://127.0.0.1:&lt;port&gt;/oauth2/redirect". The login URL the user has to open is returned by `getAuthUri()`.
 - OAuth2BrowserFlow
   - Same as OAuth2AuthCodeFlow, but opens the login URL in the users default browser automatically.
 - OAuth2UserPasswordFlow ([Section 4.3 of RFC6749](https://datatracker.ietf.org/doc/html/rfc6749#section-4.3))
   - Gets an access token directly using a username and password. [Should not be used](https://datatracker.ietf.org/doc/html/rfc9700#section-2.4) in production. `refreshAccessToken()` requests a new token with the same credentials.

I plan to add more in the future, but I currently don't have enough time.
Contributions are welcome.