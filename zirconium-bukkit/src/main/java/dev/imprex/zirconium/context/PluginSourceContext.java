package dev.imprex.zirconium.context;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class PluginSourceContext extends ArchiveSourceContext {

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

	private static File getPluginFile(Plugin plugin) {
		try {
			if (plugin instanceof JavaPlugin javaPlugin) {
				return (File) JAVA_PLUGIN_GET_FILE.invoke(plugin, new Object[0]);
			}
		} catch (Exception e) {
			throw new RuntimeException("unable to create plugin context!", e);
		}
		throw new RuntimeException("unable to create plugin context!");
	}

	public static PluginSourceContext create(Plugin plugin) throws IOException {
		return new PluginSourceContext(plugin);
	}

	private final Plugin plugin;

	private PluginSourceContext(Plugin plugin) throws IOException {
		super(getPluginFile(plugin));
		this.plugin = plugin;
	}

	@Override
	public String toString() {
		return "plugin " + this.plugin.getName();
	}
}
