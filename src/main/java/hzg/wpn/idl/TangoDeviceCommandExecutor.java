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
import wpn.hdri.tango.proxy.TangoProxy;
import wpn.hdri.tango.proxy.TangoProxyException;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 05.06.12
 */
public class TangoDeviceCommandExecutor {
    private final TangoProxy proxy;
    private final Logger log;
    private final TangoProxyExceptionHandler handler;

    public TangoDeviceCommandExecutor(TangoProxy proxy, Logger log, TangoProxyExceptionHandler handler) {
        this.proxy = proxy;
        this.log = log;
        this.handler = handler;
    }

    public Object command_inout(String command) {
        try {
            return proxy.executeCommand(command, null);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public Object command_inout(String command, String value) {
        try {
            return proxy.executeCommand(command, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public Object command_inout(String command, String[] value) {
        try {
            return proxy.executeCommand(command, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public Object command_inout(String command, byte[] value) {
        try {
            return proxy.executeCommand(command, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public Object command_inout(String command, short value) {
        try {
            return proxy.executeCommand(command, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public Object command_inout(String command, short[] value) {
        try {
            return proxy.executeCommand(command, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public Object command_inout(String command, int value) {
        try {
            return proxy.executeCommand(command, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public Object command_inout(String command, int[] value) {
        try {
            return proxy.executeCommand(command, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public Object command_inout(String command, long value) {
        try {
            return proxy.executeCommand(command, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public Object command_inout(String command, long[] value) {
        try {
            return proxy.executeCommand(command, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public Object command_inout(String command, float value) {
        try {
            return proxy.executeCommand(command, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public Object command_inout(String command, float[] value) {
        try {
            return proxy.executeCommand(command, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public Object command_inout(String command, double value) {
        try {
            return proxy.executeCommand(command, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public Object command_inout(String command, double[] value) {
        try {
            return proxy.executeCommand(command, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public Object command_inout(String command, boolean value) {
        try {
            return proxy.executeCommand(command, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }
}
