package minicraft.entity.furniture;

import minicraft.core.Game;
import minicraft.core.Updater;
import minicraft.core.io.Localization;
import minicraft.entity.mob.Player;
import minicraft.gfx.SpriteLinker.LinkedSprite;
import minicraft.gfx.SpriteLinker.SpriteType;
import minicraft.item.DyeItem;
import minicraft.level.Level;
import minicraft.level.tile.Tile;
import minicraft.util.MyUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class Bed extends Furniture {

	private static final HashMap<DyeItem.DyeColor, LinkedSprite> SPRITES = new HashMap<>();
	private static final HashMap<DyeItem.DyeColor, LinkedSprite> ITEM_SPRITES = new HashMap<>();

	@Override
	public @NotNull Furniture copy() {
		return new Bed(color);
	}

	static {
		for (DyeItem.DyeColor color : DyeItem.DyeColor.values()) {
			SPRITES.put(color, new LinkedSprite(SpriteType.Entity, color.toString().toLowerCase() + "_bed"));
			ITEM_SPRITES.put(color, new LinkedSprite(SpriteType.Item, color.toString().toLowerCase() + "_bed"));
		}
	}

	private static int playersAwake = 1;
	private static final HashMap<Player, Bed> SLEEPING_PLAYERS = new HashMap<>();

	public final DyeItem.DyeColor color;

	/**
	 * Creates a new furniture with the name Bed and the bed sprite and color.
	 */
	public Bed() { this(DyeItem.DyeColor.RED); }
	public Bed(DyeItem.DyeColor color) {
		super(MyUtils.capitalizeFully(color.toString().replace('_', ' ')) + " Bed", SPRITES.get(color), ITEM_SPRITES.get(color), 3, 2);
		this.color = color;
	}

	/**
	 * Called when the player attempts to get in bed.
	 */
	public boolean use(Player player) {
		if (checkCanSleep(player)) { // If it is late enough in the day to sleep...

			// Set the player spawn coord. to their current position, in tile coords (hence " >> 4")
			player.spawnX = player.x >> Tile.TILE_SIZE_SHIFT;
			player.spawnY = player.y >> Tile.TILE_SIZE_SHIFT;

			SLEEPING_PLAYERS.put(player, this);
			player.remove();

			playersAwake = 0;
		}

		return true;
	}

	public static boolean checkCanSleep(Player player) {
		if (inBed(player)) return false;

		if (!(Updater.tickCount >= Updater.SLEEP_START_TIME || Updater.tickCount < Updater.SLEEP_END_TIME && Updater.pastDay1)) {
			// It is too early to sleep; display how much time is remaining.
			int sec = (int) Math.ceil((Updater.SLEEP_START_TIME - Updater.tickCount) * 1.0 / Updater.NORM_SPEED); // gets the seconds until sleeping is allowed. // normSpeed is in tiks/sec.
			String note = Localization.getLocalized("minicraft.notification.cannot_sleep", sec / 60, sec % 60);
			Game.notifications.add(note); // Add the notification displaying the time remaining in minutes and seconds.

			return false;
		}

		return true;
	}

	public static boolean sleeping() {
		return playersAwake == 0;
	}

	public static boolean inBed(Player player) {
		return SLEEPING_PLAYERS.containsKey(player);
	}

	public static Level getBedLevel(Player player) {
		Bed bed = SLEEPING_PLAYERS.get(player);
		if (bed == null)
			return null;
		return bed.getLevel();
	}

	// Get the player "out of bed"; used on the client only.
	public static void removePlayer(Player player) {
		SLEEPING_PLAYERS.remove(player);
	}

	public static void removePlayers() {
		SLEEPING_PLAYERS.clear();
	}

	// Client should not call this.
	public static void restorePlayer(Player player) {
		Bed bed = SLEEPING_PLAYERS.remove(player);
		if (bed != null) {
			if (bed.getLevel() == null)
				Game.levels[Game.currentLevel].add(player);
			else
				bed.getLevel().add(player);

			playersAwake = 1;
		}
	}

	// Client should not call this.
	public static void restorePlayers() {
		for (Player p : SLEEPING_PLAYERS.keySet()) {
			Bed bed = SLEEPING_PLAYERS.get(p);
			bed.getLevel().add(p);
		}

		SLEEPING_PLAYERS.clear();

		playersAwake = 1;
	}
}
