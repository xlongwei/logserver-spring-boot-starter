package com.xlongwei.logserver;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import ch.qos.logback.core.util.Duration;

/**
 * logserver 配置
 * @author xlongwei
 *
 */
@Configuration
@ConfigurationProperties(prefix = "logserver")
public class LogserverProperties {
	/**
	 * 是否开启logserver
	 */
	private boolean enabled = true;
	/**
	 * logserver域名或ip地址，默认"logserver"，需配合/etc/hosts使用
	 */
	private String remoteHost = "logserver";
	/**
	 * 端口号，默认6000
	 */
	private int port = 6000;
	/**
	 * 默认10240，配置SocketAppender队列大小
	 */
	private int queueSize = 10240;
	/**
	 * 自动重试间隔，默认10秒
	 */
	private Duration reconnectionDelay = new Duration(10000);
	@Override
	public String toString() {
		return new StringBuilder("logserver[enabled=").append(enabled)
				.append(",remoteHost=").append(remoteHost)
				.append(",port=").append(port)
				.append(",queueSize=").append(queueSize)
				.append(",reconnectionDelay=").append(reconnectionDelay)
				.append("]").toString();
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public String getRemoteHost() {
		return remoteHost;
	}
	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public int getQueueSize() {
		return queueSize;
	}
	public void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}
	public Duration getReconnectionDelay() {
		return reconnectionDelay;
	}
	public void setReconnectionDelay(Duration reconnectionDelay) {
		this.reconnectionDelay = reconnectionDelay;
	}
}
