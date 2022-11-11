package dev.imprex.zirconium.resources;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.imprex.zirconium.util.GsonHelper;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public class ResourcePackBuilder {

	private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	private final ZipOutputStream outputStream = new ZipOutputStream(byteArrayOutputStream);

	private final Set<String> entries = new HashSet<>();

	private boolean open = true;

	private void ensureOpen() {
		if (!open) {
			throw new IllegalStateException("Already built!");
		}
	}

	public boolean hasEntry(String path) {
		return this.entries.contains(path);
	}

	public void write(String path, InputStream inputStream) throws IOException {
		ensureOpen();

		this.outputStream.putNextEntry(new ZipEntry(path));
		inputStream.transferTo(this.outputStream);
		this.entries.add(path);
		this.outputStream.closeEntry();
	}

	public void write(String path, JsonElement json) throws IOException {
		ensureOpen();

		this.outputStream.putNextEntry(new ZipEntry(path));
		this.outputStream.write(GsonHelper.GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
		this.entries.add(path);
		this.outputStream.closeEntry();
	}

	public void write(String path, BufferedImage image) throws IOException {
		ensureOpen();

		this.outputStream.putNextEntry(new ZipEntry(path));
		ImageIO.write(image, "png", this.outputStream);
		this.entries.add(path);
		this.outputStream.closeEntry();
	}

	public void writeFont(String path, List<Font.GlyphProvider> glyphProviders) throws IOException {
		JsonObject font = new JsonObject();
		font.add("providers", GsonHelper.GSON.toJsonTree(glyphProviders));
		write(path, font);
	}

	public void writeMetadata(int format, BaseComponent...description) throws IOException {
		JsonObject root = new JsonObject();
		JsonObject pack = new JsonObject();
		pack.addProperty("pack_format", format);
		pack.add("description", JsonParser.parseString(ComponentSerializer.toString(description)));
		root.add("pack", pack);
		write("pack.mcmeta", root);
	}

	public void writeMetadata(int format, String description) throws IOException {
		JsonObject root = new JsonObject();
		JsonObject pack = new JsonObject();
		pack.addProperty("pack_format", format);
		pack.addProperty("description", description);
		root.add("pack", pack);
		write("pack.mcmeta", root);
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
