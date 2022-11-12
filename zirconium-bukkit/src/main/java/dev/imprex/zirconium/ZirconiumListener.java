package dev.imprex.zirconium;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import dev.imprex.zirconium.resources.ResourcePackBuilder.ResourcePack;

public class ZirconiumListener implements Listener {

	private final ResourcePack resourcePack;
	private final int port;

	public ZirconiumListener(ResourcePack resourcePack, int port) {
		this.resourcePack = resourcePack;
		this.port = port;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		event.getPlayer().setResourcePack(String.format("http://localhost:%s/%s", port, resourcePack.hashString()),
				resourcePack.hash(), true);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		event.setMessage(event.getMessage().replaceAll("[\uE000-\uF8FF]*", ""));
	}
}
