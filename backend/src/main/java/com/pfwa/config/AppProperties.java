package com.pfwa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the application.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Security security = new Security();
    private final RateLimit rateLimit = new RateLimit();
    private final Email email = new Email();
    private String frontendUrl;

    public Jwt getJwt() {
        return jwt;
    }

    public Security getSecurity() {
        return security;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public Email getEmail() {
        return email;
    }

    public String getFrontendUrl() {
        return frontendUrl;
    }

    public void setFrontendUrl(String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    public static class Jwt {
        private String secret;
        private int accessTokenExpirationMinutes = 15;
        private int refreshTokenExpirationDays = 7;
        private int refreshTokenRememberMeDays = 30;
        private String issuer = "pfwa";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public int getAccessTokenExpirationMinutes() {
            return accessTokenExpirationMinutes;
        }

        public void setAccessTokenExpirationMinutes(int accessTokenExpirationMinutes) {
            this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
        }

        public int getRefreshTokenExpirationDays() {
            return refreshTokenExpirationDays;
        }

        public void setRefreshTokenExpirationDays(int refreshTokenExpirationDays) {
            this.refreshTokenExpirationDays = refreshTokenExpirationDays;
        }

        public int getRefreshTokenRememberMeDays() {
            return refreshTokenRememberMeDays;
        }

        public void setRefreshTokenRememberMeDays(int refreshTokenRememberMeDays) {
            this.refreshTokenRememberMeDays = refreshTokenRememberMeDays;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }
    }

    public static class Security {
        private int bcryptStrength = 12;
        private int maxFailedLoginAttempts = 5;
        private int accountLockoutMinutes = 30;
        private int maxSessionsPerUser = 5;
        private boolean secureCookies = true;
        private String cookieSameSite = "Strict";

        public int getBcryptStrength() {
            return bcryptStrength;
        }

        public void setBcryptStrength(int bcryptStrength) {
            this.bcryptStrength = bcryptStrength;
        }

        public int getMaxFailedLoginAttempts() {
            return maxFailedLoginAttempts;
        }

        public void setMaxFailedLoginAttempts(int maxFailedLoginAttempts) {
            this.maxFailedLoginAttempts = maxFailedLoginAttempts;
        }

        public int getAccountLockoutMinutes() {
            return accountLockoutMinutes;
        }

        public void setAccountLockoutMinutes(int accountLockoutMinutes) {
            this.accountLockoutMinutes = accountLockoutMinutes;
        }

        public int getMaxSessionsPerUser() {
            return maxSessionsPerUser;
        }

        public void setMaxSessionsPerUser(int maxSessionsPerUser) {
            this.maxSessionsPerUser = maxSessionsPerUser;
        }

        public boolean isSecureCookies() {
            return secureCookies;
        }

        public void setSecureCookies(boolean secureCookies) {
            this.secureCookies = secureCookies;
        }

        public String getCookieSameSite() {
            return cookieSameSite;
        }

        public void setCookieSameSite(String cookieSameSite) {
            this.cookieSameSite = cookieSameSite;
        }
    }

    public static class RateLimit {
        private final RateLimitConfig registration = new RateLimitConfig();
        private final RateLimitConfig login = new RateLimitConfig();
        private final RateLimitConfig passwordReset = new RateLimitConfig();
        private final RateLimitConfig emailResend = new RateLimitConfig();

        public RateLimitConfig getRegistration() {
            return registration;
        }

        public RateLimitConfig getLogin() {
            return login;
        }

        public RateLimitConfig getPasswordReset() {
            return passwordReset;
        }

        public RateLimitConfig getEmailResend() {
            return emailResend;
        }
    }

    public static class RateLimitConfig {
        private int maxAttempts = 5;
        private int windowMinutes = 1;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public int getWindowMinutes() {
            return windowMinutes;
        }

        public void setWindowMinutes(int windowMinutes) {
            this.windowMinutes = windowMinutes;
        }
    }

    public static class Email {
        private String from;
        private String fromName;
        private int verificationExpirationHours = 24;
        private int passwordResetExpirationHours = 1;

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getFromName() {
            return fromName;
        }

        public void setFromName(String fromName) {
            this.fromName = fromName;
        }

        public int getVerificationExpirationHours() {
            return verificationExpirationHours;
        }

        public void setVerificationExpirationHours(int verificationExpirationHours) {
            this.verificationExpirationHours = verificationExpirationHours;
        }

        public int getPasswordResetExpirationHours() {
            return passwordResetExpirationHours;
        }

        public void setPasswordResetExpirationHours(int passwordResetExpirationHours) {
            this.passwordResetExpirationHours = passwordResetExpirationHours;
        }
    }
}
