package com.herculanoleo.models.file;

import java.io.File;

public record ProcessFileResult(Boolean success, File file, String hash, Long duration) {
    @Override
    public String toString() {
        return String.format("%s|SHA256:%s|%s|%s|%s", success, hash, file.getAbsolutePath(), file.length(), duration);
    }
}
