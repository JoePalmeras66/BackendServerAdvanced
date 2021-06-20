package com.joepalmeras.buvp.BackendServer;

import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;

import com.joepalmeras.buvp.BackendServer.config.SecurityConfiguration;

public class Initializer extends AbstractHttpSessionApplicationInitializer {
	
	public Initializer() {
		super(SecurityConfiguration.class);
	}
	
}
