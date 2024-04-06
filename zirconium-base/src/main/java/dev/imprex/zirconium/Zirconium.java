package dev.imprex.zirconium;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import dev.imprex.zirconium.context.ArchiveSourceContext;
import dev.imprex.zirconium.context.DirectorySourceContext;
import dev.imprex.zirconium.context.SourceContext;
import dev.imprex.zirconium.resources.Font;
import dev.imprex.zirconium.resources.Language;
import dev.imprex.zirconium.resources.ResourcePackBuilder;
import dev.imprex.zirconium.resources.ResourcePackBuilder.ResourcePack;
import dev.imprex.zirconium.service.MojangGameFileService;
import dev.imprex.zirconium.util.ResourcePackServer;
import net.md_5.bungee.api.chat.TextComponent;

public class Zirconium {

	private final ResourcePackBuilder resourcePackBuilder = new ResourcePackBuilder();
	private final MojangGameFileService mojangGameFileService = new MojangGameFileService();

	private final Font font = new Font(this.resourcePackBuilder);
	private final Language language = new Language(this.resourcePackBuilder, this.font, this.mojangGameFileService);

	private ResourcePack resourcePack;
	private ResourcePackServer resourcePackServer;

	private boolean finalized = false;

	public ResourcePack getResourcePack() {
		return resourcePack;
	}

	public Font getFont() {
		return font;
	}

	public void startResourcePackServer(int port) {
		if (!this.finalized) {
			throw new IllegalStateException("not finalized yet!");
		}
		this.resourcePackServer = new ResourcePackServer(this.resourcePack, port);
	}

	public void stopResourcePackServer() {
		if (this.resourcePackServer != null) {
			this.resourcePackServer.close();
			this.resourcePackServer = null;
		}
	}

	public void registerDirectory(Path path) {
		try {
			this.register(DirectorySourceContext.create(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void registerArchive(Path path) {
		try (ArchiveSourceContext context = ArchiveSourceContext.create(path)) {
			this.register(context);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void register(SourceContext context) throws IOException {
		if (this.finalized) {
			throw new IllegalStateException("already finalized!");
		}

		context.visit(this.font);
		context.visit(this.language);
	}

	public void finalizeResourcePack() {
		if (this.finalized) {
			throw new IllegalStateException("already finalized!");
		}
		this.finalized = true;

		try {
			this.font.finalizeFont();
			this.language.finalizeLanguage();

			this.resourcePackBuilder.writeMetadata(22, TextComponent
					.fromLegacyText("\u00A7l\u00A78[\u00A7rZirconium\u00A7l\u00A78] \u00A7r\u00A77Server-Pack"));

			ResourcePack resourcePack = this.resourcePackBuilder.build();
			Files.write(Paths.get("debug.zip"), resourcePack.data());

			this.resourcePack = resourcePack;
		} catch (Exception e) {
			throw new RuntimeException("can't finalize resource pack", e);
		}
	}
}
