/*
 * The main contributor to this project is Institute of Materials Research,
 * Helmholtz-Zentrum Geesthacht,
 * Germany.
 *
 * This project is a contribution of the Helmholtz Association Centres and
 * Technische Universitaet Muenchen to the ESS Design Update Phase.
 *
 * The project's funding reference is FKZ05E11CG1.
 *
 * Copyright (c) 2012. Institute of Materials Research,
 * Helmholtz-Zentrum Geesthacht,
 * Germany.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package hzg.wpn.idl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.filter.LevelFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.spi.FilterReply;
import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.DevSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tango.client.ez.data.EnumDevState;
import org.tango.client.ez.data.type.TangoImage;
import org.tango.client.ez.proxy.TangoProxies;
import org.tango.client.ez.proxy.TangoProxy;
import org.tango.client.ez.proxy.TangoProxyException;
import org.tango.client.ez.util.TangoUtils;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class provides simplified interface of {@link fr.esrf.TangoApi.DeviceProxy} to be used through IDL Java importing
 * It also adds a couple of new features such as {@link this#waitUntil}
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 05.06.12
 */
public class IDLDeviceProxy {
    private static final Logger logger = LoggerFactory.getLogger(IDLDeviceProxy.class);

    //configure logback logging
    static {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        //setup file appender
        FileAppender<ILoggingEvent> fileAppender =
                new FileAppender<ILoggingEvent>();

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
    }

    private static final TangoProxyExceptionHandler handler = new TangoProxyExceptionHandler(logger);

    final TangoProxy proxy;
    final TangoDeviceAttributeReader reader;
    final TangoDeviceAttributeWriter writer;
    final TangoDeviceCommandExecutor executor;
    final TangoDevStateAwaitor awaitor;
    final AtomicReference<Exception> lastException = new AtomicReference<Exception>(new Exception("No exceptions so far."));

    /**
     * Creates a new instance of the IDLDeviceProxy.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy = OBJ_NEW("IDLJavaObject$hzg_wpn_idl_IDLDeviceProxy", "hzg.wpn.idl.IDLDeviceProxy", "sys/tg_test/1")
     * </code>
     *
     * @param name a Tango device full name, e.g. tango://{TANGO_HOST}/{SERVER_NAME}/{DOMAIN}/{DEVICE_ID}
     * @throws IDLDeviceProxyRuntimeException
     */
    public IDLDeviceProxy(String name) {
        this(name, false);
    }

