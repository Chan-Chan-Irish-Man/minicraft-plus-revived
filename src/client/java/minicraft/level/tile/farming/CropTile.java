package minicraft.level.tile.farming;

import minicraft.core.io.Sound;
import minicraft.entity.Direction;
import minicraft.entity.Entity;
import minicraft.entity.mob.Mob;
import minicraft.entity.mob.Player;
import minicraft.entity.particle.Particle;
import minicraft.gfx.SpriteLinker;
import minicraft.item.Item;
import minicraft.item.Items;
import minicraft.item.StackableItem;
import minicraft.level.Level;
import minicraft.level.tile.Tile;
import minicraft.level.tile.Tiles;
import minicraft.level.tile.WaterTile;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Random;

public class CropTile extends FarmTile {
	protected final @Nullable String seed;

	protected int maxAge = 0b111; // Must be a bit mask.

	private static final int MAX_WATER_DIST = 4;
	private static final int UNDER_HYDRATION = 7;
	private static final int RAND_MOIST_RANGE = 10;

	private static final double CARDINAL_MULTIPLIER = 0.75;
	private static final double SUBCARDINAL_MULTIPLIER = 0.85;
	private static final double HALFCARDINAL_MULTIPLIER = 0.9;
	private static final double SINGLE_DIAGONAL_MULTIPLIER = 0.98125;

	private static final int PARTICLE_LIFETIME_MEAN = 120;
	private static final int PARTICLE_LIFETIME_VARIANCE = 40;

	private static final int MIN_HARVEST_DROPS = 2;
	private static final int HARVEST_DROP_VARIATION = 3;

	private static final int LOWEST_FERT = 100;
	private static final int LOW_FERT = 200;
	private static final int MED_FERT = 300;
	private static final int HIGH_FERT = 400;

	private static final int MOST_AMOUNT = 40;
	private static final int HIGH_AMOUNT = 30;
	private static final int MED_AMOUNT = 25;
	private static final int LOW_AMOUNT = 20;
	private static final int LOWEST_AMOUNT = 10;

	private static final int PLAYER_SCORE_RANGE = 5;

	private static final int MAX_FERTILIZATION = 511;

	protected CropTile(String name, @Nullable String seed) {
		super(name, null);
		this.seed = seed;
	}

	@Override
	public boolean hurt(Level level, int x, int y, Mob source, int dmg, Direction attackDir) {
		harvest(level, x, y, source);
		return true;
	}

	@Override
	public boolean tick(Level level, int xt, int yt) {
		int data = level.getData(xt, yt);
		int moisture = data & 0b111;
		boolean successful = false;
		if (Arrays.stream(level.getAreaTiles(xt, yt, MAX_WATER_DIST)).anyMatch(t -> t instanceof WaterTile)) { // Contains water.
			if (moisture < UNDER_HYDRATION && random.nextInt(RAND_MOIST_RANGE) == 0) { // hydrating
				level.setData(xt, yt, data = (data & ~0b111) + moisture++);
				successful = true;
			}
		} else if (moisture > 0 && random.nextInt(RAND_MOIST_RANGE) == 0) { // drying
			level.setData(xt, yt, data = (data & ~0b111) + moisture--);
			successful = true;
		}

		int fertilization = getFertilization(data);
		int stage = (data >> 3) & maxAge;
		if (stage < maxAge) {
			double points = moisture > 0 ? 4 : 2;
			for (int i = -1; i < 2; i++)
				for (int j = -1; j < 2; j++) {
					Tile t = level.getTile(xt + i, yt + j);
					if ((i != 0 || j != 0) && t instanceof FarmTile) {
						points += (level.getData(xt + i, yt + j) & 0b111) > 0 ? 0.75 : 0.25;
					}
				}

			// Checking whether the target direction has targeted the same CropTile
			boolean up = level.getTile(xt, yt - 1) == this;
			boolean down = level.getTile(xt, yt + 1) == this;
			boolean left = level.getTile(xt - 1, yt) == this;
			boolean right = level.getTile(xt + 1, yt) == this;
			boolean upLeft = level.getTile(xt - 1, yt - 1) == this;
			boolean downLeft = level.getTile(xt - 1, yt + 1) == this;
			boolean upRight = level.getTile(xt + 1, yt - 1) == this;
			boolean downRight = level.getTile(xt + 1, yt + 1) == this;
			if (up && down && left && right && upLeft && downLeft && upRight && downRight)
				points /= 2;
			else {
				if (up && down && left && right)
					points *= CARDINAL_MULTIPLIER;
				if (up && (down && (left || right) || left && right) || down && left && right) // Either 3 of 4 directions.
					points *= SUBCARDINAL_MULTIPLIER;
				if (upLeft && (downRight || downLeft || upRight) || downLeft && (upRight || downRight) || upRight && downRight) // Either 2 of 4 directions.
					points *= HALFCARDINAL_MULTIPLIER;
				if (upLeft) points *= SINGLE_DIAGONAL_MULTIPLIER;
				if (downLeft) points *= SINGLE_DIAGONAL_MULTIPLIER;
				if (upRight) points *= SINGLE_DIAGONAL_MULTIPLIER;
				if (downRight) points *= SINGLE_DIAGONAL_MULTIPLIER;
			}

			if (random.nextInt((int) (100 / points) + 1) < (fertilization / 30 + 1)) // fertilization >= 0
				level.setData(xt, yt, data = (data & ~(maxAge << 3)) + ((stage + 1) << 3)); // Incrementing the stage by 1.
			successful = true;
		}

		if (fertilization > 0) {
			level.setData(xt, yt, (data & (0b111 + (maxAge << 3))) + ((fertilization - 1) << (3 + (maxAge + 1) / 2)));
			successful = true;
		}

		return successful;
	}

