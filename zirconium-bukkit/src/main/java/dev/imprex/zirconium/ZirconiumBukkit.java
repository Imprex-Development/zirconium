package dev.imprex.zirconium;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import dev.imprex.zirconium.context.PluginSourceContext;

public class ZirconiumBukkit extends Zirconium {

	private boolean listenerRegistered = false;

	public void registerListener(Plugin plugin, int port) {
		if (!this.listenerRegistered) {
			Bukkit.getPluginManager().registerEvents(new ZirconiumListener(this.getResourcePack(), port), plugin);
			this.listenerRegistered = true;
		}
	}

	public void registerPlugin(Plugin plugin) {
		try (PluginSourceContext context = PluginSourceContext.create(plugin)) {
			this.register(context);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