    /**
     * Creates a new instance of the IDLDeviceProxy.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy = OBJ_NEW("IDLJavaObject$hzg_wpn_idl_IDLDeviceProxy", "hzg.wpn.idl.IDLDeviceProxy", "sys/tg_test/1", true)
     * </code>
     *
     * @param name                  a Tango device full name, e.g. tango://{TANGO_HOST}/{SERVER_NAME}/{DOMAIN}/{DEVICE_ID}
     * @param useEventsForWaitUntil true if event driven WaitUntil is desired
     * @throws IDLDeviceProxyRuntimeException
     */
    public IDLDeviceProxy(String name, boolean useEventsForWaitUntil) {
        logger.debug("Creating proxy for device[{},useEventsForWaitUntil={}]", name, useEventsForWaitUntil);
        try {
            this.proxy = TangoProxies.newDeviceProxyWrapper(name);
            this.reader = new TangoDeviceAttributeReader(this.proxy, handler);
            this.writer = new TangoDeviceAttributeWriter(this.proxy, handler);
            this.executor = new TangoDeviceCommandExecutor(this.proxy, handler);
            this.awaitor = useEventsForWaitUntil ? new EventDevStateAwaitor(this.proxy, handler) : new PollDevStateAwaitor(this.proxy, handler);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }


    /**
     *
     * @return last exception message (localized)
     */
    public String getExceptionMessage() {
        return lastException.get().getLocalizedMessage();
    }

    /**
     *
     *
     * @return a tango version of the proxied tango device
     */
    public int getTangoVersion(){
        try {
            return proxy.toDeviceProxy().getTangoVersion();
        } catch (DevFailed devFailed) {
            Exception ex = TangoUtils.convertDevFailedToException(devFailed);
            lastException.set(ex);
            throw handler.handle(ex);
        }
    }

    /**
     * @param timeout in milliseconds
     */
    public void setTimeout(int timeout) {
        try {
            Field field = proxy.getClass().getDeclaredField("proxy");
            field.setAccessible(true);
            Object wrapped = field.get(proxy);

            wrapped.getClass().getMethod("set_timeout_millis", int.class).invoke(wrapped, timeout);
            field.setAccessible(false);
        } catch (Exception ex) {
            lastException.set(ex);
            throw handler.handle(ex);
        }
    }

    //==========================
    // waitUntil
    //==========================

    /**
     * Blocks current Thread (execution) until target Tango server reports desired state.
     * State is being checked every 100 milliseconds.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->waitUntil, "running"
     * </code>
     *
     * @param state desired state in String format, i.e. "ON","RUNNING" etc (case insensitive)
     * @throws IDLDeviceProxyRuntimeException
     */
    public void waitUntil(String state) {
        try {
            EnumDevState targetDevState = EnumDevState.valueOf(state.toUpperCase());
            awaitor.waitUntil(targetDevState);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Blocks current Thread (execution) until target Tango server reports state different from the specified.
     * State is being checked every 100 milliseconds.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->waitUntilNot, "moving"
     * </code>
     *
     * @param state state in String format, i.e. "ON","RUNNING" etc
     * @throws IDLDeviceProxyRuntimeException
     */
    public void waitUntilNot(String state) {
        try {
            EnumDevState targetDevState = EnumDevState.valueOf(state.toUpperCase());
            awaitor.waitUntilNot(targetDevState);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    //==========================
    // Execute commands
    //==========================

    /**
     * Executes command without an input argument.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joObject = joDeviceProxy->executeCommand( "SomeCommand" )
     * </code>
     *
     * @param command a command name
     * @return an Object that represents command execution result or null if output is defined to DevVoid
     * @throws IDLDeviceProxyRuntimeException
     */
    public Object executeCommand(String command) {
        try {
            return executor.command_inout(command);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link String}
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joObject = joDeviceProxy->executeCommand( "SomeCommand" , "Some Value" )
     * </code>
     *
     * @param command a command name
     * @return an Object that represents command execution result or null if output is defined to DevVoid
     * @throws IDLDeviceProxyRuntimeException
     */
    public Object executeCommand(String command, String value) {
        try {
            return executor.command_inout(command, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link double}
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joObject = joDeviceProxy->executeCommand( "SomeCommand", 0.125D )
     * </code>
     *
     * @param command a command name
     * @return an Object that represents command execution result or null if output is defined to DevVoid
     * @throws IDLDeviceProxyRuntimeException
     */
    public Object executeCommand(String command, double value) {
        try {
            return executor.command_inout(command, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link double[]}
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joObject = joDeviceProxy->executeCommand( "SomeCommand", DBLARR(3,1) )
     * </code>
     *
     * @param command a command name
     * @return an Object that represents command execution result or null if output is defined to DevVoid
     * @throws IDLDeviceProxyRuntimeException
     */
    public Object executeCommand(String command, double[] value) {
        try {
            return executor.command_inout(command, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link long[]}
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joObject = joDeviceProxy->executeCommand( "SomeCommand", LON64ARR(3,1) )
     * </code>
     *
     * @param command a command name
     * @return an Object that represents command execution result or null if output is defined to DevVoid
     * @throws IDLDeviceProxyRuntimeException
     */
    public Object executeCommand(String command, long[] value) {
        try {
            return executor.command_inout(command, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link long}
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joObject = joDeviceProxy->executeCommand( "SomeCommand", LON64ARR(3,1) )
     * </code>
     *
     * @param command a command name
     * @return an Object that represents command execution result or null if output is defined to DevVoid
     * @throws IDLDeviceProxyRuntimeException
     */
    public Object executeCommand(String command, long value) {
        try {
            return executor.command_inout(command, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link short}
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joObject = joDeviceProxy->executeCommand( "SomeCommand", LON64ARR(3,1) )
     * </code>
     *
     * @param command a command name
     * @return an Object that represents command execution result or null if output is defined to DevVoid
     * @throws IDLDeviceProxyRuntimeException
     */
    public Object executeCommand(String command, short value) {
        try {
            return executor.command_inout(command, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link float}
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joObject = joDeviceProxy->executeCommand( "SomeCommand", LON64ARR(3,1) )
     * </code>
     *
     * @param command a command name
     * @return an Object that represents command execution result or null if output is defined to DevVoid
     * @throws IDLDeviceProxyRuntimeException
     */
    public Object executeCommand(String command, float value) {
        try {
            return executor.command_inout(command, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link int}
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joObject = joDeviceProxy->executeCommand( "SomeCommand", LON64ARR(3,1) )
     * </code>
     *
     * @param command a command name
     * @return an Object that represents command execution result or null if output is defined to DevVoid
     * @throws IDLDeviceProxyRuntimeException
     */
    public Object executeCommand(String command, int value) {
        try {
            return executor.command_inout(command, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link String[]}
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joObject = joDeviceProxy->executeCommand( "SomeCommand", LON64ARR(3,1) )
     * </code>
     *
     * @param command a command name
     * @return an Object that represents command execution result or null if output is defined to DevVoid
     * @throws IDLDeviceProxyRuntimeException
     */
    public Object executeCommand(String command, String[] value) {
        try {
            return executor.command_inout(command, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link float[]}
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joObject = joDeviceProxy->executeCommand( "SomeCommand", FLTARR(3,1) )
     * </code>
     *
     * @param command a command name
     * @return an Object that represents command execution result or null if output is defined to DevVoid
     * @throws IDLDeviceProxyRuntimeException
     */
    public Object executeCommand(String command, float[] value) {
        try {
            return executor.command_inout(command, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link short[]}
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joObject = joDeviceProxy->executeCommand( "SomeCommand", INTARR(3,1) )
     * </code>
     *
     * @param command a command name
     * @return an Object that represents command execution result or null if output is defined to DevVoid
     * @throws IDLDeviceProxyRuntimeException
     */
    public Object executeCommand(String command, short[] value) {
        try {
            return executor.command_inout(command, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link boolean}
     *
     * @param command a command name
     * @return an Object that represents command execution result or null if output is defined to DevVoid
     * @throws IDLDeviceProxyRuntimeException
     */
    public Object executeCommand(String command, boolean value) {
        try {
            return executor.command_inout(command, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link byte[]}
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joObject = joDeviceProxy->executeCommand( "SomeCommand", BYTARR(3,1) )
     * </code>
     *
     * @param command a command name
     * @return an Object that represents command execution result or null if output is defined to DevVoid
     * @throws IDLDeviceProxyRuntimeException
     */
    public Object executeCommand(String command, byte[] value) {
        try {
            return executor.command_inout(command, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link int[]}
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joObject = joDeviceProxy->executeCommand( "SomeCommand", LONARR(3,1) )
     * </code>
     *
     * @param command a command name
     * @return an Object that represents command execution result or null if output is defined to DevVoid
     * @throws IDLDeviceProxyRuntimeException
     */
    public Object executeCommand(String command, int[] value) {
        try {
            return executor.command_inout(command, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    //==========================
    // Read attribute
    //==========================

    /**
     * Reads an attribute from the target Tango server.
     * Does not return attribute value of specific type.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joResult = joDeviceProxy->readAttribute("SomeAttribute")
     * </code>
     *
     * @param attname an attribute name (case sensitive)
     * @return an Object that represents the attribute value
     * @throws IDLDeviceProxyRuntimeException
     */
    public Object readAttribute(String attname) {
        try {
            return reader.readAttribute(attname);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Reads an attribute from the target Tango server.
     * Returns an attribute value of type {@link float}.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * flt = joDeviceProxy->readAttributeFloat("SomeAttribute")
     * </code>
     *
     * @param attname an attribute name (case sensitive)
     * @return float
     * @throws IDLDeviceProxyRuntimeException
     */
    public float readAttributeFloat(String attname) {
        try {
            return reader.readAttributeFloat(attname);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Reads an attribute from the target Tango server.
     * Returns an attribute value of type {@link long}.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * long64 = joDeviceProxy->readAttributeLong("SomeAttribute")
     * </code>
     *
     * @param attname an attribute name (case sensitive)
     * @return long (IDL: long64)
     * @throws IDLDeviceProxyRuntimeException
     */
    public long readAttributeLong(String attname) {
        try {
            return reader.readAttributeLong(attname);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Reads an attribute from the target Tango server.
     * Returns an attribute value of type {@link short}.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * short = joDeviceProxy->readAttributeShort("SomeAttribute")
     * </code>
     *
     * @param attname an attribute name (case sensitive)
     * @return short
     * @throws IDLDeviceProxyRuntimeException
     */
    public short readAttributeShort(String attname) {
        try {
            return reader.readAttributeShort(attname);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Reads an attribute from the target Tango server.
     * Returns an attribute value of type {@link double}.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * dbl = joDeviceProxy->readAttributeDouble("SomeAttribute")
     * </code>
     *
     * @param attname an attribute name (case sensitive)
     * @return double
     * @throws IDLDeviceProxyRuntimeException
     */
    public double readAttributeDouble(String attname) {
        try {
            return reader.readAttributeDouble(attname);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Reads an attribute from the target Tango server.
     * Returns an attribute value of type {@link int}.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * long = joDeviceProxy->readAttributeInteger("SomeAttribute")
     * </code>
     *
     * @param attname an attribute name (case sensitive)
     * @return int (IDL: long)
     * @throws IDLDeviceProxyRuntimeException
     */
    public int readAttributeInteger(String attname) {
        try {
            return reader.readAttributeInteger(attname);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Reads an attribute from the target Tango server.
     * Returns an attribute value of type {@link String}.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * String = joDeviceProxy->readAttributeState()
     * </code>
     *
     * @return String representation of the State, i.e. "ON", "RUNNING" etc
     * @throws IDLDeviceProxyRuntimeException
     */
    public String readAttributeState() {
        try {
            return reader.readAttributeState();
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Reads an attribute from the target Tango server.
     * Returns an attribute value of type {@link String}.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * String = joDeviceProxy->readAttributeString("SomeAttribute")
     * </code>
     *
     * @param attname an attribute name (case sensitive)
     * @return String
     * @throws IDLDeviceProxyRuntimeException
     */
    public String readAttributeString(String attname) {
        try {
            return reader.readAttributeString(attname);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Reads an attribute from the target Tango server.
     * Returns an attribute value of type {@link boolean}.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * integer = joDeviceProxy->readAttributeBoolean("SomeAttribute")
     * </code>
     *
     * @param attname an attribute name (case sensitive)
     * @return boolean (IDL: integer)
     * @throws IDLDeviceProxyRuntimeException
     */
    public boolean readAttributeBoolean(String attname) {
        try {
            return reader.readAttributeBoolean(attname);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    //==========================
    // Write attribute
    //==========================


    /**
     * Writes a {@link boolean} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", 1
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, boolean value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link byte} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", BYTE(1)
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, byte value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link long} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", LONG64(1)
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, long value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link double} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", 0.125D
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, double value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link short} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", 1234
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, short value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link String} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", "Some Value"
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, String value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link float} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", FLOAT(0.125)
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, float value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link int} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", LONG(0.125)
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, int value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link char} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", BYTE(125)
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, char value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link double[]} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", DBLARR(3,1)
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, double[] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link boolean[]} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", INTARR(3,1)
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, boolean[] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link int[]} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", LONARR(3,1)
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, int[] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link short[][]} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", INTARR(2,5)
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, short[][] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link int[][]} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", LONARR(2,5)
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, int[][] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    public void writeAttribute(String attrName, int[] data, int width, int height){
        try {
            TangoImage<int[]> value = new TangoImage<int[]>(data, width, height);
            writer.writeAttribute(attrName, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    public void writeAttribute(String attrName, double[] data, int width, int height){
        try {
            TangoImage<double[]> value = new TangoImage<double[]>(data, width, height);
            writer.writeAttribute(attrName, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    public void writeAttribute(String attrName, float[] data, int width, int height){
        try {
            TangoImage<float[]> value = new TangoImage<float[]>(data, width, height);
            writer.writeAttribute(attrName, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link String[]} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", STRARR(3,1)
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, String[] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link long[]} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", LON64ARR(3,1)
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, long[] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link float[][]} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", FLTARR(2,5)
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, float[][] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link byte[][]} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", BYTARR(2,5)
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, byte[][] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link short[]} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", INTARR(3,1)
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, short[] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link boolean[][]} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", INTARR(2,5)
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, boolean[][] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link byte[]} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", BYTARR(3,1)
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, byte[] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link String[][]} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", STRARR(2,5)
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, String[][] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link long[][]} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", LON64ARR(2,5)
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, long[][] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link double[][]} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", DBLARR(2,5)
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, double[][] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link float[]} value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", FLTARR(3,1)
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, float[] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    public void setSource(int srcId) {
        try {
            proxy.toDeviceProxy().set_source(DevSource.from_int(srcId));
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }
}
