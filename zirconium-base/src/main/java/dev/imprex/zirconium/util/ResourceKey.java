package dev.imprex.zirconium.util;

import java.util.regex.Pattern;

import com.google.common.base.Preconditions;

/**
 * Modified version of spigot's NamespacedKey
 */
public class ResourceKey {

	public static final String MINECRAFT = "minecraft";

	private static final Pattern VALID_NAMESPACE = Pattern.compile("[a-z0-9._-]+");
	private static final Pattern VALID_KEY = Pattern.compile("[a-z0-9/._-]+");

	public static ResourceKey fromString(String string) {
		Preconditions.checkArgument(string != null && !string.isEmpty(), "Input string must not be empty or null");
		String[] components = string.split(":", 3);
		if (components.length > 2) {
			return null;
		} else {
			String key = components.length == 2 ? components[1] : "";
			String namespace;
			if (components.length == 1) {
				namespace = components[0];
				if (!namespace.isEmpty() && VALID_KEY.matcher(namespace).matches()) {
					return minecraft(namespace);
				} else {
					return null;
				}
			} else if (components.length == 2 && !VALID_KEY.matcher(key).matches()) {
				return null;
			} else {
				namespace = components[0];
				if (namespace.isEmpty()) {
					return minecraft(key);
				} else {
					return !VALID_KEY.matcher(namespace).matches() ? null : new ResourceKey(namespace, key);
				}
			}
		}
	}

	public static ResourceKey minecraft(String key) {
		return new ResourceKey("minecraft", key);
	}

	private final String namespace;
	private final String key;

	private ResourceKey(String namespace, String key) {
		Preconditions.checkArgument(namespace != null && VALID_NAMESPACE.matcher(namespace).matches(),
				"Invalid namespace. Must be [a-z0-9._-]: %s", namespace);
		Preconditions.checkArgument(key != null && VALID_KEY.matcher(key).matches(),
				"Invalid key. Must be [a-z0-9/._-]: %s", key);

		this.namespace = namespace;
		this.key = key;

		String string = this.toString();
		Preconditions.checkArgument(string.length() < 256, "NamespacedKey must be less than 256 characters", string);
	}

	public String getNamespace() {
		return this.namespace;
	}

	public String getKey() {
		return this.key;
	}

	public int hashCode() {
		int hash = 5;
		hash = 47 * hash + this.namespace.hashCode();
		hash = 47 * hash + this.key.hashCode();
		return hash;
	}

	public boolean equals(Object obj) {
		return this == obj || (obj instanceof ResourceKey other && this.namespace.equals(other.namespace)
				&& this.key.equals(other.key));
	}

	public String toString() {
		return this.namespace + ":" + this.key;
	}
}
