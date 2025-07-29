package minicraft.level.tile.entity;

import minicraft.core.Game;
import minicraft.core.Renderer;
import minicraft.entity.Entity;
import minicraft.gfx.Screen;
import minicraft.screen.SignDisplayMenu;

public class SignTileEntity extends Entity {
	public SignTileEntity() {
		super(8, 8);
	}

	@Override
	public void render(Screen screen) {}

	@Override
	public boolean isSolid() {
		return false;
	}

	@Override
	public void tick() {
		int xt = (x >> TILE_SIZE_SHIFT), yt = (y >> TILE_SIZE_SHIFT);
		if (Game.player.x >> TILE_SIZE_SHIFT == xt && Game.player.y >> TILE_SIZE_SHIFT == yt) {
			if (Renderer.signDisplayMenu == null || Renderer.signDisplayMenu.differsFrom(level.depth, xt, yt)) {
				Renderer.signDisplayMenu = new SignDisplayMenu(level, xt, yt);
			}
		} else {
			if (Renderer.signDisplayMenu != null && Renderer.signDisplayMenu.matches(level.depth, xt, yt)) {
				Renderer.signDisplayMenu = null;
			}
		}
	}
}
