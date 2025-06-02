package dev.ikm.maven.toolkit.isolated.entity;

import java.time.Instant;

/**
 * A simple record that captures when and what a process is logging
 * @param message
 * @param instant
 */
public record LogInstant(String message, Instant instant) {
}
