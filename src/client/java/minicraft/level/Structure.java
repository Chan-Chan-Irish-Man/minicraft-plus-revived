package minicraft.level;

import minicraft.entity.furniture.Crafter;
import minicraft.entity.furniture.DungeonChest;
import minicraft.entity.furniture.Furniture;
import minicraft.entity.furniture.KnightStatue;
import minicraft.entity.furniture.Lantern;
import minicraft.gfx.Point;
import minicraft.level.tile.Tile;
import minicraft.level.tile.Tiles;

import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Consumer;

// this stores structures that can be drawn at any location.
public class Structure {

	private HashSet<TilePoint> tiles;
	private HashMap<Point, Furniture> furniture;

	public Structure() {
		tiles = new HashSet<>();
		furniture = new HashMap<>();
	}

	public Structure(Structure struct) {
		this.tiles = struct.tiles;
		this.furniture = struct.furniture;
	}

	public void setTile(int x, int y, Tile tile) {
		tiles.add(new TilePoint(x, y, tile));
	}

	public void addFurniture(int x, int y, Furniture furniture) {
		this.furniture.put(new Point(x, y), furniture);
	}

	public void draw(Level level, int xt, int yt) { draw(level, xt, yt, f -> { }); }

	public void draw(Level level, int xt, int yt, Consumer<Furniture> furnitureHandler) {
		for (TilePoint p : tiles)
			level.setTile(xt + p.x, yt + p.y, p.t);

		for (Point p : furniture.keySet()) {
			Furniture fur = furniture.get(p).copy();
			furnitureHandler.accept(fur);
			level.add(fur, xt + p.x, yt + p.y, true);
		}
	}

	public void draw(ChunkManager map, int xt, int yt) {
		for (TilePoint p : tiles)
			map.setTile(xt + p.x, yt + p.y, p.t, 0);
	}

	public void setData(String keys, String data) {
		// So, the keys are single letters, each letter represents a tile
		HashMap<String, String> keyPairs = new HashMap<>();
		String[] stringKeyPairs = keys.split(",");

		// Puts all the keys in the keyPairs HashMap
		for (int i = 0; i < stringKeyPairs.length; i++) {
			String[] thisKey = stringKeyPairs[i].split(":");
			keyPairs.put(thisKey[0], thisKey[1]);
		}

		String[] dataLines = data.split("\n");
		int width = dataLines[0].length();
		int height = dataLines.length;

		for (int i = 0; i < dataLines.length; i++) {
			for (int c = 0; c < dataLines[i].length(); c++) {
				if (dataLines[i].charAt(c) != '*') {
					Tile tile = Tiles.get(keyPairs.get(String.valueOf(dataLines[i].charAt(c))));
					this.setTile(-width / 2 + i, -height / 2 + c, tile);
				}
			}
		}
	}

	static class TilePoint {
		int x, y;
		Tile t;

