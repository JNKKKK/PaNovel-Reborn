package cc.aoeiuv020.panovel.log;

import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.helpers.MessageFormatter;

import timber.log.Timber;

/**
 * An SLF4J {@link org.slf4j.Logger} that forwards to Timber.
 *
 * <p>The pure-Java modules (scraper, bookfile) log through the SLF4J facade because
 * Timber is Android-only and can't be a dependency there. This binding, provided by
 * {@link TimberSlf4jServiceProvider}, funnels those calls into Timber on-device so all
 * logging goes through a single pipeline.
 *
 * <p>SLF4J's {@code {}} placeholders are expanded here before handing the finished
 * string to Timber (with no format args, so a literal {@code %} in a message is safe).
 */
final class TimberLogger extends LegacyAbstractLogger {
    TimberLogger(String name) {
        this.name = name;
    }

    // Enablement is delegated to Timber's planted trees (none planted = no-op).
    @Override public boolean isTraceEnabled() { return true; }
    @Override public boolean isDebugEnabled() { return true; }
    @Override public boolean isInfoEnabled() { return true; }
    @Override public boolean isWarnEnabled() { return true; }
    @Override public boolean isErrorEnabled() { return true; }

    @Override
    protected String getFullyQualifiedCallerName() {
        return null;
    }

    @Override
    protected void handleNormalizedLoggingCall(Level level, Marker marker, String messagePattern,
                                               Object[] arguments, Throwable throwable) {
        String message = MessageFormatter.basicArrayFormat(messagePattern, arguments);
        Timber.Tree tree = Timber.tag(name);
        switch (level) {
            case ERROR:
                if (throwable != null) tree.e(throwable, message); else tree.e(message);
                break;
            case WARN:
                if (throwable != null) tree.w(throwable, message); else tree.w(message);
                break;
            case INFO:
                if (throwable != null) tree.i(throwable, message); else tree.i(message);
                break;
            default: // DEBUG, TRACE
                if (throwable != null) tree.d(throwable, message); else tree.d(message);
                break;
        }
    }
}
