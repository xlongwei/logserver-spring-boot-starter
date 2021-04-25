package com.xlongwei.logserver;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.SocketAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextVO;

/**
 * LogserverAutoConfiguration
 * @author xlongwei
 *
 */
@Configuration
@EnableConfigurationProperties(LogserverProperties.class)
public class LogserverAutoConfiguration implements InitializingBean {
	private static final String ACTUATOR_ENDPOINT_FCN = "org.springframework.boot.actuate.endpoint.annotation.Endpoint";
	private static org.slf4j.Logger log = LoggerFactory.getLogger(LogserverAutoConfiguration.class);
	@Autowired Environment env;
	LogserverProperties logserverProperties;
	String contextName = null;
	Field nameField = null;

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
		}
		
		log.info("logserver config: {}", logserverProperties);
	}
	
	@Bean
	@ConditionalOnClass(name=ACTUATOR_ENDPOINT_FCN)
	@ConditionalOnMissingBean
	public LogserverEndpoint logserverEndpoint() {
		return new LogserverEndpoint(logserverProperties);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if(!logserverProperties.isEnabled()) {
			return;
		}
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
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
			log.info("logserver change contextName: {} => {}", loggerContext.getName(), contextName);
			try {
				loggerContext.setName(contextName);
				contextName = null;
			}catch(Exception e) {
				log.info("logserver change contextName failed: {}", e.getMessage());
			}
		}else {
			contextName = null;
		}
		if(ClassUtils.isPresent(ACTUATOR_ENDPOINT_FCN, null)) {
			regist(contextName!=null ? contextName : loggerContext.getName()+"@"+ip, ip);
		}else {
			log.info("actuator not present");
		}
	}
	
	private void regist(String name, String ip) {
		String url = logserverProperties.getRemoteAddress();
		if(StringUtils.isEmpty(url) || !url.startsWith("http")) {
			url = "http://" + logserverProperties.getRemoteHost() + ":9880";
		}
		try {
			String myUrl = "http://"+ip+":"+env.getProperty("server.port", Integer.class, 8080)+"/actuator/logserver";
			log.info("logserver regist: {}", myUrl);
			url += "/log?type=regist&name=" + name.replace('@', '-');
			log.info("POST {}", url);
			HttpURLConnection connection =(HttpURLConnection) new URL(url).openConnection();
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setRequestMethod("POST");
			connection.setUseCaches(false);
			connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
			connection.connect();
			DataOutputStream out = new DataOutputStream(connection.getOutputStream());
			String token = logserverProperties.getToken();
			String payload = "token=" + encode(token) + "&url=" + encode(myUrl);
			log.info("payload {}", payload);
			out.writeBytes(payload);
			out.flush();
			out.close();
			InputStream inputStream = connection.getInputStream();
			String string = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
			inputStream.close();
			log.info("logserver regist: {}", string);
		}catch(Exception e) {
			log.info("logserver regist: {} {}", e.getClass().getSimpleName(), e.getMessage());
		}
	}
	
	private String encode(String value) throws UnsupportedEncodingException {
		return value==null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8.name());
	}
}
