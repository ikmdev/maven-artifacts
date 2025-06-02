package dev.ikm.maven.toolkit.isolated.entity;

import java.io.Serializable;

/**
 * A record that captures the name of a Mojo field and it's object instance
 * @param name
 * @param object
 */
public record IsolatedField(String name, Object object) implements Serializable {
	private static final long serialVersionUID = 1L;
}
