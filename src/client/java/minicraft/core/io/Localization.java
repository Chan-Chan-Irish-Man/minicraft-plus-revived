package minicraft.core.io;

import minicraft.util.Logging;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Pattern;

public class Localization {

	public static final Locale DEFAULT_LOCALE = Locale.US;
	public static final Locale DEBUG_LOCALE = Locale.ROOT; // This locale is used for debugging;

	public static boolean isDebugLocaleEnabled = false;
	public static boolean unlocalizedStringTracing = false;

	private static final Pattern NUMBER_REGEX = Pattern.compile("^[+-]?((\\d+(\\.\\d*)?)|(\\.\\d+))$");
	private static final HashMap<Locale, HashSet<String>> KNOWN_UNLOCALIZED_STRINGS = new HashMap<>();
	private static final HashMap<String, String> LOCALIZATION = new HashMap<>();

	private static Locale selectedLocale = DEFAULT_LOCALE;
	private static final HashMap<Locale, ArrayList<String>> UNLOADED_LOCALIZATION = new HashMap<>();
	private static final HashMap<Locale, LocaleInformation> LOCALE_INFO = new HashMap<>();

	/**
	 * Get the provided key's localization for the currently selected language.
	 * @param key The key to localize.
	 * @param arguments The additional arguments to format the localized string.
	 * @return A localized string.
	 */
	@NotNull
	public static String getLocalized(String key, Object... arguments) {
		if (key.matches("^ *$")) return key; // Blank, or just whitespace
		if (selectedLocale == DEBUG_LOCALE) return key;

		if (NUMBER_REGEX.matcher(key).matches()) {
			return key; // This is a number; don't try to localize it
		}

		String localString = LOCALIZATION.get(key);

		if (localString == null) {
			if (!KNOWN_UNLOCALIZED_STRINGS.containsKey(selectedLocale))
				KNOWN_UNLOCALIZED_STRINGS.put(selectedLocale, new HashSet<>());
			if (!KNOWN_UNLOCALIZED_STRINGS.get(selectedLocale).contains(key)) {
				Logger.tag("LOC").trace(unlocalizedStringTracing ? new Throwable("Tracing") : null, "{}: '{}' is unlocalized.", selectedLocale.toLanguageTag(), key);
				KNOWN_UNLOCALIZED_STRINGS.get(selectedLocale).add(key);
			}
		}

		if (localString != null) {
			localString = String.format(getSelectedLocale(), localString, arguments);
		}

		return (localString == null ? key : localString);
	}

	/**
	 * Gets the currently selected locale.
	 * @return A locale object.
	 */
	public static Locale getSelectedLocale() {
		return selectedLocale;
	}

	/**
	 * Get the currently selected locale, but as a full name without the country code.
	 * @return A string with the name of the language.
	 */
	@NotNull
	public static LocaleInformation getSelectedLanguage() {
		return LOCALE_INFO.get(selectedLocale);
	}

	/**
	 * Gets a  list of all the known locales.
	 * @return A list of locales.
	 */
	@NotNull
	public static LocaleInformation[] getLocales() {
		return LOCALE_INFO.values().toArray(new LocaleInformation[0]);
	}

	/**
	 * Changes the selected language and loads it.
	 * If the provided language doesn't exist, it loads the default locale.
	 * @param newLanguage The language-country code of the language to load.
	 */
	public static void changeLanguage(@NotNull String newLanguage) {
		changeLanguage(Locale.forLanguageTag(newLanguage));
	}

	/**
	 * @see #changeLanguage(String)
	 */
	public static void changeLanguage(@NotNull Locale newLanguage) {
		selectedLocale = newLanguage;
		loadLanguage();
	}

	/**
	 * This method gets the currently selected locale and loads it if it exists. If not, it loads the default locale.
	 * The loaded file is then parsed, and all the entries are added to a hashmap.
	 */
	public static void loadLanguage() {
		Logging.RESOURCEHANDLER_LOCALIZATION.trace("Loading language...");
		LOCALIZATION.clear();

		if (selectedLocale == DEBUG_LOCALE) return; // DO NOT load any localization for debugging.

		// Check if selected localization exists.
		if (!UNLOADED_LOCALIZATION.containsKey(selectedLocale))
			selectedLocale = DEFAULT_LOCALE;

		// Attempt to load the string as a json object.
		JSONObject json;
		for (String text : UNLOADED_LOCALIZATION.get(selectedLocale)) {
			json = new JSONObject(text);
			for (String key : json.keySet()) {
				LOCALIZATION.put(key, json.getString(key));
			}
		}

		// Language fallback
		if (!selectedLocale.equals(DEFAULT_LOCALE)) {
			for (String text : UNLOADED_LOCALIZATION.get(DEFAULT_LOCALE)) { // Getting default localization.
				json = new JSONObject(text);
				for (String key : json.keySet()) {
					if (!LOCALIZATION.containsKey(key)) { // The default localization is added only when the key is not existed.
						LOCALIZATION.put(key, json.getString(key));
					}
				}
			}
		}
	}

	public static void resetLocalizations() {
		// Clear array with localization files.
		UNLOADED_LOCALIZATION.clear();
		LOCALE_INFO.clear();
		if (isDebugLocaleEnabled) { // Adding the debug locale as an option.
			LOCALE_INFO.put(DEBUG_LOCALE, new LocaleInformation(DEBUG_LOCALE, "Debug", null));
		}
	}

	public static class LocaleInformation {
		public final Locale locale;
		public final String name;
		public final String region;

		public LocaleInformation(Locale locale, String name, String region) {
			this.locale = locale;
			this.name = name;
			this.region = region;
		}

		@Override
		public String toString() {
			if (region == null) return name;
			return String.format("%s (%s)", name, region);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) return false;
			if (obj instanceof LocaleInformation)
				return locale.equals(((LocaleInformation) obj).locale) &&
					name.equals(((LocaleInformation) obj).name) &&
					region.equals(((LocaleInformation) obj).region);
			return false;
		}
	}

	public static void addLocale(Locale loc, LocaleInformation info) {
		if (!LOCALE_INFO.containsKey(loc)) LOCALE_INFO.put(loc, info);
	}

	public static void addLocalization(Locale loc, String json) {
		if (!LOCALE_INFO.containsKey(loc)) return; // Only add when Locale Information is exist.
		if (!UNLOADED_LOCALIZATION.containsKey(loc))
			UNLOADED_LOCALIZATION.put(loc, new ArrayList<>());
		UNLOADED_LOCALIZATION.get(loc).add(json);
	}
}
