package dev.imprex.zirconium.gui.font.providers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import dev.imprex.zirconium.gui.font.glyphs.GlyphInfo;
import dev.imprex.zirconium.service.ResourceProvider;

public final class SpaceProvider implements GlyphProvider {

	private final Map<Integer, GlyphInfo> glyphs;

	private SpaceProvider(Map<Integer, Float> advances) {
		this.glyphs = new HashMap<>(advances.size());
		advances.forEach((key, value) ->
				this.glyphs.put(key, () -> value));
	}

	@Override
	public GlyphInfo getGlyph(int codepoint) {
		return this.glyphs.get(codepoint);
	}

	public Set<Integer> getSupportedGlyphs() {
		return this.glyphs.keySet();	
	}

	static record Definition(Map<String, Float> advances) implements GlyphProviderDefinition {

		@Override
		public GlyphProviderType type() {
			return GlyphProviderType.SPACE;
		}

		@Override
		public GlyphProvider load(ResourceProvider provider) throws IOException {
			Map<Integer, Float> advances = new HashMap<>();

			for (Map.Entry<String, Float> entry : advances().entrySet()) {
				advances.put(entry.getKey().codePointAt(0), entry.getValue());
			}

			return new SpaceProvider(advances);
		}
	}
}
