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

import org.apache.log4j.Logger;
import wpn.hdri.tango.data.EnumDevState;
import wpn.hdri.tango.proxy.TangoProxyException;
import wpn.hdri.tango.proxy.TangoProxyWrapper;

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
    private static final Logger log = new Logger(IDLDeviceProxy.class.getSimpleName()){
        @Override
        public void info(Object message) {
            System.out.println(message);
        }

        @Override
        public void error(Object message) {
            System.err.println(message);
        }

        @Override
        public void error(Object message, Throwable t) {
            System.err.println(message);
            t.printStackTrace();
        }
    };
    private static final TangoProxyExceptionHandler handler = new TangoProxyExceptionHandler(log);

    private final TangoProxyWrapper proxy;
    private final TangoDeviceAttributeReader reader;
    private final TangoDeviceAttributeWriter writer;
    private final TangoDeviceCommandExecutor executor;
    private final TangoDevStateAwaitor awaitor;

    private final AtomicReference<Throwable> lastException = new AtomicReference<Throwable>(new Exception("No exceptions so far."));

    /**
     * Creates a new instance of the IDLDeviceProxy.
     *
     * Usage:
     *
     * <code>
     *     joDeviceProxy = OBJ_NEW("IDLJavaObject$hzg_wpn_idl_IDLDeviceProxy", "hzg.wpn.idl.IDLDeviceProxy", "sys/tg_test/1")
     * </code>
     *
     * @param name a Tango device full name, e.g. tango://{TANGO_HOST}/{SERVER_NAME}/{DOMAIN}/{DEVICE_ID}
     * @throws IDLDeviceProxyRuntimeException
     */
    public IDLDeviceProxy(String name){
        try {
            this.proxy = new TangoProxyWrapper(name);
            this.reader = new TangoDeviceAttributeReader(this.proxy,log, handler);
            this.writer = new TangoDeviceAttributeWriter(this.proxy, log, handler);
            this.executor = new TangoDeviceCommandExecutor(this.proxy, log, handler);
            this.awaitor = new PollDevStateAwaitor(this.proxy, log, handler);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public String getExceptionMessage(){
        return lastException.get().getMessage();
    }

    /**
     *
     * @param timeout in milliseconds
     */
    public void setTimeout(int timeout){
        try {
            Field field = proxy.getClass().getDeclaredField("proxy");
            field.setAccessible(true);
            Object wrapped = field.get(proxy);

            wrapped.getClass().getMethod("set_timeout_millis",int.class).invoke(wrapped,timeout);
            field.setAccessible(false);
        } catch (Throwable ex) {
            lastException.set(ex);
            throw handler.handle(ex);
        }
    }

    // Temporarily placed here
    public void simulateExperiment(String receiver, int nIterations, long timeout){
        try{
            TangoProxyWrapper ss = new TangoProxyWrapper("tango://hzgharwi3:10000/development/ss/0");

            //ss.executeCommand("startCollectData",null);
            Thread.sleep(1000);
            long time = System.currentTimeMillis();
            for(int i = 0; i<nIterations; i++, time = System.currentTimeMillis()){

                ss.executeCommand("sendDataTo",new String[]{receiver,Long.toString(time)});

                Thread.sleep(timeout);
            }

            //ss.executeCommand("stopCollectData",null);
        } catch(Throwable ex){
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
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->waitUntil, "running"
     * </code>
     *
     * @param state desired state in String format, i.e. "ON","RUNNING" etc (case insensitive)
     * @throws IDLDeviceProxyRuntimeException
     */
    public void waitUntil(String state){
        try {
            EnumDevState targetDevState = EnumDevState.valueOf(state.toUpperCase());
            awaitor.waitUntil(targetDevState);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Blocks current Thread (execution) until target Tango server reports state different from the specified.
     * State is being checked every 100 milliseconds.
     *
     * Usage:
     *
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
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    //==========================
    // Execute commands
    //==========================

    /**
     * Executes command without an input argument.
     *
     * Usage:
     *
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
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link String}
     *
     * Usage:
     *
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
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link double}
     *
     * Usage:
     *
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
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link double[]}
     *
     * Usage:
     *
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
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link long[]}
     *
     * Usage:
     *
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
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link long}
     *
     * Usage:
     *
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
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link short}
     *
     * Usage:
     *
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
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link float}
     *
     * Usage:
     *
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
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link int}
     *
     * Usage:
     *
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
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link String[]}
     *
     * Usage:
     *
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
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link float[]}
     *
     * Usage:
     *
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
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link short[]}
     *
     * Usage:
     *
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
        } catch (Throwable e) {
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
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link byte[]}
     *
     * Usage:
     *
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
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Executes command with an input argument of type {@link int[]}
     *
     * Usage:
     *
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
        } catch (Throwable e) {
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
     *
     * Usage:
     *
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
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Reads an attribute from the target Tango server.
     * Returns an attribute value of type {@link float}.
     *
     * Usage:
     *
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
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Reads an attribute from the target Tango server.
     * Returns an attribute value of type {@link long}.
     *
     * Usage:
     *
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
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Reads an attribute from the target Tango server.
     * Returns an attribute value of type {@link short}.
     *
     * Usage:
     *
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
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Reads an attribute from the target Tango server.
     * Returns an attribute value of type {@link double}.
     *
     * Usage:
     *
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
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Reads an attribute from the target Tango server.
     * Returns an attribute value of type {@link int}.
     *
     * Usage:
     *
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
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Reads an attribute from the target Tango server.
     * Returns an attribute value of type {@link String}.
     *
     * Usage:
     *
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
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Reads an attribute from the target Tango server.
     * Returns an attribute value of type {@link String}.
     *
     * Usage:
     *
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
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Reads an attribute from the target Tango server.
     * Returns an attribute value of type {@link boolean}.
     *
     * Usage:
     *
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
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    //==========================
    // Write attribute
    //==========================


    /**
     * Writes a {@link boolean} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", 1
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, boolean value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link byte} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", BYTE(1)
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, byte value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link long} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", LONG64(1)
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, long value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link double} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", 0.125D
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, double value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link short} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", 1234
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, short value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link String} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", "Some Value"
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, String value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link float} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", FLOAT(0.125)
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, float value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link int} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", LONG(0.125)
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, int value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link char} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", BYTE(125)
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, char value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link double[]} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", DBLARR(3,1)
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, double[] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link boolean[]} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", INTARR(3,1)
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, boolean[] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link int[]} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", LONARR(3,1)
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, int[] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link short[][]} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", INTARR(2,5)
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, short[][] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link int[][]} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", LONARR(2,5)
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, int[][] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link String[]} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", STRARR(3,1)
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, String[] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link long[]} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", LON64ARR(3,1)
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, long[] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link float[][]} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", FLTARR(2,5)
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, float[][] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link byte[][]} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", BYTARR(2,5)
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, byte[][] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link short[]} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", INTARR(3,1)
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, short[] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link boolean[][]} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", INTARR(2,5)
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, boolean[][] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link byte[]} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", BYTARR(3,1)
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, byte[] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link String[][]} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", STRARR(2,5)
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, String[][] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link long[][]} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", LON64ARR(2,5)
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, long[][] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link double[][]} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", DBLARR(2,5)
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, double[][] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }

    /**
     * Writes a {@link float[]} value to the attribute of the target Tango server.
     *
     * Usage:
     *
     * <code>
     * joDeviceProxy->writeAttribute, "SomeAttribute", FLTARR(3,1)
     * </code>
     *
     * @param name an attribute name
     * @param value a value
     * @throws IDLDeviceProxyRuntimeException
     */
    public void writeAttribute(String name, float[] value) {
        try {
            writer.writeAttribute(name, value);
        } catch (Throwable e) {
            lastException.set(e);
            throw handler.handle(e);
        }
    }
}
