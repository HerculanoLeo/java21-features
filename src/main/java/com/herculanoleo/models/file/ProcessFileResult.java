package com.herculanoleo.models.file;

import java.io.File;
import java.util.Objects;

public record ProcessFileResult(Boolean success, File file, String hash, Long duration) {
    @Override
    public String toString() {
        return String.format("%s|SHA256:%s|%s|%s|%s", success, hash, file.getAbsolutePath(), file.length(), duration);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessFileResult that = (ProcessFileResult) o;
        return Objects.equals(success, that.success) && Objects.equals(file, that.file) && Objects.equals(hash, that.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, file, hash);
    }
}
