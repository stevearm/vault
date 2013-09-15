package com.horsefire.vault;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class VaultModule extends AbstractModule {

	private final Options m_options;

	public VaultModule(Options options) {
		m_options = options;
	}

	@Override
	protected void configure() {
		bind(Options.class).toInstance(m_options);
		install(new FactoryModuleBuilder().implement(Sentinel.class,
				Sentinel.class).build(Sentinel.Factory.class));
	}
}
