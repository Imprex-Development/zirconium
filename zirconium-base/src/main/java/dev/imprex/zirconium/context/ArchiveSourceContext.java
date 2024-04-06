package dev.imprex.zirconium.context;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ArchiveSourceContext implements SourceContext, AutoCloseable {

	public static ArchiveSourceContext create(Path path) throws IOException {
		if (path.toString().endsWith(".zip") || !Files.isRegularFile(path)) {
			throw new IllegalArgumentException("path isn't a zip archive");
		}
		return new ArchiveSourceContext(path.toFile());
	}

	private final File file;

	private final ZipFile zipFile;
	private final Map<Path, ZipEntry> zipEntries = new LinkedHashMap<>();

	private final AtomicBoolean open = new AtomicBoolean(true);

	protected ArchiveSourceContext(File file) throws IOException {
		this.file = file;
		this.zipFile = new ZipFile(file);

		this.initializeZipEntries();
	}

	private void initializeZipEntries() {
		Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
		while (zipEntries.hasMoreElements()) {
			ZipEntry zipEntry = zipEntries.nextElement();
			if (!zipEntry.getName().startsWith("assets/")) {
				continue;
			}

			this.zipEntries.put(Paths.get(zipEntry.getName()), zipEntry);
		}
	}

	private void ensureOpen() throws IOException {
		if (!this.open.get()) {
			throw new IOException("archive source already closed: " + this.file);
		}
	}

	public boolean has(Path path) throws IOException {
		this.ensureOpen();

		return this.zipEntries.containsKey(path);
	}

	public InputStream getInputStream(Path path) throws IOException {
		this.ensureOpen();

		ZipEntry zipEntry = this.zipEntries.get(path);
		if (zipEntry == null) {
			throw new IOException("can't find entry: " + path);
		}

		return this.zipFile.getInputStream(zipEntry);
	}

	public void visit(SourceContextEntryVisitor visitor) throws IOException {
		this.ensureOpen();

		for (Path path : this.zipEntries.keySet()) {
			visitor.visit(this, path);
		}
	}

	public void close() throws IOException {
		if (this.open.compareAndSet(false, true)) {
			this.zipFile.close();
		}
	}

	@Override
	public String toString() {
		return "archive " + file.toString();
	}
}
