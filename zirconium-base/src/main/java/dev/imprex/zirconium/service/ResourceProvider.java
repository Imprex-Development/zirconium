package dev.imprex.zirconium.service;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Optional;

import dev.imprex.zirconium.util.ResourcePath;

public interface ResourceProvider {

	Optional<InputStream> getResource(ResourcePath path);

	default InputStream getResourceOrThrow(ResourcePath path) throws FileNotFoundException {
		return this.getResource(path).orElseThrow(() -> {
			return new FileNotFoundException(path.toString());
		});
	}
}
