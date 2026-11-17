module net.nikomitk.ssoauth2.pubclient {
    
    requires transitive net.nikomitk.ssoauth2;
    requires static lombok;
    requires static org.jetbrains.annotations;
    requires transitive java.net.http;
    requires jdk.httpserver;

    exports net.nikomitk.ssoauth2.pubclient;
    exports net.nikomitk.ssoauth2.pubclient.authcodeflow;
    exports net.nikomitk.ssoauth2.pubclient.userpasswordflow;
}
