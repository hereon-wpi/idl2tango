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
import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.DevSource;
import fr.esrf.Tango.DevState;
import org.apache.commons.beanutils.ConvertUtils;
import org.slf4j.Logger;
import org.tango.client.ez.data.type.TangoImage;
import org.tango.client.ez.proxy.*;
import org.tango.client.ez.util.TangoUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class provides simplified interface of {@link fr.esrf.TangoApi.DeviceProxy} to be used through IDL Java importing
 * It also adds a couple of new features such as {@link this#waitUntil}
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 05.06.12
 */
public class IDLDeviceProxy {
    private static final Logger logger = LoggerConfiguration.createLogger(IDLDeviceProxy.class);

    private static final TangoProxyExceptionHandler handler = new TangoProxyExceptionHandler(logger);

    final TangoProxy proxy;
    final TangoDevStateAwaitor awaitor;
    final AtomicReference<Exception> lastException = new AtomicReference<Exception>(new Exception("No exceptions so far."));

    private final static Map<String, DevState> devStates = new HashMap<String, DevState>();

    static {
        devStates.put("ON", DevState.ON);
        devStates.put("OFF", DevState.OFF);
        devStates.put("CLOSE", DevState.CLOSE);
        devStates.put("OPEN", DevState.OPEN);
        devStates.put("INSERT", DevState.INSERT);
        devStates.put("EXTRACT", DevState.EXTRACT);
        devStates.put("MOVING", DevState.MOVING);
        devStates.put("STANDBY", DevState.STANDBY);
        devStates.put("FAULT", DevState.FAULT);
        devStates.put("INIT", DevState.INIT);
        devStates.put("RUNNING", DevState.RUNNING);
        devStates.put("ALARM", DevState.ALARM);
        devStates.put("DISABLE", DevState.DISABLE);
        devStates.put("UNKNOWN", DevState.UNKNOWN);
    }

    static {
        // :)
        printVersion();
        System.out.println("Log output file: " + LoggerConfiguration.DEFAULT_LOG_FILE);
    }

    /**
     * Changes current log output file to the specified one
     *
     * @param fileName
     */
    public static void setLogFile(String fileName){
        File file = new File(fileName);

        if(!file.getParentFile().exists()) file.getParentFile().mkdirs();

        System.out.println("Set new log output file=" + file);
        LoggerConfiguration.setLogFile(fileName);
    }

    /**
     * Changes log level for IDL2Tango classes
     *
     * @param level
     */
    public static void setLogLevel(String level){
        LoggerConfiguration.setLogLevel(Level.valueOf(level.toUpperCase()));
    }

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
     * @throws RuntimeException
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
     * @throws RuntimeException
     */
    public IDLDeviceProxy(String name, boolean useEventsForWaitUntil) {
        logger.debug("Creating proxy for device[{},useEventsForWaitUntil={}]", name, useEventsForWaitUntil);
        if (useEventsForWaitUntil)
            throw handler.handle(new UnsupportedOperationException("useEventsForWaitUntil=true is no longer supported use false"));
        try {
            this.proxy = TangoProxies.newDeviceProxyWrapper(name);
            this.awaitor = new PollDevStateAwaitor(this.proxy, handler);
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
        logger.trace("Set {}/timeout={}", proxy.getName(), timeout);
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
     * @throws RuntimeException
     */
    public void waitUntil(String state) {
        logger.debug("Waiting until {}/{}", proxy.getName(), state);
        try {
            DevState targetDevState = devStates.get(state.toUpperCase());
            awaitor.waitUntil(targetDevState);
            logger.debug("Done waiting.");
        } catch (Exception e) {
            if(e.getCause() instanceof ReadAttributeException){
                ReadAttributeException exception = (ReadAttributeException) e.getCause();
                if(exception.reason.contains("CORBA.TIMEOUT")) waitUntil(state);
            }
            lastException.set(e);
            throw handler.handle(e);
        }
    }

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
     * @throws RuntimeException
     */
    public void waitUntil(String state, long timeout, long delay) {
        logger.debug("Waiting until {}/{}", proxy.getName(), state);
        try {
            DevState targetDevState = devStates.get(state.toUpperCase());
            awaitor.waitUntil(targetDevState, timeout, delay);
            logger.debug("Done waiting.");
        } catch (Exception e) {
            if (e.getCause() instanceof ReadAttributeException) {
                ReadAttributeException exception = (ReadAttributeException) e.getCause();
                if (exception.reason.contains("CORBA.TIMEOUT")) waitUntil(state);
            }
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
     * @throws RuntimeException
     */
    public void waitUntilNot(String state) {
        logger.debug("Waiting until not {}/{}", proxy.getName(), state);
        try {
            DevState targetDevState = devStates.get(state.toUpperCase());
            awaitor.waitUntilNot(targetDevState);
            logger.debug("Done waiting.");
        } catch (Exception e) {
            if(e.getCause() instanceof ReadAttributeException){
                ReadAttributeException exception = (ReadAttributeException) e.getCause();
                if(exception.reason.contains("CORBA.TIMEOUT")) waitUntilNot(state);
            }
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    public void waitUntilNot(String state, long timeout, long delay) {
        logger.debug("Waiting until not {}/{}", proxy.getName(), state);
        try {
            DevState targetDevState = devStates.get(state.toUpperCase());
            awaitor.waitUntilNot(targetDevState, timeout, delay);
            logger.debug("Done waiting.");
        } catch (Exception e) {
            if (e.getCause() instanceof ReadAttributeException) {
                ReadAttributeException exception = (ReadAttributeException) e.getCause();
                if (exception.reason.contains("CORBA.TIMEOUT")) waitUntilNot(state);
            }
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
     * @throws RuntimeException
     */
    public Object executeCommand(String command) {
        logger.trace("Executing command {}/{}({})", proxy.getName(), command, "Void");
        try {
            return proxy.executeCommand(command, null);
        } catch (Exception e) {
            if(e instanceof ExecuteCommandException){
                ExecuteCommandException exception = (ExecuteCommandException) e.getCause();
                if(exception.reason.contains("CORBA.TIMEOUT")) return executeCommand(command);
            }
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
     * @throws RuntimeException
     */
    public Object executeCommand(String command, String value) {
        logger.trace("Executing command {}/{}({})", proxy.getName(), command, value);
        try {
            return proxy.executeCommand(command, value);
        } catch (Exception e) {
            if(e instanceof ExecuteCommandException){
                ExecuteCommandException exception = (ExecuteCommandException) e.getCause();
                if(exception.reason.contains("CORBA.TIMEOUT")) return executeCommand(command, value);
            }
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
     * @throws RuntimeException
     */
    public Object executeCommand(String command, double value) {
        logger.trace("Executing command {}/{}({})", proxy.getName(), command, value);
        try {
            TangoCommandInfoWrapper info = proxy.getCommandInfo(command);
            Object converted = ConvertUtils.convert(value, info.getArginType());
            logger.trace("Target value[={}] of type[={}]", String.valueOf(converted), info.getArginType().getSimpleName());
            return proxy.executeCommand(command, converted);
        } catch (Exception e) {
            if(e instanceof ExecuteCommandException){
                ExecuteCommandException exception = (ExecuteCommandException) e.getCause();
                if(exception.reason.contains("CORBA.TIMEOUT")) return executeCommand(command, value);
            }
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
     * @throws RuntimeException
     */
    public Object executeCommand(String command, double[] value) {
        logger.trace("Executing command {}/{}({})", proxy.getName(), command, value);
        try {
            return proxy.executeCommand(command, value);
        } catch (Exception e) {
            if(e instanceof ExecuteCommandException){
                ExecuteCommandException exception = (ExecuteCommandException) e.getCause();
                if(exception.reason.contains("CORBA.TIMEOUT")) return executeCommand(command, value);
            }
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
     * @throws RuntimeException
     */
    public Object executeCommand(String command, long[] value) {
        logger.trace("Executing command {}/{}({})", proxy.getName(), command, value);
        try {
            return proxy.executeCommand(command, value);
        } catch (Exception e) {
            if(e instanceof ExecuteCommandException){
                ExecuteCommandException exception = (ExecuteCommandException) e.getCause();
                if(exception.reason.contains("CORBA.TIMEOUT")) return executeCommand(command, value);
            }
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
     * @throws RuntimeException
     */
    public Object executeCommand(String command, long value) {
        logger.trace("Executing command {}/{}({})", proxy.getName(), command, value);
        try {
            TangoCommandInfoWrapper info = proxy.getCommandInfo(command);
            Object converted = ConvertUtils.convert(value, info.getArginType());
            logger.trace("Target value[={}] of type[={}]", String.valueOf(converted), info.getArginType().getSimpleName());
            return proxy.executeCommand(command, converted);
        } catch (Exception e) {
            if(e instanceof ExecuteCommandException){
                ExecuteCommandException exception = (ExecuteCommandException) e.getCause();
                if(exception.reason.contains("CORBA.TIMEOUT")) return executeCommand(command, value);
            }
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
     * @throws RuntimeException
     */
    public Object executeCommand(String command, short value) {
        logger.trace("Executing command {}/{}({})", proxy.getName(), command, value);
        try {
            TangoCommandInfoWrapper info = proxy.getCommandInfo(command);
            Object converted = ConvertUtils.convert(value, info.getArginType());
            logger.trace("Target value[={}] of type[={}]", String.valueOf(converted), info.getArginType().getSimpleName());
            return proxy.executeCommand(command, converted);
        } catch (Exception e) {
            if(e instanceof ExecuteCommandException){
                ExecuteCommandException exception = (ExecuteCommandException) e.getCause();
                if(exception.reason.contains("CORBA.TIMEOUT")) return executeCommand(command, value);
            }
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
     * @throws RuntimeException
     */
    public Object executeCommand(String command, float value) {
        logger.trace("Executing command {}/{}({})", proxy.getName(), command, value);
        try {
            TangoCommandInfoWrapper info = proxy.getCommandInfo(command);
            Object converted = ConvertUtils.convert(value, info.getArginType());
            logger.trace("Target value[={}] of type[={}]", String.valueOf(converted), info.getArginType().getSimpleName());
            return proxy.executeCommand(command, converted);
        } catch (Exception e) {
            if(e instanceof ExecuteCommandException){
                ExecuteCommandException exception = (ExecuteCommandException) e.getCause();
                if(exception.reason.contains("CORBA.TIMEOUT")) return executeCommand(command, value);
            }
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
     * @throws RuntimeException
     */
    public Object executeCommand(String command, int value) {
        logger.trace("Executing command {}/{}({})", proxy.getName(), command, value);
        try {
            TangoCommandInfoWrapper info = proxy.getCommandInfo(command);
            Object converted = ConvertUtils.convert(value, info.getArginType());
            logger.trace("Target value[={}] of type[={}]", String.valueOf(converted), info.getArginType().getSimpleName());
            return proxy.executeCommand(command, converted);
        } catch (Exception e) {
            if(e instanceof ExecuteCommandException){
                ExecuteCommandException exception = (ExecuteCommandException) e.getCause();
                if(exception.reason.contains("CORBA.TIMEOUT")) return executeCommand(command, value);
            }
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
     * @throws RuntimeException
     */
    public Object executeCommand(String command, String[] value) {
        logger.trace("Executing command {}/{}({})", proxy.getName(), command, value);
        try {
            return proxy.executeCommand(command, value);
        } catch (Exception e) {
            if(e instanceof ExecuteCommandException){
                ExecuteCommandException exception = (ExecuteCommandException) e.getCause();
                if(exception.reason.contains("CORBA.TIMEOUT")) return executeCommand(command, value);
            }
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
     * @throws RuntimeException
     */
    public Object executeCommand(String command, float[] value) {
        logger.trace("Executing command {}/{}({})", proxy.getName(), command, value);
        try {
            return proxy.executeCommand(command, value);
        } catch (Exception e) {
            if(e instanceof ExecuteCommandException){
                ExecuteCommandException exception = (ExecuteCommandException) e.getCause();
                if(exception.reason.contains("CORBA.TIMEOUT")) return executeCommand(command, value);
            }
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
     * @throws RuntimeException
     */
    public Object executeCommand(String command, short[] value) {
        logger.trace("Executing command {}/{}({})", proxy.getName(), command, value);
        try {
            return proxy.executeCommand(command, value);
        } catch (Exception e) {
            if(e instanceof ExecuteCommandException){
                ExecuteCommandException exception = (ExecuteCommandException) e.getCause();
                if(exception.reason.contains("CORBA.TIMEOUT")) return executeCommand(command, value);
            }
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
     * @throws RuntimeException
     */
    public Object executeCommand(String command, byte[] value) {
        logger.trace("Executing command {}/{}({})", proxy.getName(), command, value);
        try {
            return proxy.executeCommand(command, value);
        } catch (Exception e) {
            if(e instanceof ExecuteCommandException){
                ExecuteCommandException exception = (ExecuteCommandException) e.getCause();
                if(exception.reason.contains("CORBA.TIMEOUT")) return executeCommand(command, value);
            }
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
     * @throws RuntimeException
     */
    public Object executeCommand(String command, int[] value) {
        logger.trace("Executing command {}/{}({})", proxy.getName(), command, value);
        try {
            return proxy.executeCommand(command, value);
        } catch (Exception e) {
            if(e instanceof ExecuteCommandException){
                ExecuteCommandException exception = (ExecuteCommandException) e.getCause();
                if(exception.reason.contains("CORBA.TIMEOUT")) return executeCommand(command, value);
            }
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
     * @throws RuntimeException
     */
    public Object readAttribute(String attname) {
        logger.trace("Reading attribute {}/{}", proxy.getName(), attname);
        try {
            return proxy.readAttribute(attname);
        } catch (Exception e) {
            if(e instanceof ReadAttributeException){
                ReadAttributeException readAttributeException = (ReadAttributeException) e;
                if(readAttributeException.reason.contains("CORBA.TIMEOUT")) return readAttribute(attname);
            }
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
     * @throws RuntimeException
     */
    public float readAttributeFloat(String attname) {
        logger.trace("Reading float attribute {}/{}", proxy.getName(), attname);
        try {
            return proxy.readAttribute(attname);
        } catch (Exception e) {
            if(e instanceof ReadAttributeException){
                ReadAttributeException readAttributeException = (ReadAttributeException) e;
                if(readAttributeException.reason.contains("CORBA.TIMEOUT")) return readAttributeFloat(attname);
            }
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
     * @throws RuntimeException
     */
    public long readAttributeLong(String attname) {
        logger.trace("Reading long attribute {}/{}", proxy.getName(), attname);
        try {
            return proxy.readAttribute(attname);
        } catch (Exception e) {
            if(e instanceof ReadAttributeException){
                ReadAttributeException readAttributeException = (ReadAttributeException) e;
                if(readAttributeException.reason.contains("CORBA.TIMEOUT")) return readAttributeLong(attname);
            }

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
     * @throws RuntimeException
     */
    public short readAttributeShort(String attname) {
        logger.trace("Reading short attribute {}/{}", proxy.getName(), attname);
        try {
            return proxy.readAttribute(attname);
        } catch (Exception e) {
            if(e instanceof ReadAttributeException){
                ReadAttributeException readAttributeException = (ReadAttributeException) e;
                if(readAttributeException.reason.contains("CORBA.TIMEOUT")) return readAttributeShort(attname);
            }

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
     * @throws RuntimeException
     */
    public double readAttributeDouble(String attname) {
        logger.trace("Reading double attribute {}/{}", proxy.getName(), attname);
        try {
            return proxy.readAttribute(attname);
        } catch (Exception e) {
            if(e instanceof ReadAttributeException){
                ReadAttributeException readAttributeException = (ReadAttributeException) e;
                if(readAttributeException.reason.contains("CORBA.TIMEOUT")) return readAttributeDouble(attname);
            }

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
     * @throws RuntimeException
     */
    public int readAttributeInteger(String attname) {
        logger.trace("Reading int attribute {}/{}", proxy.getName(), attname);
        try {
            return (Integer) proxy.readAttribute(attname);
        } catch (Exception e) {
            if(e instanceof ReadAttributeException){
                ReadAttributeException readAttributeException = (ReadAttributeException) e;
                if(readAttributeException.reason.contains("CORBA.TIMEOUT")) return readAttributeInteger(attname);
            }

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
     * @throws RuntimeException
     */
    public String readAttributeState() {
        logger.trace("Reading {}/State", proxy.getName());
        try {
            return ((DevState) proxy.readAttribute("State")).toString();
        } catch (Exception e) {
            if(e instanceof ReadAttributeException){
                ReadAttributeException readAttributeException = (ReadAttributeException) e;
                if(readAttributeException.reason.contains("CORBA.TIMEOUT")) return readAttributeState();
            }

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
     * @throws RuntimeException
     */
    public String readAttributeString(String attname) {
        logger.trace("Reading String attribute {}/{}", proxy.getName(), attname);
        try {
            return (String) proxy.readAttribute(attname);
        } catch (Exception e) {
            if(e instanceof ReadAttributeException){
                ReadAttributeException readAttributeException = (ReadAttributeException) e;
                if(readAttributeException.reason.contains("CORBA.TIMEOUT")) return readAttributeString(attname);
            }

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
     * @throws RuntimeException
     */
    public boolean readAttributeBoolean(String attname) {
        logger.trace("Reading boolean attribute {}/{}", proxy.getName(), attname);
        try {
            return (Boolean) proxy.readAttribute(attname);
        } catch (Exception e) {
            if(e instanceof ReadAttributeException){
                ReadAttributeException readAttributeException = (ReadAttributeException) e;
                if(readAttributeException.reason.contains("CORBA.TIMEOUT")) return readAttributeBoolean(attname);
            }

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
     * joDeviceProxy->writeAttributeBoolean, "boolean_scalar", 1
     * </code>
     * Any positive value is considered to be <b>true</b>, while 0 or negative value -- <b>false</b>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws RuntimeException
     */
    public void writeAttributeBoolean(String name, short value) {
        logger.trace("Writing attribute {}/{}={}[passed {}] (boolean)", proxy.getName(), name, value > 0,  value);
        try {
            proxy.writeAttribute(name, value > 0);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttributeBoolean(name, value);
            }

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
     * joDeviceProxy->writeAttributeShort, "short_scalar_w", 123
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws RuntimeException
     */
    public void writeAttributeShort(String name, short value) {
        logger.trace("Writing attribute {}/{}={} (short)", proxy.getName(), name, value);
        try {
            proxy.writeAttribute(name, value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttributeBoolean(name, value);
            }

            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a <b>ushort</b> value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttributeUShort, "ushort_scalar", 123
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws RuntimeException
     */
    public void writeAttributeUShort(String name, short value) {
        logger.trace("Writing attribute {}/{}={} (ushort)", proxy.getName(), name, value);
        try {
            proxy.writeAttribute(name, (int)value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttributeBoolean(name, value);
            }

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
     * joDeviceProxy->writeAttributeLong, "long_scalar_w", 123456L
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws RuntimeException
     */
    public void writeAttributeLong(String name, int value) {
        logger.trace("Writing attribute {}/{}={} (long)", proxy.getName(), name, value);
        try {
            proxy.writeAttribute(name, value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttributeLong(name, value);
            }

            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a <b>uint</b> value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttributeULong, "ulong_scalar", 123456L
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws RuntimeException
     */
    public void writeAttributeULong(String name, int value) {
        logger.trace("Writing attribute {}/{}={} (ulong)", proxy.getName(), name, value);
        try {
            proxy.writeAttribute(name, (long)value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttributeULong(name, value);
            }

            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a <b>long64</b> value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttributeLong64, "boolean_scalar", LONG64(12345678)
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws RuntimeException
     */
    public void writeAttributeLong64(String name, long value) {
        logger.trace("Writing attribute {}/{}={} (long64)", proxy.getName(), name, value);
        try {
            proxy.writeAttribute(name, value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttributeLong64(name, value);
            }

            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a <b>ulong64</b> value to the attribute of the target Tango server.
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttributeULong64, "ulong64_scalar", LONG64(12345678)
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws RuntimeException
     */
    public void writeAttributeULong64(String name, long value) {
        logger.trace("Writing attribute {}/{}={} (ulong64)", proxy.getName(), name, value);
        try {
            proxy.writeAttribute(name, value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttributeULong64(name, value);
            }

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
     * @throws RuntimeException
     */
    public void writeAttribute(String name, byte value) {
        logger.trace("Writing attribute {}/{}={} (byte)", proxy.getName(), name, value);
        try {
            TangoAttributeInfoWrapper info = proxy.getAttributeInfo(name);
            Object converted = ConvertUtils.convert(value, info.getClazz());
            logger.trace("Target value[={}] of type[={}]", String.valueOf(converted), info.getClazz().getSimpleName());
            proxy.writeAttribute(name, converted);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }

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
     * @throws RuntimeException
     */
    public void writeAttribute(String name, long value) {
        logger.trace("Writing attribute {}/{}={} (long)", proxy.getName(), name, value);
        try {
            TangoAttributeInfoWrapper info = proxy.getAttributeInfo(name);
            Object converted = ConvertUtils.convert(value, info.getClazz());
            logger.trace("Target value[={}] of type[={}]", String.valueOf(converted), info.getClazz().getSimpleName());
            proxy.writeAttribute(name, converted);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }
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
     * @throws RuntimeException
     */
    public void writeAttribute(String name, double value) {
        logger.trace("Writing attribute {}/{}={} (double)", proxy.getName(), name, value);
        try {
            TangoAttributeInfoWrapper info = proxy.getAttributeInfo(name);
            Object converted = ConvertUtils.convert(value, info.getClazz());
            logger.trace("Target value[={}] of type[={}]", String.valueOf(converted), info.getClazz().getSimpleName());
            proxy.writeAttribute(name, converted);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }
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
     * @throws RuntimeException
     */
    public void writeAttribute(String name, short value) {
        logger.trace("Writing attribute {}/{}={} (short)", proxy.getName(), name, value);
        try {
            TangoAttributeInfoWrapper info = proxy.getAttributeInfo(name);
            Object converted = ConvertUtils.convert(value, info.getClazz());
            logger.trace("Target value[={}] of type[={}]", String.valueOf(converted), info.getClazz().getSimpleName());
            proxy.writeAttribute(name, converted);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                if(e instanceof ReadAttributeException){
                ReadAttributeException readAttributeException = (ReadAttributeException) e;
                if(readAttributeException.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }
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
     * @throws RuntimeException
     */
    public void writeAttribute(String name, String value) {
        logger.trace("Writing attribute {}/{}={} (String)", proxy.getName(), name, value);
        try {
            proxy.writeAttribute(name, value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }
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
     * joDeviceProxy->writeAttribute, "SomeAttribute", 0.125
     * </code>
     *
     * @param name  an attribute name
     * @param value a value
     * @throws RuntimeException
     */
    public void writeAttribute(String name, float value) {
        logger.trace("Writing attribute {}/{}={} (float)", proxy.getName(), name, value);
        try {
            TangoAttributeInfoWrapper info = proxy.getAttributeInfo(name);
            Object converted = ConvertUtils.convert(value, info.getClazz());
            logger.trace("Target value[={}] of type[={}]", String.valueOf(converted), info.getClazz().getSimpleName());
            proxy.writeAttribute(name, converted);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }
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
     * @throws RuntimeException
     */
    public void writeAttribute(String name, int value) {
        logger.trace("Writing attribute {}/{}={} (int)", proxy.getName(), name, value);
        try {
            TangoAttributeInfoWrapper info = proxy.getAttributeInfo(name);
            Object converted = ConvertUtils.convert(value, info.getClazz());
            logger.trace("Target value[={}] of type[={}]", String.valueOf(converted), info.getClazz().getSimpleName());
            proxy.writeAttribute(name, converted);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }
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
     * @throws RuntimeException
     */
    public void writeAttribute(String name, double[] value) {
        logger.trace("Writing attribute {}/{}={} (double[])", proxy.getName(), name, value);
        try {
            proxy.writeAttribute(name, value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }
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
     * @throws RuntimeException
     */
    public void writeAttribute(String name, boolean[] value) {
        logger.trace("Writing attribute {}/{}={} (boolean[])", proxy.getName(), name, value);
        try {
            proxy.writeAttribute(name, value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**        if(e instanceof ReadAttributeException){
                ReadAttributeException readAttributeException = (ReadAttributeException) e;
                if(readAttributeException.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }
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
     * @throws RuntimeException
     */
    public void writeAttribute(String name, int[] value) {
        logger.trace("Writing attribute {}/{}={} (int[])", proxy.getName(), name, value);
        try {
            proxy.writeAttribute(name, value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }
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
     * @throws RuntimeException
     */
    public void writeAttribute(String name, short[][] value) {
        logger.trace("Writing attribute {}/{}={} (short[][])", proxy.getName(), name, value);
        try {
            proxy.writeAttribute(name, value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }
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
     * @throws RuntimeException
     */
    public void writeAttribute(String name, int[][] value) {
        logger.trace("Writing attribute {}/{}={} (int[][])", proxy.getName(), name, value);
        try {
            proxy.writeAttribute(name, value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes an int image of the specified dimensions
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", LONGARR(10), 2L, 5L
     * </code>
     *
     * @param attrName
     * @param data
     * @param width
     * @param height
     * @throws java.lang.RuntimeException
     */
    public void writeAttribute(String attrName, int[] data, int width, int height){
        logger.trace("Writing an image {}/{}[{}x{}] (int)", proxy.getName(), attrName, width, height);
        try {
            TangoImage<int[]> value = new TangoImage<int[]>(data, width, height);
            proxy.writeAttribute(attrName, value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(attrName, data, width, height);
            }
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a double image of the specified dimensions
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", DBLARR(10), 2L, 5L
     * </code>
     *
     * @param attrName
     * @param data
     * @param width
     * @param height
     * @throws java.lang.RuntimeException
     */
    public void writeAttribute(String attrName, double[] data, int width, int height){
        logger.trace("Writing an image {}/{}[{}x{}] (double)", proxy.getName(), attrName, width, height);
        try {
            TangoImage<double[]> value = new TangoImage<double[]>(data, width, height);
            proxy.writeAttribute(attrName, value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(attrName, data, width, height);
            }
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a float image of the specified dimensions
     * <p/>
     * Usage:
     * <p/>
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", FLOATARR(10), 2L, 5L
     * </code>
     *
     * @param attrName
     * @param data
     * @param width
     * @param height
     * @throws java.lang.RuntimeException
     */
    public void writeAttribute(String attrName, float[] data, int width, int height){
        logger.trace("Writing an image {}/{}[{}x{}] (float)", proxy.getName(), attrName, width, height);
        try {
            TangoImage<float[]> value = new TangoImage<float[]>(data, width, height);
            proxy.writeAttribute(attrName, value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(attrName, data, width, height);
            }
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
     * @throws RuntimeException
     */
    public void writeAttribute(String name, String[] value) {
        logger.trace("Writing attribute {}/{}={} (String[])", proxy.getName(), name, value);
        try {
            proxy.writeAttribute(name, value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }
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
     * @throws RuntimeException
     */
    public void writeAttribute(String name, long[] value) {
        logger.trace("Writing attribute {}/{}={} (long[])", proxy.getName(), name, value);
        try {
            proxy.writeAttribute(name, value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }

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
     * @throws RuntimeException
     */
    public void writeAttribute(String name, float[][] value) {
        logger.trace("Writing attribute {}/{}={} (float[][])", proxy.getName(), name, value);
        try {
            proxy.writeAttribute(name, value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }

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
     * @throws RuntimeException
     */
    public void writeAttribute(String name, byte[][] value) {
        logger.trace("Writing attribute {}/{}={} (byte[][])", proxy.getName(), name, value);
        try {
            proxy.writeAttribute(name, value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }

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
     * @throws RuntimeException
     */
    public void writeAttribute(String name, short[] value) {
        logger.trace("Writing attribute {}/{}={} (short[])", proxy.getName(), name, value);
        try {
            proxy.writeAttribute(name, value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }

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
     * @throws RuntimeException
     */
    public void writeAttribute(String name, boolean[][] value) {
        logger.trace("Writing attribute {}/{}={} (boolean[][])", proxy.getName(), name, value);
        try {
            proxy.writeAttribute(name, value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }

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
     * @throws RuntimeException
     */
    public void writeAttribute(String name, byte[] value) {
        logger.trace("Writing attribute {}/{}={} (byte[])", proxy.getName(), name, value);
        try {
            proxy.writeAttribute(name, value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }

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
     * @throws RuntimeException
     */
    public void writeAttribute(String name, String[][] value) {
        logger.trace("Writing attribute {}/{}={} (String[][])", proxy.getName(), name, value);
        try {
            proxy.writeAttribute(name, value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }
              if(e instanceof ReadAttributeException){
                ReadAttributeException readAttributeException = (ReadAttributeException) e;
                if(readAttributeException.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }
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
     * @throws RuntimeException
     */
    public void writeAttribute(String name, long[][] value) {
        logger.trace("Writing attribute {}/{}={} (long[][])", proxy.getName(), name, value);
        try {
            proxy.writeAttribute(name, value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }

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
     * @throws RuntimeException
     */
    public void writeAttribute(String name, double[][] value) {
        logger.trace("Writing attribute {}/{}={} (double[][])", proxy.getName(), name, value);
        try {
            proxy.writeAttribute(name, value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }

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
     * @throws RuntimeException
     */
    public void writeAttribute(String name, float[] value) {
        logger.trace("Writing attribute {}/{}={} (float[])", proxy.getName(), name, value);
        try {
            proxy.writeAttribute(name, value);
        } catch (Exception e) {
            if(e instanceof WriteAttributeException){
                WriteAttributeException exception = (WriteAttributeException) e;
                if(exception.reason.contains("CORBA.TIMEOUT")) writeAttribute(name, value);
            }

            lastException.set(e);
            throw handler.handle(e);
        }
    }

    public void setSource(int srcId) {
        logger.trace("Set {}/source={} aka {}", proxy.getName(), srcId, DevSource.from_int(srcId).toString());
        try {
            proxy.toDeviceProxy().set_source(DevSource.from_int(srcId));
        } catch (Exception e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * <pre>
     *     Code-Version: XXX;
     *     Loaded from LOCATION
     * </pre>
     *
     * @return current version
     */
    public static String getVersion() {
        return String.format("Code-Version: %s;\nLoaded from %s",
                IDLDeviceProxy.class.getPackage().getImplementationVersion(),
                IDLDeviceProxy.class.getProtectionDomain().getCodeSource().getLocation().toString());
    }

    /**
     * Prints current version
     */
    public static void printVersion(){
        System.out.println();
        System.out.println("\n" +
                "\n" +
                "'####:'########::'##::::::::'#######::'########::::'###::::'##::: ##::'######::::'#######::\n" +
                ". ##:: ##.... ##: ##:::::::'##.... ##:... ##..::::'## ##::: ###:: ##:'##... ##::'##.... ##:\n" +
                ": ##:: ##:::: ##: ##:::::::..::::: ##:::: ##:::::'##:. ##:: ####: ##: ##:::..::: ##:::: ##:\n" +
                ": ##:: ##:::: ##: ##::::::::'#######::::: ##::::'##:::. ##: ## ## ##: ##::'####: ##:::: ##:\n" +
                ": ##:: ##:::: ##: ##:::::::'##::::::::::: ##:::: #########: ##. ####: ##::: ##:: ##:::: ##:\n" +
                ": ##:: ##:::: ##: ##::::::: ##::::::::::: ##:::: ##.... ##: ##:. ###: ##::: ##:: ##:::: ##:\n" +
                "'####: ########:: ########: #########:::: ##:::: ##:::: ##: ##::. ##:. ######:::. #######::\n" +
                "....::........:::........::.........:::::..:::::..:::::..::..::::..:::......:::::.......:::\n" +
                "\n");
        System.out.println(getVersion());
    }
}