	private static final SpriteLinker.LinkedSprite PARTICLE_SPRITE = new SpriteLinker.LinkedSprite(SpriteLinker.SpriteType.Entity, "glint");

	@Override
	public boolean interact(Level level, int xt, int yt, Player player, Item item, Direction attackDir) {
		if (item instanceof StackableItem && item.getName().equalsIgnoreCase("Fertilizer")) {
			((StackableItem) item).count--;
			Random random = new Random();
			for (int i = 0; i < 2; ++i) {
				double x = (double) (xt << Tile.TILE_SIZE_SHIFT) + Tile.TILE_CENTER + (random.nextGaussian() * 0.5) * Tile.TILE_CENTER;
				double y = (double) (yt << Tile.TILE_SIZE_SHIFT) + Tile.TILE_CENTER + (random.nextGaussian() * 0.5) * Tile.TILE_CENTER;
				level.add(new Particle((int) x, (int) y, PARTICLE_LIFETIME_MEAN + random.nextInt(21) - PARTICLE_LIFETIME_VARIANCE, PARTICLE_SPRITE));
			}
			int fertilization = getFertilization(level.getData(xt, yt));
			if (fertilization < LOWEST_FERT) { // More fertilization, lower the buffer is applied.
				fertilize(level, xt, yt, MOST_AMOUNT);
			} else if (fertilization < LOW_FERT) {
				fertilize(level, xt, yt, HIGH_AMOUNT);
			} else if (fertilization < MED_FERT) {
				fertilize(level, xt, yt, MED_AMOUNT);
			} else if (fertilization < HIGH_FERT) {
				fertilize(level, xt, yt, LOW_AMOUNT);
			} else {
				fertilize(level, xt, yt, LOWEST_AMOUNT);
			}

			return true;
		}

		return super.interact(level, xt, yt, player, item, attackDir);
	}

	/**
	 * Default harvest method, used for everything that doesn't really need any special behavior.
	 */
	protected void harvest(Level level, int x, int y, Entity entity) {
		int data = level.getData(x, y);
		int age = (data >> 3) & maxAge;

		if (seed != null)
			level.dropItem((x << Tile.TILE_SIZE_SHIFT) + Tile.TILE_CENTER, (y << Tile.TILE_SIZE_SHIFT) + Tile.TILE_CENTER, 1, Items.get(seed));

		if (age == maxAge) {
			level.dropItem((x << Tile.TILE_SIZE_SHIFT) + Tile.TILE_CENTER, (y << Tile.TILE_SIZE_SHIFT) + Tile.TILE_CENTER, random.nextInt(HARVEST_DROP_VARIATION) + MIN_HARVEST_DROPS, Items.get(name));
		} else if (seed == null) {
			level.dropItem((x << Tile.TILE_SIZE_SHIFT) + Tile.TILE_CENTER, (y << Tile.TILE_SIZE_SHIFT) + Tile.TILE_CENTER, 1, Items.get(name));
		}

		if (age == maxAge && entity instanceof Player) {
			((Player) entity).addScore(random.nextInt(PLAYER_SCORE_RANGE) + 1);
		}

		// Play sound.
		Sound.play("monsterhurt");

		level.setTile(x, y, Tiles.get("farmland"), data & 0b111);
	}

	public int getFertilization(int data) {
		return data >> (3 + (maxAge + 1) / 2);
	}

	/**
	 * Fertilization: Each magnitude of fertilization (by 1) increases the chance of growth by 1/30.
	 * (The addition by fertilization is rounded down to the nearest integer in chance calculation)
	 * For example, if the chance is originally 10% (1/10), the final chance with 30 fertilization will be 20% (2/10).
	 */
	public void fertilize(Level level, int x, int y, int amount) {
		int data = level.getData(x, y);
		int fertilization = getFertilization(data);
		fertilization += amount;
		if (fertilization < 0) fertilization = 0;
		if (fertilization > MAX_FERTILIZATION) fertilization = MAX_FERTILIZATION; // The maximum possible value to be reached.
		// If this value exceeds 511, the final value would be greater than the hard maximum value that short can be.
		level.setData(x, y, (data & (0b111 + (maxAge << 3))) + (fertilization << (3 + (maxAge + 1) / 2)));
	}
}
