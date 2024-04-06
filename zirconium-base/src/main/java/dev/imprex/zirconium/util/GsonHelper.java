package dev.imprex.zirconium.util;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class GsonHelper {

	public static final Gson GSON = new GsonBuilder().setPrettyPrinting()
			.registerTypeAdapter(ResourcePath.class, new TypeAdapter<ResourcePath>() {

				@Override
				public void write(JsonWriter out, ResourcePath value) throws IOException {
					out.value(value.toString());
				}

				@Override
				public ResourcePath read(JsonReader in) throws IOException {
					return ResourcePath.fromString(in.nextString());
				}
			})
			.create();

}
