package dev.imprex.zirconium;

import org.bukkit.plugin.java.JavaPlugin;

public class ZirconiumPlugin extends JavaPlugin {

	private final ZirconiumBukkit zirconium = new ZirconiumBukkit();

	@Override
	public void onEnable() {
		zirconium.finalizeResourcePack();
		zirconium.startResourcePackServer(40320);
		zirconium.registerListener(this, 40320);
	}

	@Override
	public void onDisable() {
		zirconium.stopResourcePackServer();
	}

	public ZirconiumBukkit getZirconium() {
		return zirconium;
	}
}
