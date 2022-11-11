package dev.imprex.zirconium.util;

import java.lang.reflect.Type;

import org.bukkit.NamespacedKey;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;

public class GsonHelper {

	public static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(NamespacedKey.class,
			(JsonDeserializer<NamespacedKey>) (JsonElement json, Type typeOfT, JsonDeserializationContext context) -> {
				return NamespacedKey.fromString(json.getAsString());
			}).create();

}
