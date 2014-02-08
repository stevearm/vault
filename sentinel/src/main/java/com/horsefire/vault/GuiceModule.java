package com.horsefire.vault;

import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;

public class GuiceModule extends AbstractModule {

	private final CmdArgs m_cmdArgs;
	private final Quitter m_quitter;

	public GuiceModule(CmdArgs cmdArgs, Quitter quitter) {
		m_cmdArgs = cmdArgs;
		m_quitter = quitter;
	}

	private void bindMemberConstants() {
		bind(CmdArgs.class).toInstance(m_cmdArgs);
		bind(Quitter.class).toInstance(m_quitter);
	}

	@Override
	protected void configure() {
		bindMemberConstants();

		bind(GsonBuilder.class).toProvider(GsonBuilderProvider.class);
	}
}
