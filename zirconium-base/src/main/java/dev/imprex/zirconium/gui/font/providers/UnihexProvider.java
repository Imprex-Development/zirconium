package dev.imprex.zirconium.gui.font.providers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dev.imprex.zirconium.gui.font.glyphs.GlyphInfo;
import dev.imprex.zirconium.service.ResourceProvider;
import dev.imprex.zirconium.util.ResourcePath;

public final class UnihexProvider implements GlyphProvider {

	private final Map<Integer, Glyph> glyphs;

	public UnihexProvider(Map<Integer, Glyph> glyphs) {
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

	static record Definition(ResourcePath hexFile, List<OverrideRange> sizeOverrides)
			implements GlyphProviderDefinition {

		@Override
		public GlyphProviderType type() {
			return GlyphProviderType.UNIHEX;
		}

		@Override
		public GlyphProvider load(ResourceProvider provider) throws IOException {
			try (ZipInputStream inputStream = new ZipInputStream(provider.getResourceOrThrow(hexFile()))) {
				Map<Integer, Glyph> glyphs = new HashMap<>();

				ZipEntry entry;
				while ((entry = inputStream.getNextEntry()) != null) {
					if (entry.getName().endsWith(".hex")) {
						glyphs.putAll(readFromStream(inputStream));
					}
				}

				for (OverrideRange overrideRange : sizeOverrides()) {
					int from = overrideRange.from().codePointAt(0);
					int to = overrideRange.to().codePointAt(0);

					for (int codepoint = from; codepoint < to; codepoint++) {
						glyphs.put(codepoint, new Glyph(overrideRange.left(), overrideRange.right()));
					}
				}

				return new UnihexProvider(glyphs);
			}
		}

		private Map<Integer, Glyph> readFromStream(InputStream inputStream) throws IOException {
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			Map<Integer, Glyph> glyphs = new HashMap<>();

			String line;
			for (int i = 0; (line = reader.readLine()) != null; i++) {
				String[] arguments = line.split(":");
				if (arguments.length != 2) {
					return glyphs;
				}

				int codepointLength = arguments[0].length();
				if (codepointLength < 4 || codepointLength > 6) {
					throw new IllegalArgumentException(
							"Invalid entry at line " + i + ": expected 4, 5 or 6 hex digits followed by a colon");
				}

				int codepoint = Integer.parseInt(arguments[0], 16);
				String bitmap = arguments[1];

				int charsPerRow = bitmap.length() / 16;
				if (charsPerRow != 2 && charsPerRow != 4 && charsPerRow != 6 && charsPerRow != 8) {
					throw new IllegalArgumentException("Invalid entry at line " + i
							+ ": expected hex number describing (8,16,24,32) x 16 bitmap, followed by a new line");
				}

				int mask = 0;
				for (int j = 0; j < 16; j++) {
					int start = j * charsPerRow;
					mask |= Integer.parseInt(bitmap.substring(start, start + charsPerRow), 16);
				}

				int left, right;
				if (mask == 0) {
					left = 0;
					right = charsPerRow * 4;
				} else {
					left = Integer.numberOfLeadingZeros(mask);
					right = 32 - Integer.numberOfTrailingZeros(mask) - 1;
				}

				glyphs.put(codepoint, new Glyph(left, right));
			}

			return glyphs;
		}
	}

	private static record OverrideRange(String from, String to, int left, int right) {
	}

	private static record Glyph(int left, int right) implements GlyphInfo {

		public int width() {
			return this.right - this.left + 1;
		}

		public float getAdvance() {
			return (float) (this.width() / 2 + 1);
		}

		public float getShadowOffset() {
			return 0.5F;
		}

		public float getBoldOffset() {
			return 0.5F;
		}
	}
}
