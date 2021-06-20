package com.joepalmeras.buvp.BackendServer.Listener;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceSessionListener implements HttpSessionListener {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private static int totalActiveSessions;
	
	public static int getTotalActiveSession(){
	  return totalActiveSessions;
	}
	
	@Override
	public void sessionCreated(HttpSessionEvent arg0) {
	  totalActiveSessions++;
	  logger.info("sessionCreated - add one session into counter");
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent arg0) {
	  totalActiveSessions--;
	  logger.info("sessionDestroyed - deduct one session from counter");
	}	
	
}
