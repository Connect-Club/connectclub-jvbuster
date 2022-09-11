package com.connectclub.jvbuster.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.MDC;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.StringUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class JWTAuthorizationFilter extends BasicAuthenticationFilter {

    public static final String BEARER_TOKEN_PREFIX = "Bearer ";
    public static final String AUTHORIZATION_HEADER = "authorization";
    public static final String GUEST_ENDPOINT_HEADER = "guest-endpoint";

    private final RSAPublicKey rsaPublicKey;

    public JWTAuthorizationFilter(AuthenticationManager authManager, String publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        super(authManager);
        byte[] decoded = Base64.getDecoder().decode(
                publicKey.replace("-----BEGIN PUBLIC KEY-----", "")
                        .replaceAll(System.lineSeparator(), "")
                        .replace("-----END PUBLIC KEY-----", "")
        );
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        rsaPublicKey = (RSAPublicKey) kf.generatePublic(spec);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String token = request.getHeader(AUTHORIZATION_HEADER);

        VideobridgeAuthenticationToken authentication = null;
        if (token != null && token.startsWith(BEARER_TOKEN_PREFIX)) {
            try {
                authentication = getAuthentication(token, request.getHeader(GUEST_ENDPOINT_HEADER));
            } catch (JWTVerificationException e) {
                logger.info("JWT verification exception", e);
            }
        }

        if (authentication != null) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            MDC.put("endpoint", authentication.getEndpoint());
            MDC.put("conferenceGid", authentication.getConferenceGid());
        }
        try {
            chain.doFilter(request, response);
        } finally {
            if (authentication != null) {
                MDC.remove("endpoint");
                MDC.remove("conferenceGid");
            }
        }
    }

    private VideobridgeAuthenticationToken getAuthentication(String token, String guestEndpoint) {
        DecodedJWT decodedJWT = JWT.require(Algorithm.RSA256(rsaPublicKey, null))
                .build()
                .verify(token.substring(BEARER_TOKEN_PREFIX.length()));

        if (decodedJWT != null) {
            boolean guest = false;
            String endpoint;
            if (StringUtils.hasText(guestEndpoint)) {
                endpoint = guestEndpoint;
                guest = true;
            } else {
                Claim endpointClaim = decodedJWT.getClaim("endpoint");
                if (endpointClaim.isNull()) {
                    throw new InvalidClaimException("The Claim 'endpoint' value is null.");
                }
                endpoint = endpointClaim.as(String.class);
            }

            Claim conferenceGidClaim = decodedJWT.getClaim("conferenceGid");
            if (conferenceGidClaim.isNull()) {
                throw new InvalidClaimException("The Claim 'conferenceGid' value is null.");
            }
            return new VideobridgeAuthenticationToken(guest, endpoint, conferenceGidClaim.as(String.class));
        }
        return null;
    }
}
