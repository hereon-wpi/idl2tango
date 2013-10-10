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
import wpn.hdri.tango.proxy.TangoProxy;
import wpn.hdri.tango.proxy.TangoProxyException;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 05.06.12
 */
public class TangoDeviceAttributeReader {
    private final TangoProxy proxy;
    private final Logger log;
    private final TangoProxyExceptionHandler handler;

    public TangoDeviceAttributeReader(TangoProxy proxy, Logger log, TangoProxyExceptionHandler handler) {
        this.proxy = proxy;
        this.log = log;
        this.handler = handler;
    }

    public Object readAttribute(String attname) {
        try {
            return proxy.readAttribute(attname);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public boolean readAttributeBoolean(String attname) {
        try {
            return (Boolean) proxy.readAttribute(attname);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public String readAttributeString(String attname) {
        try {
            return (String) proxy.readAttribute(attname);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public short readAttributeShort(String attname) {
        try {
            return (Short) proxy.readAttribute(attname);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public int readAttributeInteger(String attname) {
        try {
            return (Integer) proxy.readAttribute(attname);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public long readAttributeLong(String attname) {
        try {
            return (Long) proxy.readAttribute(attname);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public float readAttributeFloat(String attname) {
        try {
            return (Float) proxy.readAttribute(attname);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public double readAttributeDouble(String attname) {
        try {
            return (Double) proxy.readAttribute(attname);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public String readAttributeState() {
        try {
            return ((EnumDevState) proxy.readAttribute("State")).name();
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }
}
