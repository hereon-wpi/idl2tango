package hzg.wpn.idl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
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

        //setup root logger
        ch.qos.logback.classic.Logger root = lc.getLogger("root");
        root.addAppender(fileAppender);
        root.setLevel(Level.DEBUG);

        //disable console output
        root.detachAppender("console");
//        Appender<ILoggingEvent> consoleAppender = root.getAppender("console");
//
//        consoleAppender.stop();
//        LevelFilter levelFilter = new LevelFilter();
//        levelFilter.setLevel(Level.ERROR);
//        levelFilter.setOnMatch(FilterReply.ACCEPT);
//        levelFilter.setOnMismatch(FilterReply.DENY);
//        consoleAppender.addFilter(levelFilter);
//        consoleAppender.start();

        return LoggerFactory.getLogger(clazz);
    }
}
