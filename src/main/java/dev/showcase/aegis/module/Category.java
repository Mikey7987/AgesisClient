package dev.showcase.aegis.module;

public enum Category {
	COMBAT("Combat"),
	RENDER("Render"),
	PLAYER("Player"),
	MOVEMENT("Movement");

	public final String label;

	Category(String label) {
		this.label = label;
	}
}
