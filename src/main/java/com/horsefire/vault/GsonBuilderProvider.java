package com.horsefire.vault;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.inject.Provider;
import com.horsefire.vault.couch.VaultDocument;

public class GsonBuilderProvider implements Provider<GsonBuilder> {

	public GsonBuilder get() {
		return new GsonBuilder().registerTypeAdapter(
				VaultDocument.Direction.class,
				new EnumTypeAdapter<VaultDocument.Direction>(
						VaultDocument.Direction.values()));
	}

	private static final class EnumTypeAdapter<T extends Enum<T>> implements
			JsonSerializer<T>, JsonDeserializer<T> {

		private final Map<String, T> m_lowercaseToConstant = new HashMap<String, T>();

		public EnumTypeAdapter(T... options) {
			for (T constant : options) {
				m_lowercaseToConstant.put(constant.name().toLowerCase(),
						constant);
			}
		}

		public JsonElement serialize(T src, Type typeOfSrc,
				JsonSerializationContext context) {
			return new JsonPrimitive(src.name().toLowerCase());
		}

		@SuppressWarnings({ "cast" })
		public T deserialize(JsonElement json, Type classOfT,
				JsonDeserializationContext context) throws JsonParseException {
			String jsonString = json.toString().toLowerCase();
			if (jsonString.charAt(0) == '"'
					&& jsonString.charAt(jsonString.length() - 1) == '"') {
				jsonString = jsonString.substring(1, jsonString.length() - 1);
			}
			T constant = m_lowercaseToConstant.get(jsonString);
			if (constant == null) {
				throw new JsonParseException("Sorry, '" + json.toString()
						+ "' is not a valid " + classOfT);
			}
			return constant;
		}

		@Override
		public String toString() {
			return EnumTypeAdapter.class.getSimpleName();
		}
	}
}
