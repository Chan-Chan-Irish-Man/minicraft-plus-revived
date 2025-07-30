package minicraft.item;

import minicraft.saveload.Load;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FishingData {

	public static final List<String> FISH_DATA = getData("fish");

	public static final List<String> TOOL_DATA = getData("tool");

	public static final List<String> JUNK_DATA = getData("junk");

	public static final List<String> RARE_DATA = getData("rare");

	public static List<String> getData(String name) {
		List<String> data;
		try {
			data = Load.loadFile("/resources/data/fishing/" + name + "_loot.txt");
		} catch (IOException e) {
			e.printStackTrace();
			data = new ArrayList<>();
		}
		return data;
	}
}
