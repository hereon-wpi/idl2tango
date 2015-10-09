package hzg.wpn.idl;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.spi.AppenderAttachable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Heavily depends on logback
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 17.09.2015
 */
public class LoggerConfiguration {
    private LoggerConfiguration(){}

    public static Logger createLogger(Class<?> clazz){
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        //setup file appender
        FileAppender<ILoggingEvent> fileAppender =
                new FileAppender<ILoggingEvent>();

        fileAppender.setName("file");
        //start new file each run
        fileAppender.setAppend(false);

        fileAppender.setFile((System.getenv("XENV_ROOT") != null ?
                System.getenv("XENV_ROOT") : System.getProperty("user.home"))  + "/idl2tango.log");

        PatternLayout pl = new PatternLayout();
        pl.setPattern("%p %d{dd-MM-yyyy HH:mm:ss,SSS} [%t - %C{1}] %m%n");
        pl.setContext(lc);
        pl.start();

        fileAppender.setContext(lc);
        fileAppender.setLayout(pl);

        fileAppender.start();
        //set levels
        lc.getLogger("org.jacorb").setLevel(Level.ERROR);
        lc.getLogger("org.tango").setLevel(Level.INFO);
        lc.getLogger("org.quartz").setLevel(Level.ERROR);
        lc.getLogger("net.sf.ehcache").setLevel(Level.ERROR);
        lc.getLogger("hzg.wpn.idl").setLevel(Level.TRACE);

        //asynchronous appender
        AsyncAppender asyncAppender = new AsyncAppender();
        asyncAppender.setName("async");
        //do not discard any events
        asyncAppender.setDiscardingThreshold(0);
        asyncAppender.setContext(lc);
        asyncAppender.addAppender(fileAppender);
        asyncAppender.start();

        //setup root logger
        ch.qos.logback.classic.Logger root = lc.getLogger("root");
        root.addAppender(asyncAppender);
        root.setLevel(Level.DEBUG);

        //disable console output because it pollutes IDL's console
        root.detachAppender("console");

        return LoggerFactory.getLogger(clazz);
    }

    public static void setLogFile(String file){
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        ch.qos.logback.classic.Logger root = lc.getLogger("root");

        Appender<?> async = root.getAppender("async");

        FileAppender<?> fileAppender = (FileAppender<?>)
                ((AppenderAttachable)async).getAppender("file");

        fileAppender.setFile(file);
        fileAppender.start();
    }

    public static void setLogLevel(Level level) {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        lc.getLogger("hzg.wpn.idl").setLevel(level);
    }
}
