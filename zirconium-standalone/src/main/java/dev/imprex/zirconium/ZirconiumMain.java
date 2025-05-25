package dev.imprex.zirconium;

import java.nio.file.Paths;

public class ZirconiumMain {

	private final Zirconium zirconium = new Zirconium();

	public static void main(String[] args) throws Exception {
		new ZirconiumMain();
	}

	public ZirconiumMain() throws Exception {
		zirconium.registerDirectory(Paths.get("src/main/resources/"));
		zirconium.finalizeResourcePack();

		for (int i = 8; i <= 71; i++) {
			System.out.println(zirconium.getFont().getGlyph("streamevent:logo_" + i).character());
		}
	}
}
