package dev.imprex.zirconium.gui.font.providers;

import java.util.HashMap;
import java.util.Map;

public enum GlyphProviderType {

	BITMAP("bitmap", BitmapProvider.Definition.class),
	SPACE("space", SpaceProvider.Definition.class),
	UNIHEX("unihex", UnihexProvider.Definition.class),
	REFERENCE("reference", ReferenceProvider.Defintion.class);

	private static final Map<String, GlyphProviderType> LOOKUP = new HashMap<>();

	static {
		for (GlyphProviderType type : values()) {
			LOOKUP.put(type.getName(), type);
		}
	}

	public static GlyphProviderType fromName(String name) {
		GlyphProviderType type = LOOKUP.get(name);
		if (type == null) {
			throw new IllegalArgumentException("unknown glyph type: " + name);
		}
		return type;
	}

	private final String name;
	private final Class<? extends GlyphProviderDefinition> type;

	private GlyphProviderType(String name, Class<? extends GlyphProviderDefinition> type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return this.name;
	}

	public Class<? extends GlyphProviderDefinition> getType() {
		return type;
	}
}
