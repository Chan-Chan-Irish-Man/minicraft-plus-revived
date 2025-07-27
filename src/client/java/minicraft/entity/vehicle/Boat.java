package minicraft.entity.vehicle;

import minicraft.core.Game;
import minicraft.core.Updater;
import minicraft.core.io.Sound;
import minicraft.entity.Direction;
import minicraft.entity.Entity;
import minicraft.entity.PlayerRideable;
import minicraft.entity.mob.Mob;
import minicraft.entity.mob.Player;
import minicraft.gfx.Screen;
import minicraft.gfx.SpriteLinker;
import minicraft.item.Items;
import minicraft.level.tile.Tile;
import minicraft.level.tile.LavaTile;
import minicraft.level.tile.Tiles;
import minicraft.level.tile.WaterTile;
import minicraft.util.Vector2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Boat extends Entity implements PlayerRideable {
	private static final SpriteLinker.LinkedSprite[][] boatSprites = new SpriteLinker.LinkedSprite[][] {
		Mob.compileSpriteList(0, 0, 3, 3, 0, 4, "boat"), //
		Mob.compileSpriteList(0, 3, 3, 3, 0, 4, "boat")
	};

	private static final int MOVE_SPEED = 1;
	private static final int STAMINA_COST_TIME = 45; // a unit of stamina is consumed per this amount of time of move
	private static final int BOAT_OFFSET = 8;
	private static final int SCREEN_RENDER_DIST = 4;
	private static final int BOAT_DIST = 3;
	private static final int LEVEL_BITWISE = 4;
	private static final int UNIT_MOVE_DEFAULT = 0;
	private static final int STAMINA_COST = 1;
	private static final int SPRITE_INDEX_OFFSET_UP_RIGHT = 2;
	private static final int PUSH_DELAY_WATER = 2;
	private static final int PUSH_DELAY_LAVA = 4;
	private static final int PUSH_DELAY_DEFAULT = 6;

	private @Nullable Entity passenger;
	private @NotNull Direction dir;

	private int walkDist = 0;
	private int unitMoveCounter = 0;

	public Boat(@NotNull Direction dir) {
		super(6, 6);
		this.dir = dir;
	}

	@Override
	public void render(Screen screen) {
		int xo = x - BOAT_OFFSET; // Horizontal
		int yo = y - BOAT_OFFSET; // Vertical

		int spriteIndex = ((walkDist >> BOAT_DIST) & 1);

		switch (dir) {
			case UP: // if currently riding upwards...
				screen.render(xo - SCREEN_RENDER_DIST, yo - SCREEN_RENDER_DIST, boatSprites[0][spriteIndex + SPRITE_INDEX_OFFSET_UP_RIGHT].getSprite());
				break;
			case LEFT: // Riding to the left... (Same as above)
				screen.render(xo - SCREEN_RENDER_DIST, yo - SCREEN_RENDER_DIST, boatSprites[1][spriteIndex].getSprite());
				break;
			case RIGHT: // Riding to the right (Same as above)
				screen.render(xo - SCREEN_RENDER_DIST, yo - SCREEN_RENDER_DIST, boatSprites[1][spriteIndex + SPRITE_INDEX_OFFSET_UP_RIGHT].getSprite());
				break;
			case DOWN: // Riding downwards (Same as above)
				screen.render(xo - SCREEN_RENDER_DIST, yo - SCREEN_RENDER_DIST, boatSprites[0][((walkDist >> BOAT_DIST) & 1)].getSprite());
				break;
			default:
				throw new UnsupportedOperationException("dir must be defined when on world");
		}

		if (passenger != null)
			passenger.render(screen);
	}

	private Tile getCurrentTile() {
		return level.getTile(x >> LEVEL_BITWISE, y >> LEVEL_BITWISE);
	}

	@Override
	public void tick() {
		if (isRemoved()) return;
		if (level != null && getCurrentTile() == Tiles.get("lava")) {
			hurt();
			if (isRemoved()) return;
		}

		// Moves the furniture in the correct direction.
		move(pushDir.getX(), pushDir.getY());
		pushDir = Direction.NONE;

		if (pushTime > 0) pushTime--; // Update pushTime by subtracting 1.
		else multiPushTime = 0;
	}

	public void hurt() {
		if (isRemoved())
			return;
		stopRiding((Player)passenger);
		die();
	}

	@Override
	public void die() {
		level.dropItem(x, y, Items.get("Boat"));
		Sound.play("monsterhurt");
		super.die();
	}

	public boolean isInWater() {
		return getCurrentTile() instanceof WaterTile;
	}

	public boolean isInLava() {
		return getCurrentTile() instanceof LavaTile;
	}

	@Override
	protected int getPushTimeDelay() {
		return isInWater() ? PUSH_DELAY_WATER : isInLava() ? PUSH_DELAY_LAVA : PUSH_DELAY_DEFAULT;
	}

	@Override
	protected void touchedBy(Entity entity) {
		if (entity instanceof Player)
			tryPush((Player) entity);
		if (entity instanceof Boat && ((Boat) entity).passenger instanceof Player)
			tryPush((Player) ((Boat) entity).passenger);
	}

	private void syncPassengerState(Entity passenger) {
		passenger.x = x;
		passenger.y = y;
		if (passenger instanceof Mob) {
			((Mob) passenger).dir = dir;
			((Mob) passenger).walkDist = walkDist;
		}
	}

	@Override
	public boolean canSwim() {
		return true;
	}

	@Override
	public boolean rideTick(Player passenger, Vector2 vec) {
		if (this.passenger != passenger) return false;

		if (unitMoveCounter >= STAMINA_COST_TIME) {
			passenger.payStamina(STAMINA_COST);
			unitMoveCounter -= STAMINA_COST_TIME;
		}

		boolean inLava = isInLava(), inWater = isInWater();
		if (inLava) {
			if (Updater.tickCount % 2 != 0) return true; // A bit slower when in lava.
		} else if (!inWater && Updater.tickCount % 4 != 0) return true; // Slower when not in water.
		int xd = (int) (vec.x * MOVE_SPEED);
		int yd = (int) (vec.y * MOVE_SPEED);
		dir = Direction.getDirection(xd, yd);
		if (passenger.stamina > 0 && move(xd, yd)) {
			if (!(inWater || inLava) || (inLava && Updater.tickCount % 4 == 0) ||
				(inWater && Updater.tickCount % 2 != 0)) walkDist++; // Slower the animation
			syncPassengerState(passenger);
			unitMoveCounter++;
		} else {
			if (passenger.dir != this.dir) // Sync direction even not moved to render in consistent state
				syncPassengerState(passenger);
			if (unitMoveCounter > UNIT_MOVE_DEFAULT) // Simulation of resting
				unitMoveCounter--;
		}
		return true;
	}

	public @NotNull Direction getDir() {
		return dir;
	}

	public boolean isMoving() {
		return unitMoveCounter > UNIT_MOVE_DEFAULT;
	}

	@Override
	public boolean startRiding(Player player) {
		if (passenger == null) {
			passenger = player;
			unitMoveCounter = UNIT_MOVE_DEFAULT;
			syncPassengerState(passenger);
			return true;
		} else
			return false;
	}

	@Override
	public void stopRiding(Player player) {
		if (passenger == player) {
			passenger = null;
			unitMoveCounter = UNIT_MOVE_DEFAULT; // reset counters
		}
	}
}
