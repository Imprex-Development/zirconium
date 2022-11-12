package dev.imprex.zirconium.resources;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import dev.imprex.zirconium.context.SourceContext;
import dev.imprex.zirconium.context.SourceContextFileVisitor;
import dev.imprex.zirconium.util.GsonHelper;
import dev.imprex.zirconium.util.ResourceKey;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public class Font implements SourceContextFileVisitor {

	private static final Logger LOGGER = LogManager.getLogger(Font.class);

	private static final int MAX_OFFSET = 1023;
	private static final int OFFSET_BITS = 32 - Integer.numberOfLeadingZeros(MAX_OFFSET);

	private static final char OFFSET_BASE_CHAR = '\uE000';

	private static final String[] OFFSET_CACHE = new String[MAX_OFFSET * 2 + 1];

	public static String getOffsetString(int offset) {
		if (offset < -MAX_OFFSET || offset > MAX_OFFSET) {
			throw new IllegalArgumentException("offset out of range");
		}

		int index = offset + MAX_OFFSET;
		String offsetString = OFFSET_CACHE[index];

		if (OFFSET_CACHE[index] == null) {
			offsetString = computeOffsetString(offset);
			OFFSET_CACHE[index] = offsetString;
		}

		return offsetString;
	}

	private static String computeOffsetString(int offset) {
		StringBuilder builder = new StringBuilder(10);

		char baseChar = (char) (OFFSET_BASE_CHAR + (offset < 0 ? 0 : OFFSET_BITS));
		offset = Math.abs(offset);

		for (int index = 0; offset != 0; index++) {
			if ((offset & 1) != 0) {
				builder.append((char) ((int) baseChar + index));
			}
			offset >>>= 1;
		}

		return builder.toString();
	}

	private final ResourcePackBuilder resourcePackBuilder;

	private final List<GlyphProvider> glyphProviders = new ArrayList<>();
	private final Map<ResourceKey, Glyph> glyphs = new HashMap<>();

	private char nextCharacter = '\uE040';
	private boolean finalized = false;

	public Font(ResourcePackBuilder resourcePackBuilder) {
		this.resourcePackBuilder = resourcePackBuilder;
		this.registerOffsetCharacters();
	}

	private void registerOffsetCharacters() {
		for (int bit = 0; bit < OFFSET_BITS; bit++) {
			int negativeHeight = -(1 << bit) - 2;
			char negativeOffsetChar = (char) (OFFSET_BASE_CHAR + bit);
			this.glyphProviders.add(GlyphProvider.from(negativeHeight, negativeOffsetChar));

			int positiveHeight = (1 << bit) - 1;
			char positiveOffsetChar = (char) (OFFSET_BASE_CHAR + OFFSET_BITS + bit);
			this.glyphProviders.add(GlyphProvider.from(positiveHeight, positiveOffsetChar));
		}

		try {
			this.resourcePackBuilder.write("assets/zirconium/textures/offset.png",
					getClass().getResourceAsStream("/assets/offset.png"));
		} catch (IOException e) {
			throw new RuntimeException("can't register offset characters", e);
		}
	}

	public Glyph getGlyph(String key) {
		return getGlyph(ResourceKey.fromString(key));
	}

	public Glyph getGlyph(ResourceKey key) {
		Glyph glyph = this.glyphs.get(key);
		if (glyph == null) {
			throw new NullPointerException("can't find glyph: " + key);
		}
		return glyph;
	}

	public BaseComponent getComponent(ResourceKey key) {
		return getComponent(key, 0);
	}

	public BaseComponent getComponent(ResourceKey key, int offse) {
		Glyph glyph = Objects.requireNonNull(getGlyph(key), key.toString());

		BaseComponent component = new TextComponent(glyph.character + getOffsetString(offse));
		component.setFont("minecraft:default");
		return component;
	}

	public void finalizeFont() throws IOException {
		if (this.finalized) {
			throw new IllegalStateException("already finalized!");
		}
		
		this.finalized = true;
		this.resourcePackBuilder.writeFont("assets/minecraft/font/default.json", this.glyphProviders);
		System.out.println(this.glyphs.keySet().stream().map(ResourceKey::toString).collect(Collectors.joining("\n")));
	}

	@Override
	public void visit(SourceContext context, Path path) throws IOException {
		if (this.finalized) {
			throw new IllegalStateException("already finalized!");
		} else if (!path.toString().endsWith(".font.json")) {
			return;
		}

		LOGGER.info("found font file {} in {}", path, context);

		try (InputStream inputStream = context.getInputStream(path)) {
			Type listType = new TypeToken<List<Texture>>() {
			}.getType();
			List<Texture> json = GsonHelper.GSON.fromJson(new InputStreamReader(inputStream, StandardCharsets.UTF_8), listType);
			for (Texture texture : json) {

				Path textureFile = getPath(texture.file);
				if (!context.has(textureFile)) {
					new IOException("can't find texture file: " + textureFile).printStackTrace();
					continue;
				}

				try (InputStream textureStream = context.getInputStream(textureFile)) {
					BufferedImage image = ImageIO.read(textureStream);
		
					String resourcePackPath = textureFile.toString();
					if (!this.resourcePackBuilder.hasEntry(resourcePackPath)) {
						this.resourcePackBuilder.write(resourcePackPath, image);
					}

					this.registerTexture(texture, image);
				}
			}
		}
	}

	private static Path getPath(ResourceKey key) {
		return Paths.get(String.format("assets/%s/textures/%s", key.getNamespace(), key.getKey()));
	}

	private void registerTexture(Texture texture, BufferedImage image) {
		if (texture.grid == null || texture.grid.length == 0 || texture.grid[0].length == 0) {
			this.registerSimpleTexture(texture, image);
		} else {
			this.registerGridTexture(texture, image);
		}
	}

	private void registerSimpleTexture(Texture texture, BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		float scale = (float) texture.height / (float) height;

		int actualWidth = getActualGlyphWidth(image, width, height, 0, 0);
		int glyphWidth = (int) (0.5D + (double) ((float) actualWidth * scale)) + 1;

		char[] glyphCharacters = getGlyphCharacters(width);
		String glyphString = getGlyphString(glyphCharacters);

		Glyph glyph = new Glyph(glyphString, glyphWidth, texture.height, texture.ascent);
		GlyphProvider glyphProvider = GlyphProvider.from(texture, new String(glyphCharacters));

		this.glyphProviders.add(glyphProvider);
		this.glyphs.put(texture.name, glyph);
	}

	private void registerGridTexture(Texture texture, BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();

		int columns = texture.grid[0].length;
		int rows = texture.grid.length;
		int gridWidth = width / columns;
		int gridHeight = height / rows;

		float scale = (float) texture.height / (float) gridHeight;

		String[] chars = new String[rows];
		for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
			StringBuilder row = new StringBuilder();

			for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
				int actualWidth = getActualGlyphWidth(image, gridWidth, gridHeight, columnIndex, rowIndex);
				int glyphWidth = (int) (0.5D + (double) ((float) actualWidth * scale)) + 1;

				char[] glyphCharacters = getGlyphCharacters(gridWidth);
				String glyphString = getGlyphString(glyphCharacters);

				Glyph glyph = new Glyph(glyphString, glyphWidth, texture.height, texture.ascent);
				ResourceKey name = ResourceKey.fromString(texture.name + "/" + texture.grid[rowIndex][columnIndex]);

				row.append(glyphCharacters);
				this.glyphs.put(name, glyph);
			}

			chars[rowIndex] = row.toString();
		}

		this.glyphProviders.add(GlyphProvider.from(texture, chars));
	}

	private char[] getGlyphCharacters(int width) {
		char[] chars = new char[(width / 256) + 1];

		for (int i = 0; i < chars.length; i++) {
			chars[i] = this.nextCharacter++;
		}

		return chars;
	}

	private String getGlyphString(char[] chars) {
		StringBuilder glyphString = new StringBuilder((chars.length * 2) - 1);

		for (int i = 0; i < chars.length - 1; i++) {
			glyphString.append(chars[i]);
			glyphString.append(Font.getOffsetString(-1));
		}

		glyphString.append(chars[chars.length - 1]);

		return new String(glyphString);
	}

	private static int getActualGlyphWidth(BufferedImage image, int width, int height, int xIndex, int yIndex) {
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

	public record GlyphProvider(String type, String file, int ascent, int height, String... chars) {

		public static GlyphProvider from(int height, char character) {
			return new GlyphProvider("bitmap", "zirconium:offset.png", -32768, height,
					new String[] { Character.toString(character) });
		}

		public static GlyphProvider from(Texture texture, String chars) {
			return new GlyphProvider("bitmap", texture.file.toString(), texture.ascent, texture.height,
					new String[] { chars });
		}

		public static GlyphProvider from(Texture texture, String[] chars) {
			return new GlyphProvider("bitmap", texture.file.toString(), texture.ascent, texture.height, chars);
		}
	}

	public record Glyph(String character, int width, int height, int ascent) {
	}

	private class Texture {

		public ResourceKey name;
		public ResourceKey file;

		public int ascent;
		public int height;

		public String[][] grid;

	}
}
