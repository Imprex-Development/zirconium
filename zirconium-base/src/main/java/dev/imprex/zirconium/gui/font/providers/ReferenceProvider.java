package dev.imprex.zirconium.gui.font.providers;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import dev.imprex.zirconium.gui.font.glyphs.GlyphInfo;
import dev.imprex.zirconium.service.ResourceProvider;
import dev.imprex.zirconium.util.ResourcePath;

public class ReferenceProvider implements GlyphProvider {

	private final Map<Integer, GlyphInfo> glyphs;

	public ReferenceProvider(Map<Integer, GlyphInfo> glyphs) {
		this.glyphs = glyphs;
	}

	@Override
	public GlyphInfo getGlyph(int codepoint) {
		return this.glyphs.get(codepoint);
	}

	@Override
	public Set<Integer> getSupportedGlyphs() {
		return this.glyphs.keySet();
	}

	static record Defintion(ResourcePath id) implements GlyphProviderDefinition {

		@Override
		public GlyphProviderType type() {
			return GlyphProviderType.REFERENCE;
		}

		@Override
		public GlyphProvider load(ResourceProvider provider) throws IOException {
			return new ReferenceProvider(GlyphProvider.loadDefinitionFile(provider, id.withPrefix("font/").withSuffix(".json")));
		}
	}
}
