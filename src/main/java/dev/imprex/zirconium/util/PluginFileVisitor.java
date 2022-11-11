package dev.imprex.zirconium.util;

import java.io.IOException;
import java.util.zip.ZipEntry;

@FunctionalInterface
public interface PluginFileVisitor {
	void visit(PluginContext context, ZipEntry entry) throws IOException;
}
