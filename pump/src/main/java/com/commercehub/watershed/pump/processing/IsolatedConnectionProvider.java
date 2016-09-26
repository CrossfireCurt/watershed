package com.commercehub.watershed.pump.processing;

import com.amazonaws.util.IOUtils;
import com.github.davidmoten.rx.jdbc.ConnectionProvider;
import com.github.davidmoten.rx.jdbc.exceptions.SQLRuntimeException;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * ConnectionProvider that wraps a custom DataSource to talk to a conflicting version of Drill libraries.
 * Loads DataSource class from an external jar (watershed-pump-support.shadow.jar).
 */
public class IsolatedConnectionProvider {
    /**
     * Loads a ConnectionProvider by utilizing the DataSource class from an external jar
     * (watershed-pump-support.shadow.jar).
     *
     * @param jdbcUrl
     * @param jdbcUsername
     * @param jdbcPassword
     * @param driverClass
     * @return a new ConnectionProvider
     */
    public static ConnectionProvider get(final String jdbcUrl, final String jdbcUsername,
                                         final String jdbcPassword, final String driverClass) {
        try {
            URL supportUrl = IsolatedConnectionProvider.class.getResource("/watershed-pump-support.shadow.jar");
            if (supportUrl == null) {
                throw new IllegalStateException("Support jar not found, cannot continue!");
            }

            String tDir = System.getProperty("java.io.tmpdir");
            File tempJar = new File(tDir + "/watershed-pump-support-shadow.jar");

            if(!tempJar.exists()){
                InputStream in = supportUrl.openStream();
                FileOutputStream out = new FileOutputStream(tempJar);
                IOUtils.copy(in, out);
                out.close();
                in.close();
            }

            URLClassLoader jdbcLoader = new URLClassLoader(new URL[]{ tempJar.toURI().toURL() }, null);
            Class<?> dsClass = jdbcLoader.loadClass("com.commercehub.watershed.pump.support.DataSource");
            final DataSource ds = (DataSource)
                    dsClass.getConstructor(String.class, String.class).newInstance(driverClass, jdbcUrl);
            return new ConnectionProvider() {
                @Override
                public Connection get() {
                    try {
                        return ds.getConnection(jdbcUsername, jdbcPassword);
                    } catch (SQLException e) {
                        throw new SQLRuntimeException(e);
                    }
                }

                @Override
                public void close() {
                    //no-op
                }
            };
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }
}
