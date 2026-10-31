@SuppressWarnings("module")
module net.nikomitk.ssoauth2 {
    requires com.fasterxml.jackson.annotation;
    requires java.net.http;
    requires jdk.httpserver;
    requires com.fasterxml.jackson.databind;
    requires static lombok;
    requires java.desktop;
    requires java.naming;

    exports net.nikomitk.ssoauth2;
    exports net.nikomitk.ssoauth2.flows;
    exports net.nikomitk.ssoauth2.dto;
    exports net.nikomitk.ssoauth2.security;
}