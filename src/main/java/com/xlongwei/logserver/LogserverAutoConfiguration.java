package com.xlongwei.logserver;

import java.lang.reflect.Field;
import java.net.InetAddress;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.SocketAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextVO;

@Configuration
@EnableConfigurationProperties(LogserverProperties.class)
public class LogserverAutoConfiguration implements InitializingBean {
	@Autowired Environment env;
	LogserverProperties logserverProperties;
	String contextName = null;
	Field nameField = null;
	boolean enabled = false;

	public LogserverAutoConfiguration(LogserverProperties logserverProperties) {
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
		this.logserverProperties = logserverProperties;
		
		if(logserverProperties.isEnabled()) {
			SocketAppender socketAppender = new SocketAppender();
			socketAppender.setContext(loggerContext);
			socketAppender.setRemoteHost(logserverProperties.getRemoteHost());
			socketAppender.setPort(logserverProperties.getPort());
			socketAppender.setQueueSize(logserverProperties.getQueueSize());
			socketAppender.setReconnectionDelay(logserverProperties.getReconnectionDelay());
			socketAppender.start();
			
			AsyncAppender asyncAppender = new AsyncAppender() {
				@Override
				protected void append(ILoggingEvent eventObject) {
					if(contextName != null) {
						LoggerContextVO loggerContextVO = eventObject.getLoggerContextVO();
						if(nameField == null) {
							nameField = ReflectionUtils.findField(LoggerContextVO.class, "name");
							ReflectionUtils.makeAccessible(nameField);
						}
						ReflectionUtils.setField(nameField, loggerContextVO, contextName);
					}
					super.append(eventObject);
				}
			};
			asyncAppender.setContext(loggerContext);
			asyncAppender.addAppender(socketAppender);
			asyncAppender.setIncludeCallerData(true);
			asyncAppender.setQueueSize(logserverProperties.getQueueSize());
			asyncAppender.start();
	
			rootLogger.addAppender(asyncAppender);
			this.enabled = true;
		}
		
		rootLogger.info("logserver config: {}", logserverProperties);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if(this.enabled == false) {
			return;
		}
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
		// if <contextName> is missing, using spring.application.name@ip instead
		contextName = loggerContext.getName();
		boolean appendIp = true;
		if("default".equals(contextName) || "nacos".equals(contextName)) {
			String applicationName = env.getProperty("spring.application.name");
			if(StringUtils.hasText(applicationName)) {
				contextName = applicationName;
			}else {
				appendIp = false;
			}
		}
		String ip = null;
		try{
			ip = InetAddress.getLocalHost().getHostAddress();
		}catch(Exception e) {
			//ignore
		}
		if(appendIp && StringUtils.hasText(ip)) {
			contextName += "@" + ip;
			rootLogger.info("logserver change contextName: {} => {}", loggerContext.getName(), contextName);
			try {
				loggerContext.setName(contextName);
				contextName = null;
			}catch(Exception e) {
				rootLogger.info("logserver change contextName failed: {}", e.getMessage());
			}
		}else {
			contextName = null;
		}
	}
}
