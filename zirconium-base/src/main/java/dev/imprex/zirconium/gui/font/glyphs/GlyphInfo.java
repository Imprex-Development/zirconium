package dev.imprex.zirconium.gui.font.glyphs;

public interface GlyphInfo {

	float getAdvance();

	default float getAdvance(boolean bold) {
		return getAdvance() + (bold ? getBoldOffset() : 0.0F);
	}

	default float getBoldOffset() {
		return 1.0F;
	}

	default float getShadowOffset() {
		return 1.0F;
	}
}
