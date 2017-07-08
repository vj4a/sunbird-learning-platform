package org.ekstep.contentstore.util;

import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.ilimi.common.logger.LogHelper;
import com.ilimi.common.util.ILogger;
import com.ilimi.common.util.PlatformLogger;
import com.ilimi.common.util.PlatformLogger;;
import com.ilimi.common.util.PlatformLogManager;
import com.ilimi.common.util.PlatformLogger;
import com.ilimi.graph.common.mgr.Configuration;

/**
 * Class used for connecting to Cassandra database.
 * 
 * @author rayulu
 */
public class CassandraConnector {

	/** Cassandra Cluster. */
	private static Cluster cluster;

	/** Cassandra Session. */
	private static Session session;

	/** The Logger object. */
	private static ILogger LOGGER = PlatformLogManager.getLogger();

	static {
		// Connect to Cassandra Cluster specified by provided node IP address
		// and port number in cassandra.properties file
		try (InputStream inputStream = Configuration.class.getClassLoader()
				.getResourceAsStream("cassandra.properties")) {
			if (null != inputStream) {
				Properties props = new Properties();
				props.load(inputStream);
				String host = props.getProperty("cassandra.host");
				if (StringUtils.isBlank(host))
					host = "localhost";
				int port = -1;
				String portConfig = props.getProperty("cassandra.port");
				if (StringUtils.isNotBlank(portConfig)) {
					try {
						port = Integer.parseInt(portConfig);
					} catch (Exception e) {
					}
				}
				if (port < 0)
					port = 9042;
				cluster = Cluster.builder().addContactPoint(host).withPort(port).build();
				session = cluster.connect();
				registerShutdownHook();
			}
		} catch (Exception e) {
			LOGGER.log("Error! While Loading Cassandra Properties.", e.getMessage(), e);
		}
	}

	/**
	 * Provide my Session.
	 * 
	 * @return My session.
	 */
	public static Session getSession() {
		return session;
	}

	/**
	 * Close connection with the cluster.
	 * 
	 */
	public static void close() {
		session.close();
		cluster.close();
	}

	/**
	 * Register JVM shutdown hook to close cassandra open session.
	 */
	private static void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				LOGGER.log("Shutting down Cassandra connector session");
				CassandraConnector.close();
			}
		});
	}

}
