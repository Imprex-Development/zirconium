package dev.imprex.zirconium.gui.font.providers;

import java.io.IOException;

import dev.imprex.zirconium.service.ResourceProvider;

public interface GlyphProviderDefinition {

	GlyphProviderType type();

	GlyphProvider load(ResourceProvider provider) throws IOException;

}
