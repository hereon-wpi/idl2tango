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


import org.tango.client.ez.data.EnumDevState;
import org.tango.client.ez.proxy.TangoProxy;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 06.06.12
 */
public abstract class TangoDevStateAwaitor {
    public static final String STATE = "State";
    private final TangoProxy proxy;
    private final TangoProxyExceptionHandler handler;
    private final AtomicReference<EnumDevState> crtDevState = new AtomicReference<EnumDevState>();

    protected TangoDevStateAwaitor(TangoProxy proxy, TangoProxyExceptionHandler handler) {
        this.proxy = proxy;
        this.handler = handler;
    }

    /**
     * Blocks current thread until device state changes to targetState
     *
     * @param targetState wait until the state
     * @throws IDLDeviceProxyRuntimeException
     */
    public abstract void waitUntil(EnumDevState targetState);

    /**
     * Blocks current thread until the device is in targetState
     *
     * @param targetState current device state
     * @throws IDLDeviceProxyRuntimeException
     */
    public abstract void waitUntilNot(EnumDevState targetState);

    protected boolean targetStateReached(EnumDevState targetState) {
        return crtDevState.get() == targetState;
    }

    protected void setCrtDevState(EnumDevState state) {
        this.crtDevState.set(state);
    }

    protected TangoProxy getProxy() {
        return proxy;
    }

    protected TangoProxyExceptionHandler getHandler() {
        return handler;
    }
}
