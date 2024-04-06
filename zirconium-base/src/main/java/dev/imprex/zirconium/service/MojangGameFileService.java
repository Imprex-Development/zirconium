package dev.imprex.zirconium.service;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.JsonObject;

public class MojangGameFileService extends AbstractHttpService {

	private static final String VERSION_INDEX_URI = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
	private static final String DOWNLOAD_BASE_URI = "https://resources.download.minecraft.net/%s/%s";

	private final AtomicReference<CompletableFuture<JsonObject>> packMeta = new AtomicReference<>();

	public Set<String> getLanguages() {
		try {
			return getPackMeta().thenApply(root -> {
				return root.getAsJsonObject("language").keySet();
			}).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			return Collections.emptySet();
		}
	}

	public CompletableFuture<JsonObject> getPackMeta() {
		if (packMeta.get() == null && packMeta.compareAndSet(null, new CompletableFuture<>())) {
			CompletableFuture<JsonObject> future = packMeta.get();
			requestPackMeta().whenComplete((result, throwable) -> {
				if (throwable != null) {
					future.completeExceptionally(throwable);
				} else {
					future.complete(result);
				}
			});
		}
		return packMeta.get();
	}

	private CompletableFuture<JsonObject> requestPackMeta() {
		// request version index
		return HTTP.sendAsync(request(VERSION_INDEX_URI).build(), json(JsonObject.class))
			// request version metadata
			.thenCompose(response -> {
				JsonObject root = response.body();
				if (root == null) {
					return CompletableFuture.failedStage(new IOException("response (versionIndex) is empty or missing"));
				}

				String versionUrl = root.getAsJsonArray("versions")
					.get(0).getAsJsonObject()
					.getAsJsonPrimitive("url")
					.getAsString();

				return HTTP.sendAsync(request(versionUrl).build(), json(JsonObject.class));
			})
			// request asset index
			.thenCompose(response -> {
				JsonObject root = response.body();
				if (root == null) {
					return CompletableFuture.failedStage(new IOException("response (assetIndex) is empty or missing"));
				}

				String assetIndexUrl = root.getAsJsonObject("assetIndex")
					.getAsJsonPrimitive("url")
					.getAsString();

				return HTTP.sendAsync(request(assetIndexUrl).build(), json(JsonObject.class));
			})
			// request pack.mcmeta
			.thenCompose(response -> {
				JsonObject root = response.body();
				if (root == null) {
					return CompletableFuture.failedStage(new IOException("response (pack.mcmeta) is empty or missing"));
				}

				String objectHash = root.getAsJsonObject("objects")
					.getAsJsonObject("pack.mcmeta")
					.getAsJsonPrimitive("hash")
					.getAsString();

				String url = String.format(DOWNLOAD_BASE_URI, objectHash.substring(0, 2), objectHash);
				return HTTP.sendAsync(request(url).build(), json(JsonObject.class));
			})
			.thenApply(HttpResponse::body);
	}
}
