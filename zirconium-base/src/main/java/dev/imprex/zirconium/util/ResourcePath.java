package dev.imprex.zirconium.util;

import java.util.regex.Pattern;

import com.google.common.base.Preconditions;

/**
 * Modified version of spigot's NamespacedKey
 */
public class ResourcePath {

	public static final String MINECRAFT = "minecraft";

	private static final Pattern VALID_NAMESPACE = Pattern.compile("[a-z0-9._-]+");
	private static final Pattern VALID_PATH = Pattern.compile("[a-z0-9/._-]+");

	public static ResourcePath fromString(String string) {
		Preconditions.checkArgument(string != null && !string.isEmpty(), "Input string must not be empty or null");
		String[] components = string.split(":", 3);
		if (components.length > 2) {
			return null;
		} else {
			String path = components.length == 2 ? components[1] : "";
			String namespace;
			if (components.length == 1) {
				namespace = components[0];
				if (!namespace.isEmpty() && VALID_PATH.matcher(namespace).matches()) {
					return minecraft(namespace);
				} else {
					return null;
				}
			} else if (components.length == 2 && !VALID_PATH.matcher(path).matches()) {
				return null;
			} else {
				namespace = components[0];
				if (namespace.isEmpty()) {
					return minecraft(path);
				} else {
					return !VALID_PATH.matcher(namespace).matches() ? null : new ResourcePath(namespace, path);
				}
			}
		}
	}

	public static ResourcePath minecraft(String path) {
		return new ResourcePath("minecraft", path);
	}

	private final String namespace;
	private final String path;

	private ResourcePath(String namespace, String path) {
		Preconditions.checkArgument(namespace != null && VALID_NAMESPACE.matcher(namespace).matches(),
				"Invalid namespace. Must be [a-z0-9._-]: %s", namespace);
		Preconditions.checkArgument(path != null && VALID_PATH.matcher(path).matches(),
				"Invalid path. Must be [a-z0-9/._-]: %s", path);

		this.namespace = namespace;
		this.path = path;

		String string = this.toString();
		Preconditions.checkArgument(string.length() < 256, "ResourcePath must be less than 256 characters", string);
	}

	public String namespace() {
		return this.namespace;
	}

	public String path() {
		return this.path;
	}

	public int hashCode() {
		int hash = 5;
		hash = 47 * hash + this.namespace.hashCode();
		hash = 47 * hash + this.path.hashCode();
		return hash;
	}

	public boolean equals(Object obj) {
		return this == obj || (obj instanceof ResourcePath other && this.namespace.equals(other.namespace)
				&& this.path.equals(other.path));
	}

	public String toString() {
		return this.namespace + ":" + this.path;
	}
}
