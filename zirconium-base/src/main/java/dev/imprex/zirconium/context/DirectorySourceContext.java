package dev.imprex.zirconium.context;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashSet;
import java.util.Set;

public class DirectorySourceContext implements SourceContext {

	public static DirectorySourceContext create(Path path) throws IOException {
		if (!Files.isDirectory(path)) {
			throw new IllegalArgumentException("path isn't a directory");
		}
		return new DirectorySourceContext(path);
	}

	private final Path directory;
	private final Set<Path> entries = new LinkedHashSet<>();

	private DirectorySourceContext(Path directory) throws IOException {
		this.directory = directory;

		this.initializeEntries();
	}

	private void initializeEntries() throws IOException {
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (attrs.isRegularFile()) {
					DirectorySourceContext.this.entries.add(directory.relativize(file));
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}

	@Override
	public boolean has(Path path) throws IOException {
		return this.entries.contains(path);
	}

	@Override
	public InputStream getInputStream(Path path) throws IOException {
		if (!this.entries.contains(path)) {
			throw new IOException("can't find entry: " + path);
		}

		return Files.newInputStream(this.directory.resolve(path));
	}

	@Override
	public void visit(SourceContextEntryVisitor visitor) throws IOException {
		for (Path entry : this.entries) {
			visitor.visit(this, entry);
		}
	}

	@Override
	public String toString() {
		return "directory " + this.directory;
	}
}
