package com.horsefire.vault;

import com.google.gson.Gson;
import com.google.inject.Provider;

public class GsonProvider implements Provider<Gson> {

	public Gson get() {
		return new Gson();
	}
}
