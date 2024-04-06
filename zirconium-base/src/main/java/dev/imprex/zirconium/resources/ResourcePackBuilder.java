package dev.imprex.zirconium.resources;

import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.imprex.zirconium.util.GsonHelper;
import dev.imprex.zirconium.util.ResourcePath;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public class ResourcePackBuilder {

	private static final Logger LOGGER = LogManager.getLogger(ResourcePackBuilder.class);

	private static String path(ResourcePath resourceKey) {
		return String.format("assets/%s/%s", resourceKey.namespace(), resourceKey.path());
	}

	private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	private final ZipOutputStream outputStream = new ZipOutputStream(byteArrayOutputStream, StandardCharsets.UTF_8);

	private final Set<ResourcePath> entries = new HashSet<>();

	private boolean open = true;

	private void ensureOpen() {
		if (!open) {
			throw new IllegalStateException("Already built!");
		}
	}

	public void write(ResourcePath resourcePath, Object data) throws IOException {
		Objects.requireNonNull(resourcePath);
		Objects.requireNonNull(data);

		this.ensureOpen();
		if (!this.entries.add(resourcePath)) {
			LOGGER.warn("skipping duplicate entry (path={})", resourcePath);
			return;
		}

		this.outputStream.putNextEntry(new ZipEntry(path(resourcePath)));

		if (data instanceof InputStream inputStream) {
			inputStream.transferTo(this.outputStream);
		} else if (data instanceof JsonElement jsonElement) {
			this.outputStream.write(GsonHelper.GSON.toJson(jsonElement).getBytes(StandardCharsets.UTF_8));
		} else if (data instanceof RenderedImage image) {
			ImageIO.write(image, "png", this.outputStream);
		} else {
			throw new IllegalArgumentException("unsupported data type: " + data.getClass());
		}

		this.outputStream.closeEntry();
	}

	private void writeMetadata(JsonObject json) throws IOException {
		this.ensureOpen();

		this.outputStream.putNextEntry(new ZipEntry("pack.mcmeta"));
		this.outputStream.write(GsonHelper.GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
		this.outputStream.closeEntry();
	}

	public void writeFont(ResourcePath resourcePath, List<Font.GlyphProvider> glyphProviders) throws IOException {
		JsonObject font = new JsonObject();
		font.add("providers", GsonHelper.GSON.toJsonTree(glyphProviders));
		write(resourcePath, font);
	}

	public void writeMetadata(int format, BaseComponent...description) throws IOException {
		JsonObject root = new JsonObject();
		JsonObject pack = new JsonObject();
		pack.addProperty("pack_format", format);
		pack.add("description", JsonParser.parseString(ComponentSerializer.toString(description)));
		root.add("pack", pack);
		writeMetadata(root);
	}

	public void writeMetadata(int format, String description) throws IOException {
		JsonObject root = new JsonObject();
		JsonObject pack = new JsonObject();
		pack.addProperty("pack_format", format);
		pack.addProperty("description", description);
		root.add("pack", pack);
		writeMetadata(root);
	}

	@SuppressWarnings("deprecation")
	public ResourcePack build() throws IOException {
		this.outputStream.close();
		this.open = false;

		byte[] data = this.byteArrayOutputStream.toByteArray();
		HashCode hash = Hashing.sha1().hashBytes(data);

		return new ResourcePack(data, hash.asBytes(), hash.toString());
	}

	public record ResourcePack(byte[] data, byte[] hash, String hashString) {
	}
}
