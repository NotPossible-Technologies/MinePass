package io.github.duckycashy.minepass.model;

public final class PasswordEntry {

    private final String password;
    private final PasswordType type;
    private final long createdAt;
    private final Long expiresAt;

    public PasswordEntry(
            String password,
            PasswordType type,
            long createdAt,
            Long expiresAt
    ) {
        this.password = password;
        this.type = type;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getPassword() {
        return password;
    }

    public PasswordType getType() {
        return type;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return type == PasswordType.TEMPORARY
                && expiresAt != null
                && System.currentTimeMillis() >= expiresAt;
    }
}