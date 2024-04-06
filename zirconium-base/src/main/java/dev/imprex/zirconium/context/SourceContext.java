package dev.imprex.zirconium.context;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface SourceContext {

	boolean has(Path path) throws IOException;

	InputStream getInputStream(Path path) throws IOException;

	void visit(SourceContextEntryVisitor visitor) throws IOException;
}
