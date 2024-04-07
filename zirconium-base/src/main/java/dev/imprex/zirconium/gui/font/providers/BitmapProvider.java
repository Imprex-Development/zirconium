package dev.imprex.zirconium.gui.font.providers;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import dev.imprex.zirconium.gui.font.glyphs.GlyphInfo;
import dev.imprex.zirconium.service.ResourceProvider;
import dev.imprex.zirconium.util.ResourcePath;

public final class BitmapProvider implements GlyphProvider {

	private final Map<Integer, GlyphInfo> glyphs;

	private BitmapProvider(Map<Integer, Float> advances) {
		this.glyphs = new HashMap<>(advances.size());
		advances.forEach((key, value) -> this.glyphs.put(key, () -> value));
	}

	@Override
	public GlyphInfo getGlyph(int codepoint) {
		return this.glyphs.get(codepoint);
	}

	public Set<Integer> getSupportedGlyphs() {
		return this.glyphs.keySet();
	}

	public static record Definition(ResourcePath file, int height, int ascent, String[] chars)
			implements GlyphProviderDefinition {

		@Override
		public GlyphProviderType type() {
			return GlyphProviderType.BITMAP;
		}

		@Override
		public GlyphProvider load(ResourceProvider provider) throws IOException {
			ResourcePath path = file().withPrefix("textures/");

			try (InputStream inputStream = provider.getResourceOrThrow(path)) {
				BufferedImage image = ImageIO.read(inputStream);
				Map<Integer, Float> advances = new HashMap<>();

				int imageWidth = image.getWidth();
				int imageHeight = image.getHeight();

				int glyphWidth = imageWidth / chars()[0].length();
				int glyphHeight = imageHeight / chars().length;

				int height = height() == 0 ? 8 : height();
				float scale = (float) height / (float) glyphHeight;

				for (int y = 0; y < chars().length; y++) {
					int[] row = chars()[y].codePoints().toArray();

					for (int x = 0; x < row.length; x++) {
						if (row[x] == 0) {
							continue;
						}

						int advance = getActualGlyphWidth(image, glyphWidth, glyphHeight, x, y);
						advances.put(row[x], (int)(0.5D + (double)(advance * scale)) + 1F);
					}
				}

				return new BitmapProvider(advances);
			}
		}

	}

	public static int getActualGlyphWidth(BufferedImage image, int width, int height, int xIndex, int yIndex) {
		Raster raster = image.getRaster();
		ColorModel colorModel = image.getColorModel();

		for (int baseX = width - 1; baseX >= 0; baseX--) {
			int x = xIndex * width + baseX;
			for (int baseY = 0; baseY < height; baseY++) {
				int y = yIndex * height + baseY;
				if (colorModel.getAlpha(raster.getDataElements(x, y, null)) != 0) {
					return baseX + 1;
				}
			}
		}

		return 1;
	}
}
