package dev.imprex.zirconium.gui.font.providers;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;

import dev.imprex.zirconium.gui.font.glyphs.GlyphInfo;
import dev.imprex.zirconium.service.ResourceProvider;
import dev.imprex.zirconium.util.GsonHelper;
import dev.imprex.zirconium.util.ResourcePath;

public interface GlyphProvider {

	public static final JsonDeserializer<GlyphProviderDefinition> DESERIALIZER = (json, typeOfT, context) -> {
		JsonObject jsonObject = json.getAsJsonObject();
		GlyphProviderType type = GlyphProviderType.fromName(jsonObject.get("type").getAsString());
		return context.deserialize(json, type.getType());
	};

	@Nullable
	default GlyphInfo getGlyph(int codepoint) {
		return null;
	}

	Set<Integer> getSupportedGlyphs();

	static Map<Integer, GlyphInfo> loadDefinitionFile(ResourceProvider provider, ResourcePath path) {
		try (InputStreamReader reader = new InputStreamReader(provider.getResourceOrThrow(path))) {
			FontDefinitionFile definitionFile = GsonHelper.GSON.fromJson(reader, FontDefinitionFile.class);

			Map<Integer, GlyphInfo> glyphs = new HashMap<>();

			for (GlyphProviderDefinition definition : definitionFile.providers()) {
				GlyphProvider glyphProvider = definition.load(provider);

				for (Integer codepoint : glyphProvider.getSupportedGlyphs()) {
					glyphs.putIfAbsent(codepoint, glyphProvider.getGlyph(codepoint));
				}
			}

			return glyphs;
		} catch (IOException e) {
			e.printStackTrace();
			return Collections.emptyMap();
		}
	}

	static record FontDefinitionFile(List<GlyphProviderDefinition> providers) {
	}
}
