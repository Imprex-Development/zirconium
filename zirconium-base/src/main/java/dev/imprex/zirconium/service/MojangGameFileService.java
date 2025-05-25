package dev.imprex.zirconium.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.imprex.zirconium.util.GsonHelper;

public class MojangGameFileService {

	private static final String VERSION_INDEX_URI = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
	private static final String DOWNLOAD_BASE_URI = "https://resources.download.minecraft.net/%s/%s";

	private static <T> Function<InputStream, T> json(Class<T> type) {
		return (inputStream) -> {
			try (InputStreamReader reader = new InputStreamReader(inputStream)) {
				return GsonHelper.GSON.fromJson(reader, type);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		};
	}

	private static <T> Supplier<T> lazy(Supplier<T> supplier) {
		return new Supplier<T>() {

			private T result;

			@Override
			public T get() {
				if (this.result == null) {
					this.result = supplier.get();
				}

				return this.result;
			}
		};
	}

	private static <S, T> Supplier<Optional<T>> lazyOptionalChain(Supplier<Optional<S>> source, Function<S, Optional<T>> map) {
		return lazy(() -> source.get().map(map).map(result -> result.orElse(null)));
	}

	private final HttpService http;

	private Supplier<Optional<JsonObject>> version = lazy(this::requestLatestRelease);
	private Supplier<Optional<JsonObject>> assetIndex = lazyOptionalChain(this.version, this::requestAssetIndex);
	private Supplier<Optional<JsonObject>> packMcmeta = lazyOptionalChain(this.assetIndex, this::requestPackMcmeta);

	public MojangGameFileService(HttpService http) {
		this.http = http;
	}

	public Optional<JsonObject> getVersion() {
		return version.get();
	}

	public Optional<JsonObject> getAssetIndex() {
		return assetIndex.get();
	}

	public Optional<JsonObject> getPackMcmeta() {
		return packMcmeta.get();
	}

	public Set<String> getLanguages() {
		return getPackMcmeta()
				.map(response -> response.getAsJsonObject("language").keySet())
				.orElse(Collections.emptySet());
	}

	public Optional<InputStream> requestClientJar() {
		return this.version.get()
			.map(response -> {
				String url = response
						.getAsJsonObject("downloads")
						.getAsJsonObject("client")
						.getAsJsonPrimitive("url")
						.getAsString();

				return this.http.request(url).orElse(null);
			});
	}

	public Optional<InputStream> requestObject(String objectHash) {
		String url = String.format(DOWNLOAD_BASE_URI, objectHash.substring(0, 2), objectHash);
		return this.http.request(url);
	}

	private Optional<JsonObject> requestLatestRelease() {
		return this.http.request(VERSION_INDEX_URI)
			.map(json(JsonObject.class))
			.map(response -> {
				String latestRelease = response
					.getAsJsonObject("latest")
					.get("release")
					.getAsString();

				for (JsonElement element : response.getAsJsonArray("versions")) {
					JsonObject version = element.getAsJsonObject();
					if (version.get("id").getAsString().equals(latestRelease)) {
						return version.get("url").getAsString();
					}
				}

				return null;
			})
			.map(uri -> this.http.request(uri).orElse(null))
			.map(json(JsonObject.class));
	}

	private Optional<JsonObject> requestAssetIndex(JsonObject version) {
		String url = version
				.getAsJsonObject("assetIndex")
				.getAsJsonPrimitive("url")
				.getAsString();

		return this.http.request(url)
				.map(json(JsonObject.class));
	}

	private Optional<JsonObject> requestPackMcmeta(JsonObject assetIndex) {
		String objectHash = assetIndex
				.getAsJsonObject("objects")
				.getAsJsonObject("pack.mcmeta")
				.getAsJsonPrimitive("hash")
				.getAsString();

		return requestObject(objectHash)
				.map(json(JsonObject.class));
	}
}
