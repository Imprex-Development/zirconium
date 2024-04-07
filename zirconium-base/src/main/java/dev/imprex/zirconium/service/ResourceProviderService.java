package dev.imprex.zirconium.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.gson.JsonObject;

import dev.imprex.zirconium.util.ResourcePath;

public class ResourceProviderService implements ResourceProvider {

	private final MojangGameFileService gameFileService;

	private final Path assetDirectory;
	private final Path assetMetadataPath;

	public ResourceProviderService(MojangGameFileService gameFileService, Path assetDirectory) {
		this.gameFileService = gameFileService;
		this.assetDirectory = assetDirectory;
		this.assetMetadataPath = assetDirectory.resolve(".metadata.json");

		this.downloadAndUnpackClientJar();
	}

	@Override
	public Optional<InputStream> getResource(ResourcePath path) {
		JsonObject objects = this.gameFileService.getAssetIndex()
			.orElseThrow(() -> new RuntimeException("can't download assetIndex"))
			.getAsJsonObject("objects");

		String assetName = String.format("%s/%s", path.namespace(), path.path());
		if (objects.has(assetName)) {
			String objectHash = objects.getAsJsonObject(assetName).get("hash").getAsString();
			return gameFileService.requestObject(objectHash);
		}

		Path assetPath = this.assetDirectory.resolve("assets/" + assetName);
		if (Files.notExists(assetPath)) {
			return Optional.empty();
		}

		try {
			return Optional.of(Files.newInputStream(assetPath));
		} catch (IOException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}

	private void downloadAndUnpackClientJar() {
		byte[] versionBytes = this.gameFileService.getVersion()
			.map(version -> version.get("id").getAsString())
			.orElse("<empty>")
			.getBytes(StandardCharsets.US_ASCII);

		try {
			Files.createDirectories(this.assetDirectory);

			if (Files.exists(this.assetMetadataPath)) {
				byte[] metadataVersion = Files.readAllBytes(this.assetMetadataPath);
				if (Arrays.equals(versionBytes, metadataVersion)) {
					return;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		Optional<InputStream> request = this.gameFileService.requestClientJar();
		if (request.isEmpty()) {
			throw new RuntimeException("can't download client.jar");
		}

		try (ZipInputStream inputStream = new ZipInputStream(request.get())) {
			ZipEntry zipEntry;
			while ((zipEntry = inputStream.getNextEntry()) != null) {
				if (!zipEntry.getName().startsWith("assets/")) {
					continue;
				}

				Path filePath = this.assetDirectory.resolve(zipEntry.getName());
				Files.createDirectories(filePath.getParent());

				try (OutputStream outputStream = Files.newOutputStream(filePath)) {
					inputStream.transferTo(outputStream);
				}
			}

			Files.write(this.assetMetadataPath, versionBytes);
		} catch (IOException e) {
			throw new RuntimeException("can't unpack client.jar", e);
		}
	}
}
