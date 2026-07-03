module net.nikomitk.ssoauth2 {
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires java.net.http;
    requires jdk.httpserver;
    requires com.fasterxml.jackson.databind;
    requires static lombok;

    exports net.nikomitk.ssoauth2;
    exports  net.nikomitk.ssoauth2.flows;
    exports net.nikomitk.ssoauth2.dto;
}