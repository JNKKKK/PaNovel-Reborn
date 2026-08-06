package cc.aoeiuv020.panovel.log;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.Logger;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SLF4J 2.x {@link SLF4JServiceProvider} that binds the facade to Timber.
 *
 * <p>Registered via {@code META-INF/services/org.slf4j.spi.SLF4JServiceProvider} and
 * discovered by SLF4J's {@code ServiceLoader} lookup at runtime. Replaces the abandoned
 * {@code slf4j-android} binding, which never shipped an SLF4J 2.x release.
 */
public final class TimberSlf4jServiceProvider implements SLF4JServiceProvider {
    // The SLF4J API version this provider is built against (major.minor is what SLF4J checks).
    private static final String REQUESTED_API_VERSION = "2.0.99";

    private final ConcurrentMap<String, Logger> loggers = new ConcurrentHashMap<>();
    private ILoggerFactory loggerFactory;
    private IMarkerFactory markerFactory;
    private MDCAdapter mdcAdapter;

    @Override public ILoggerFactory getLoggerFactory() { return loggerFactory; }
    @Override public IMarkerFactory getMarkerFactory() { return markerFactory; }
    @Override public MDCAdapter getMDCAdapter() { return mdcAdapter; }
    @Override public String getRequestedApiVersion() { return REQUESTED_API_VERSION; }

    @Override
    public void initialize() {
        loggerFactory = name -> loggers.computeIfAbsent(name, TimberLogger::new);
        markerFactory = new BasicMarkerFactory();
        mdcAdapter = new NOPMDCAdapter();
    }
}
