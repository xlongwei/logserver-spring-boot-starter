package com.xlongwei.logserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

/**
 * <pre>
 * management.endpoints.web.exposure.include=logserver
 * POST /actuator/logserver
 * </pre>
 * @author xlongwei
 */
@Endpoint(id = "logserver")
public class LogserverEndpoint {

	private static org.slf4j.Logger log = LoggerFactory.getLogger(LogserverEndpoint.class);
	private LogserverProperties logserverProperties;
	
	public LogserverEndpoint(LogserverProperties logserverProperties) {
		this.logserverProperties = logserverProperties;
	}
	
	@WriteOperation
	public Object log(@Nullable String logger, @Nullable String level, @Nullable String token) {
		boolean invalidToken = StringUtils.hasLength(logserverProperties.getToken()) && !logserverProperties.getToken().equals(token);
		if(!logserverProperties.isEnabled() || invalidToken) {
			return null;
		}
		LoggerContext lc = (LoggerContext)LoggerFactory.getILoggerFactory();
		List<Logger> loggers = null;
		if(StringUtils.hasText(logger)) {
			Logger theLogger = lc.getLogger(logger);
			if(theLogger != null) {
				loggers = Arrays.asList(theLogger);
			}
			if(StringUtils.hasText(level) && theLogger!=null) {
				Level theLevel = Level.toLevel(level, null);
				if(theLevel != null) {
					log.info("change logger:{} level from:{} to:{}", theLogger.getName(), theLogger.getLevel(), level);
					theLogger.setLevel(theLevel);
				}
			}
		}
		if(loggers==null) {
			loggers = lc.getLoggerList();
		}
		log.info("check logger level, loggers:{}", loggers.size());
		List<Map<String, String>> list = new ArrayList<>();
		for(Logger theLogger : loggers) {
			HashMap<String, String> map = new HashMap<>();
			map.put("logger", theLogger.getName());
			map.put("level", Objects.toString(theLogger.getLevel(), ""));
			list.add(map);
		}
		return Collections.singletonMap("loggers", list);
	}
}
