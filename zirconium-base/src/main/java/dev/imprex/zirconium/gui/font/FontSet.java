package dev.imprex.zirconium.gui.font;

import java.util.Map;

import javax.annotation.Nullable;

import dev.imprex.zirconium.gui.font.glyphs.GlyphInfo;
import dev.imprex.zirconium.gui.font.providers.GlyphProvider;
import dev.imprex.zirconium.service.ResourceProvider;
import dev.imprex.zirconium.util.ResourcePath;

public class FontSet {

	@Nullable
	public static FontSet fromPath(ResourceProvider provider, ResourcePath resourcePath) {
		return new FontSet(GlyphProvider.loadDefinitionFile(provider, resourcePath));
	}

	private final Map<Integer, GlyphInfo> glyphs;

	public FontSet(Map<Integer, GlyphInfo> glyphs) {
		this.glyphs = glyphs;
	}

	public float getStringWidth(String character) {
		float width = 0;

		for (int i = 0; i < character.length(); i++) {
			GlyphInfo glyph = this.glyphs.get(character.codePointAt(i));
			if (glyph != null) {
				width += glyph.getAdvance();
			}
		}

		return width;
	}
}
