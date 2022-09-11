package com.connectclub.jvbuster.security;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.List;

public class VideobridgeAuthenticationToken extends AbstractAuthenticationToken {

    @Getter
    private final boolean guest;

    @Getter
    private final String endpoint;

    @Getter
    private final String conferenceGid;

    public VideobridgeAuthenticationToken(boolean guest, String endpoint, String conferenceGid) {
        super(List.of());
        this.guest = guest;
        this.endpoint = endpoint;
        this.conferenceGid = conferenceGid;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return endpoint;
    }
}