		public TilePoint(int x, int y, Tile tile) {
			this.x = x;
			this.y = y;
			this.t = tile;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof TilePoint)) return false;
			TilePoint p = (TilePoint) o;
			return x == p.x && y == p.y && t.id == p.t.id;
		}

		@Override
		public int hashCode() {
			return x + y * 51 + t.id * 131;
		}
	}

	static final Structure DUNGEON_GATE;
	static final Structure DUNGEON_LOCK;
	static final Structure DUNGEON_BOSS_ROOM;
	static final Structure LAVA_POOL;
	static final Structure ORNATE_LAVA_POOL;
	static final Structure DUNGEON_GARDEN;
	static final Structure DUNGEON_CHEST;
	static final Structure DUNGEON_SPAWNER;

	// All the "mobDungeon" structures are for the spawner structures
	static final Structure MOB_DUNGEON_CENTER;
	static final Structure MOB_DUNGEON_NORTH;
	static final Structure MOB_DUNGEON_SOUTH;
	static final Structure MOB_DUNGEON_EAST;
	static final Structure MOB_DUNGEON_WEST;

	static final Structure AIR_WIZARD_HOUSE;

	// Used for random villages
	static final Structure VILLAGE_HOUSE_NORMAL;
	static final Structure VILLAGE_HOUSE_TWO_DOOR;

	static final Structure VILLAGE_RUINED_OVERLAY_1;
	static final Structure VILLAGE_RUINED_OVERLAY_2;

	// Ok, because of the way the system works, these structures are rotated 90 degrees clockwise when placed
	// Then it's flipped on the vertical
	static {
		DUNGEON_GATE = new Structure();
		DUNGEON_GATE.setData("O:Obsidian,D:Obsidian Door,W:Obsidian Wall",
			"WWDWW\n" +
				"WOOOW\n" +
				"DOOOD\n" +
				"WOOOW\n" +
				"WWDWW"
		);
		DUNGEON_GATE.addFurniture(-1, -1, new Lantern(Lantern.Type.IRON));

		DUNGEON_LOCK = new Structure();
		DUNGEON_LOCK.setData("O:Obsidian,W:Obsidian Wall",
			"WWWWW\n" +
				"WOOOW\n" +
				"WOOOW\n" +
				"WOOOW\n" +
				"WWWWW"
		);
		DUNGEON_BOSS_ROOM = new Structure();
		DUNGEON_BOSS_ROOM.setData("O:Obsidian Boss Floor,D:Obsidian Boss Door,W:Obsidian Boss Wall",
			"WWWWDWWWW\n" +
				"WOOOOOOOW\n" +
				"WOOOOOOOW\n" +
				"WOOOOOOOW\n" +
				"DOOOOOOOD\n" +
				"WOOOOOOOW\n" +
				"WOOOOOOOW\n" +
				"WOOOOOOOW\n" +
				"WWWWDWWWW"
		);
		DUNGEON_BOSS_ROOM.addFurniture(0, 0, new KnightStatue(5000));

		DUNGEON_SPAWNER = new Structure();
		DUNGEON_SPAWNER.setData("F:Grass,W:Obsidian Wall,O:Ornate Obsidian,D:Obsidian Door",
			"WWWDWWW\n" +
				"WOOOOOW\n" +
				"WOFFFOW\n" +
				"DOFFFOD\n" +
				"WOFFFOW\n" +
				"WOOOOOW\n" +
				"WWWDWWW"
		);

		LAVA_POOL = new Structure();
		LAVA_POOL.setData("L:Lava",
			"LL\n" +
				"LL"
		);

		ORNATE_LAVA_POOL = new Structure();
		ORNATE_LAVA_POOL.setData("L:Lava,W:Obsidian Wall,O:Ornate Obsidian,D:Obsidian Door",
			"WWWDWWW\n" +
				"WOOOOOW\n" +
				"WOLLLOW\n" +
				"DOLLLOD\n" +
				"WOLLLOW\n" +
				"WOOOOOW\n" +
				"WWWDWWW"
		);

		DUNGEON_GARDEN = new Structure();
		DUNGEON_GARDEN.setData("F:Flower,W:Obsidian Wall,O:Ornate Obsidian,D:Obsidian Door",
			"WWWDWWW\n" +
				"WOOOOOW\n" +
				"WOFFFOW\n" +
				"DOFFFOD\n" +
				"WOFFFOW\n" +
				"WOOOOOW\n" +
				"WWWDWWW"
		);

		DUNGEON_CHEST = new Structure();
		DUNGEON_CHEST.setData("F:Grass,W:Obsidian Wall,O:Ornate Obsidian,D:Obsidian Door",
			"WWWDWWW\n" +
				"WOOOOOW\n" +
				"WOFFFOW\n" +
				"DOFFFOD\n" +
				"WOFFFOW\n" +
				"WOOOOOW\n" +
				"WWWDWWW"
		);
		DUNGEON_CHEST.addFurniture(0, 0, new DungeonChest(null));

		MOB_DUNGEON_CENTER = new Structure();
		MOB_DUNGEON_CENTER.setData("B:Stone Bricks,W:Stone Wall",
			"WWBWW\n" +
				"WBBBW\n" +
				"BBBBB\n" +
				"WBBBW\n" +
				"WWBWW"
		);
		MOB_DUNGEON_NORTH = new Structure();
		MOB_DUNGEON_NORTH.setData("B:Stone Bricks,W:Stone Wall",
			"WWWWW\n" +
				"WBBBB\n" +
				"BBBBB\n" +
				"WBBBB\n" +
				"WWWWW"
		);
		MOB_DUNGEON_SOUTH = new Structure();
		MOB_DUNGEON_SOUTH.setData("B:Stone Bricks,W:Stone Wall",
			"WWWWW\n" +
				"BBBBW\n" +
				"BBBBB\n" +
				"BBBBW\n" +
				"WWWWW"
		);
		MOB_DUNGEON_EAST = new Structure();
		MOB_DUNGEON_EAST.setData("B:Stone Bricks,W:Stone Wall",
			"WBBBW\n" +
				"WBBBW\n" +
				"WBBBW\n" +
				"WBBBW\n" +
				"WWBWW"
		);
		MOB_DUNGEON_WEST = new Structure();
		MOB_DUNGEON_WEST.setData("B:Stone Bricks,W:Stone Wall",
			"WWBWW\n" +
				"WBBBW\n" +
				"WBBBW\n" +
				"WBBBW\n" +
				"WBBBW"
		);

		AIR_WIZARD_HOUSE = new Structure();
		AIR_WIZARD_HOUSE.setData("F:Wood Planks,W:Wood Wall,D:Wood Door",
			"WWWWWWW\n" +
				"WFFFFFW\n" +
				"DFFFFFW\n" +
				"WFFFFFW\n" +
				"WWWWWWW"
		);
		AIR_WIZARD_HOUSE.addFurniture(-2, 0, new Lantern(Lantern.Type.GOLD));
		AIR_WIZARD_HOUSE.addFurniture(0, 0, new Crafter(Crafter.Type.Enchanter));

		VILLAGE_HOUSE_NORMAL = new Structure();
		VILLAGE_HOUSE_NORMAL.setData("F:Wood Planks,W:Wood Wall,D:Wood Door,G:Grass",
			"WWWWW\n" +
				"WFFFW\n" +
				"WFFFD\n" +
				"WFFFG\n" +
				"WWWWW"
		);

		VILLAGE_HOUSE_TWO_DOOR = new Structure();
		VILLAGE_HOUSE_TWO_DOOR.setData("F:Wood Planks,W:Wood Wall,D:Wood Door,G:Grass",
			"WWWWW\n" +
				"WFFFW\n" +
				"DFFFW\n" +
				"WFFFW\n" +
				"WWDWW"
		);

		VILLAGE_RUINED_OVERLAY_1 = new Structure();
		VILLAGE_RUINED_OVERLAY_1.setData("G:Grass,F:Wood Planks",
			"**FG*\n" +
				"F*GG*\n" +
				"*G**F\n" +
				"G*G**\n" +
				"***G*"
		);

		VILLAGE_RUINED_OVERLAY_2 = new Structure();
		VILLAGE_RUINED_OVERLAY_2.setData("G:Grass,F:Wood Planks",
			"F**G*\n" +
				"*****\n" +
				"*GG**\n" +
				"F**G*\n" +
				"*F**G"
		);
	}
}
