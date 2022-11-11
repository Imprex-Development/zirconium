package dev.imprex.zirconium;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import dev.imprex.zirconium.resources.Font;
import dev.imprex.zirconium.resources.Language;
import dev.imprex.zirconium.resources.ResourcePackBuilder;
import dev.imprex.zirconium.resources.ResourcePackBuilder.ResourcePack;
import dev.imprex.zirconium.util.PluginContext;
import dev.imprex.zirconium.util.ResourcePackServer;
import net.md_5.bungee.api.chat.TextComponent;

public class Zirconium {

	private final ResourcePackBuilder resourcePackBuilder = new ResourcePackBuilder();

	private final Font font = new Font(this.resourcePackBuilder);
	private final Language language = new Language(this.resourcePackBuilder, this.font);

	private ResourcePack resourcePack;
	private ResourcePackServer resourcePackServer;

	private boolean finalized = false;

	public Font getFont() {
		return font;
	}

	public void startResourcePackServer(Plugin plugin, int port) {
		if (!this.finalized) {
			throw new IllegalStateException("not finalized yet!");
		}
		Bukkit.getPluginManager().registerEvents(new PlayerListener(this.resourcePack, port), plugin);
		this.resourcePackServer = new ResourcePackServer(this.resourcePack, port);
	}

	public void stopResourcePackServer() {
		if (this.resourcePackServer != null) {
			this.resourcePackServer.close();
			this.resourcePackServer = null;
		}
	}

	public void register(Plugin plugin) {
		if (this.finalized) {
			throw new IllegalStateException("already finalized!");
		}

		try (PluginContext pluginContext = PluginContext.create(plugin)) {
			pluginContext.visit((context, entry) -> {
				this.font.visit(pluginContext, entry);
				this.language.visit(pluginContext, entry);
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void finalizeResourcePack() {
		if (this.finalized) {
			throw new IllegalStateException("already finalized!");
		}
		this.finalized = true;

		try {
			this.font.finalizeFont();
			this.language.finalizeLanguage();

			this.resourcePackBuilder.writeMetadata(9, TextComponent.fromLegacyText("§l§8[§rZirconium§l§8] §r§7Server-Pack"));

			ResourcePack resourcePack = this.resourcePackBuilder.build();
			Files.write(Paths.get("debug.zip"), resourcePack.data());

			this.resourcePack = resourcePack;
		} catch (Exception e) {
			throw new RuntimeException("can't finalize resource pack", e);
		}
	}
}
