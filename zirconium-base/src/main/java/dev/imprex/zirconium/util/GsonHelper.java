package dev.imprex.zirconium.util;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.imprex.zirconium.gui.font.providers.GlyphProvider;
import dev.imprex.zirconium.gui.font.providers.GlyphProviderDefinition;

public class GsonHelper {

	public static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
			.registerTypeAdapter(GlyphProviderDefinition.class, GlyphProvider.DESERIALIZER)
			.registerTypeAdapter(ResourcePath.class, ResourcePath.TYPE_ADAPTER)
			.create();
}
