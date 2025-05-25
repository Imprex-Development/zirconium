package dev.imprex.zirconium.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.common.hash.Hashing;

import dev.imprex.zirconium.util.GsonHelper;

public class HttpService {

	private final HttpClient http = HttpClient.newHttpClient();

	private final Path cacheDirectory;
	private final Path cacheMetadataPath;

	private final HttpMetadata metadata;

	public HttpService(Path cacheDirectory) {
		this.cacheDirectory = cacheDirectory;
		this.cacheMetadataPath = cacheDirectory.resolve(".metadata.json");

		try {
			if (Files.notExists(this.cacheDirectory)) {
				Files.createDirectories(this.cacheDirectory);
			}

			if (Files.notExists(this.cacheMetadataPath)) {
				this.metadata = new HttpMetadata(new HashMap<>());
				this.writeMetadata();
				return;
			}

			try (BufferedReader reader = Files.newBufferedReader(this.cacheMetadataPath)) {
				this.metadata = GsonHelper.GSON.fromJson(reader, HttpMetadata.class);
			}
		} catch (IOException e) {
			throw new RuntimeException("can't initialize http service", e);
		}
	}

	public Optional<InputStream> request(String uri) {
		try {
			return Optional.of(requestInternal(uri));
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}

	@Nullable
	private InputStream requestInternal(String uri) throws IOException, InterruptedException {
		// prepare request object
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(uri))
				.header("User-Agent", "Zirconium/1.0.0")
				.header("Accept", "*/*");

		// add If-Modified-Since if a cache entry exists
		String requestDate = this.metadata.request.get(uri);
		if (requestDate != null) {
			request.header("If-Modified-Since", requestDate);
		}

		// send request
		HttpResponse<InputStream> response = this.http.send(request.build(),
				(responseInfo) -> responseInfo.statusCode() == 304
					? BodySubscribers.replacing(null)
					: BodySubscribers.ofInputStream());

		// calculate cache stream location
		String uriHash = Hashing.murmur3_128()
			.hashString(uri, StandardCharsets.US_ASCII)
			.toString();
		Path path = this.cacheDirectory.resolve(uriHash + ".bin");

		// write to cache if request is okay
		if (response.statusCode() == 200) {
			this.metadata.request.put(uri, response.headers().firstValue("Date").get());
			this.writeMetadata();

			try (InputStream inputStream = response.body();
				 OutputStream outputStream = Files.newOutputStream(path)) {
				inputStream.transferTo(outputStream);
			}
		}

		// return empty stream if status is not okay or cache match
		return response.statusCode() == 200 || response.statusCode() == 304
				? Files.newInputStream(path)
				: null;
	}

	private void writeMetadata() throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(this.cacheMetadataPath)) {
			GsonHelper.GSON.toJson(this.metadata, writer);
		}
	}

	private static record HttpMetadata(Map<String, String> request) {
	}
}
