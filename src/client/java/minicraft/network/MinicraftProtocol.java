package minicraft.network;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface MinicraftProtocol {

	int PORT = 4225;

	enum InputType {
		INVALID, PING, USERNAMES, LOGIN, GAME, INIT, LOAD, TILES, ENTITIES, TILE, ENTITY, PLAYER, MOVE, ADD, REMOVE, DISCONNECT, SAVE, NOTIFY, INTERACT, PUSH, PICKUP, CHESTIN, CHESTOUT, ADDITEMS, BED, POTION, HURT, DIE, RESPAWN, DROP, STAMINA, SHIRT, STOPFISHING;

		public static final InputType[] VALUES = InputType.values();
		public static final List<InputType> SERVER_ONLY = Arrays.asList(INIT, TILES, ENTITIES, ADD, REMOVE, HURT, GAME, ADDITEMS, STAMINA, STOPFISHING);
		public static final List<InputType> ENTITY_UPDATES = Arrays.asList(ENTITY, ADD, REMOVE);
		public static final List<InputType> TILE_UPDATES = Collections.singletonList(TILE);
	}
}
