package dev.imprex.zirconium.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.imprex.zirconium.context.SourceContext;
import dev.imprex.zirconium.context.SourceContextEntryVisitor;
import dev.imprex.zirconium.resources.Font.Glyph;
import dev.imprex.zirconium.service.MojangGameFileService;
import dev.imprex.zirconium.util.ResourcePath;

public class Language implements SourceContextEntryVisitor {

	private static final Logger LOGGER = LogManager.getLogger(Language.class);
	private static final Pattern NAMESPACE_PATTERN = Pattern.compile("\\{\\{([^\\{\\}]+)\\}\\}");

	private static ResourcePath path(String languageCode) {
		return ResourcePath.minecraft(String.format("lang/%s.json", languageCode));
	}

	private final ResourcePackBuilder resourcePackBuilder;
	private final Font font;

	private final Set<String> languageCodes = new HashSet<>();
	private final Map<String, Map<String, Translation>> languages = new HashMap<>();

	private boolean finalized = false;

	public Language(ResourcePackBuilder resourcePackBuilder, Font font, MojangGameFileService mojangGameFileService) {
		this.resourcePackBuilder = resourcePackBuilder;
		this.font = font;

		this.languageCodes.addAll(mojangGameFileService.getLanguages());
	}

	public void finalizeLanguage() throws IOException {
		if (this.finalized) {
			throw new IllegalStateException("already finalized!");
		}
		this.finalized = true;

		for (Map.Entry<String, Map<String, Translation>> language : this.languages.entrySet()) {
			JsonObject root = new JsonObject();
			for (Map.Entry<String, Translation> translation : language.getValue().entrySet()) {
				root.addProperty(translation.getKey(), translation.getValue().translation());
			}
			this.resourcePackBuilder.write(path(language.getKey()), root);
		}
	}

	@Override
	public void visit(SourceContext context, Path path) throws IOException {
		if (this.finalized) {
			throw new IllegalStateException("already finalized!");
		} else if (!path.getFileName().toString().endsWith(".lang.json")) {
			return;
		}

		LOGGER.info("found language file {} in {}", path, context);

		try (InputStream inputStream = context.getInputStream(path)) {
			JsonObject root = (JsonObject) JsonParser
					.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

			// copy common to all possible languages
			JsonObject common = root.remove("common").getAsJsonObject();
			if (common != null) {
				for (String languageCode : this.languageCodes) {
					mergeTranslations(context, languageCode, common, true);
				}
			}

			// copy each language only if the languageCode is supported
			for (String languageCode : root.keySet()) {
				if (!this.languageCodes.contains(languageCode)) {
					LOGGER.warn("unknown language code {} in plugin {}", languageCode, context);
					continue;
				}
				mergeTranslations(context, languageCode, root.getAsJsonObject(languageCode), false);
			}
		}
	}

	/**
	 * Merges already defined translations with the given translations object
	 */
	private void mergeTranslations(SourceContext context, String languageCode, JsonObject translations, boolean common) {
		Map<String, Translation> language = this.languages.get(languageCode);
		if (language == null) {
			language = new HashMap<>();
			this.languages.put(languageCode, language);
		}

		for (String key : translations.keySet()) {
			Translation translation = language.get(key);
			if (translation != null && !translation.common()) {
				LOGGER.warn("{} defined duplicate translation key {} for {}", context, key, languageCode);
				continue;
			}

			String value = translations.get(key).getAsString();
			language.put(key, new Translation(common, this.transformTranslation(value)));
		}
	}

	/**
	 * Replaces {{namespace:key}} with the glyph string and {{offset}} with an
	 * offset string
	 */
	private String transformTranslation(String value) {
		StringBuilder builder = new StringBuilder();

		Matcher matcher = NAMESPACE_PATTERN.matcher(value);
		while (matcher.find()) {
			String match = matcher.group(1);
			try {
				int offset = Integer.parseInt(match);
				matcher.appendReplacement(builder, Font.getOffsetString(offset));
			} catch (NumberFormatException e) {
				Glyph glyph = this.font.getGlyph(match);
				if (glyph == null) {
					throw new RuntimeException("can't find glyph " + match);
				}
				matcher.appendReplacement(builder, glyph.character());
			}
		}
		matcher.appendTail(builder);

		return builder.toString();
	}

	private record Translation(boolean common, String translation) {
	}
}
