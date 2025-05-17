package dev.ikm.maven.toolkit.entity;

import java.io.Serializable;

public record IsolatedField(String name, Object object) implements Serializable {
	private static final long serialVersionUID = 1L;
}
