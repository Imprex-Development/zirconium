package dev.imprex.zirconium.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class PluginContext implements AutoCloseable {

	private static final Method JAVA_PLUGIN_GET_FILE = getJavaPluginGetFile();

	private static Method getJavaPluginGetFile() {
		try {
			Method method = JavaPlugin.class.getDeclaredMethod("getFile");
			method.setAccessible(true);
			return method;
		} catch (Exception e) {
			throw new RuntimeException("can't get getFile", e);
		}
	}

	public static PluginContext create(Plugin plugin) {
		try {
			if (plugin instanceof JavaPlugin javaPlugin) {
				return new PluginContext(javaPlugin);
			}
		} catch (Exception e) {
			throw new RuntimeException("unable to create plugin context!", e);
		}
		throw new RuntimeException("unable to create plugin context!");
	}

	private final JavaPlugin plugin;
	private final File pluginFile;

	private final ZipFile zipFile;
	private boolean closed = false;

	private PluginContext(JavaPlugin plugin) throws Exception {
		this.plugin = plugin;
		this.pluginFile = (File) JAVA_PLUGIN_GET_FILE.invoke(plugin);
		this.zipFile = new ZipFile(pluginFile);
	}

	public JavaPlugin getPlugin() {
		return plugin;
	}

	private void ensureOpen() {
		if (this.closed) {
			throw new IllegalStateException("plugin context already closed!");
		}
	}

	public ZipEntry getEntry(String name) {
		this.ensureOpen();
		return this.zipFile.getEntry(name);
	}

	public InputStream getInputStream(ZipEntry entry) throws IOException {
		this.ensureOpen();
		return this.zipFile.getInputStream(entry);
	}

	public void visit(PluginFileVisitor visitor) throws IOException {
		Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
		while (zipEntries.hasMoreElements()) {
			ZipEntry zipEntry = zipEntries.nextElement();
			if (!zipEntry.getName().startsWith("assets/")) {
				continue;
			}

			visitor.visit(this, zipEntry);
		}
	}

	public void close() throws IOException {
		this.zipFile.close();
		this.closed = true;
	}
}
