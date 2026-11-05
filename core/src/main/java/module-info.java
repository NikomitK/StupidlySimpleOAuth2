@SuppressWarnings("module")
module net.nikomitk.ssoauth2 {
    requires com.fasterxml.jackson.annotation;
    requires java.net.http;
    requires jdk.httpserver;
    requires com.fasterxml.jackson.databind;
    requires static lombok;
    requires java.desktop;

    exports net.nikomitk.ssoauth2;
    exports net.nikomitk.ssoauth2.flows;
    exports net.nikomitk.ssoauth2.dto;
    exports net.nikomitk.ssoauth2.security;
    exports net.nikomitk.ssoauth2.exception;
    exports net.nikomitk.ssoauth2.states;
    exports net.nikomitk.ssoauth2.flows.types;
}