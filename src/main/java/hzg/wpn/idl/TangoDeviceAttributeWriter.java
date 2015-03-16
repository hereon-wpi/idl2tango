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


import org.tango.client.ez.data.type.TangoImage;
import org.tango.client.ez.proxy.TangoProxy;
import org.tango.client.ez.proxy.TangoProxyException;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 05.06.12
 */
public class TangoDeviceAttributeWriter {
    private final TangoProxy proxy;
    private final TangoProxyExceptionHandler handler;

    public TangoDeviceAttributeWriter(TangoProxy proxy, TangoProxyExceptionHandler handler) {
        this.proxy = proxy;
        this.handler = handler;
    }

    public void writeAttribute(String name, String value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public void writeAttribute(String name, char value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public void writeAttribute(String name, boolean value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public void writeAttribute(String name, byte value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public void writeAttribute(String name, short value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public void writeAttribute(String name, int value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    //TODO Tango fails to write long attribute
    public void writeAttribute(String name, long value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public void writeAttribute(String name, float value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public void writeAttribute(String name, double value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public void writeAttribute(String name, String[] value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public void writeAttribute(String name, boolean[] value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public void writeAttribute(String name, byte[] value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public void writeAttribute(String name, short[] value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public void writeAttribute(String name, int[] value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public void writeAttribute(String name, long[] value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public void writeAttribute(String name, float[] value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public void writeAttribute(String name, double[] value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public void writeAttribute(String name, String[][] value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public void writeAttribute(String name, boolean[][] value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public void writeAttribute(String name, byte[][] value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public void writeAttribute(String name, short[][] value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public void writeAttribute(String name, int[][] value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public void writeAttribute(String name, TangoImage<?> image) {
        try {
            proxy.writeAttribute(name, image);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public void writeAttribute(String name, long[][] value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public void writeAttribute(String name, float[][] value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }

    public void writeAttribute(String name, double[][] value) {
        try {
            proxy.writeAttribute(name, value);
        } catch (TangoProxyException devFailed) {
            throw handler.handle(devFailed);
        }
    }
}
