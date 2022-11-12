package dev.imprex.zirconium.context;

import java.io.IOException;
import java.nio.file.Path;

@FunctionalInterface
public interface SourceContextFileVisitor {

	void visit(SourceContext context, Path path) throws IOException;
}
