module net.nikomitk.ssoauth2.pubclient {
    
    requires net.nikomitk.ssoauth2;
    requires static lombok;
    requires java.net.http;
    requires jdk.httpserver;

    exports net.nikomitk.ssoauth2.pubclient;
    exports net.nikomitk.ssoauth2.pubclient.authcodeflow;
    exports net.nikomitk.ssoauth2.pubclient.userpasswordflow;
}
