package dev.imprex.zirconium.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscribers;

import dev.imprex.zirconium.util.GsonHelper;

public class AbstractHttpService {

	protected static final HttpClient HTTP = HttpClient.newHttpClient();

	protected static HttpRequest.Builder request(String url) {
		return HttpRequest.newBuilder(URI.create(url))
				.header("User-Agent", "Zirconium/1.0.0")
				.header("Accept", "application/json");
	}

	protected static <T> BodyHandler<T> json(Class<T> target) {
		return (responseInfo) -> responseInfo.statusCode() == 200
				? BodySubscribers.mapping(BodySubscribers.ofInputStream(), inputStream -> {
					try (InputStreamReader reader = new InputStreamReader(inputStream)) {
						return GsonHelper.GSON.fromJson(new InputStreamReader(inputStream), target);
					} catch (IOException e) {
						e.printStackTrace();
						return null;
					}
				})
				: BodySubscribers.replacing(null);
	}
}
