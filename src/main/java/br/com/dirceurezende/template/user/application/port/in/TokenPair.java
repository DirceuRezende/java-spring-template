package br.com.dirceurezende.template.user.application.port.in;

public record TokenPair(String accessToken, String refreshToken, long expiresInSeconds) {

    @Override
    public String toString() {
        return "TokenPair[expiresInSeconds=" + expiresInSeconds + "]";
    }
}
