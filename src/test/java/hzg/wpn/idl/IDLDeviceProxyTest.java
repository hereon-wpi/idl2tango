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

import fr.esrf.Tango.DevState;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.tango.client.ez.data.type.ImageTangoDataTypes;
import org.tango.client.ez.data.type.TangoImage;
import org.tango.client.ez.util.TangoImageUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertArrayEquals;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 05.06.12
 */
@Ignore
public class IDLDeviceProxyTest {
    public static final String TANGO_DEV_NAME = "test/local/0";
    public static final int TANGO_PORT = 16547;
    public static final String TEST_TANGO = "tango://localhost:" + TANGO_PORT + "/" + TANGO_DEV_NAME + "#dbase=no";
    public static final String X64 = "x64";
    public static final String LINUX = "linux";
    public static final String WINDOWS_7 = "windows 7";
    public static final String AMD64 = "amd64";

    private static Process PRC;

    @BeforeClass
    public static void beforeClass() throws Exception {
        String crtDir = System.getProperty("user.dir");
        //TODO define executable according to current OS
        String os = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");
        StringBuilder bld = new StringBuilder(crtDir);
        //TODO other platforms or rely on the environmet
        if (LINUX.equalsIgnoreCase(os) && AMD64.equals(arch))
            bld.append("/exec/tango/debian/").append("TangoTest");
        else if (WINDOWS_7.equalsIgnoreCase(os) && AMD64.equals(arch))
            bld.append("\\exec\\tango\\win64\\").append("TangoTest");
        else
            throw new RuntimeException(String.format("Unsupported platform: name=%s arch=%s", os, arch));

        PRC = new ProcessBuilder(bld.toString(), "test", "-ORBendPoint", "giop:tcp::" + TANGO_PORT, "-nodb", "-dlist", TANGO_DEV_NAME)
                .start();

        //drain slave's out stream
        new Thread(new Runnable() {
            @Override
            public void run() {
                char bite;
                try {
                    while ((bite = (char) PRC.getInputStream().read()) > -1) {
                        System.out.print(bite);
                    }
                } catch (IOException ignore) {
                }
            }
        }).start();

        //drains slave's err stream
        new Thread(new Runnable() {
            @Override
            public void run() {
                char bite;
                try {
                    while ((bite = (char) PRC.getErrorStream().read()) > -1) {
                        System.err.print(bite);
                    }
                } catch (IOException ignore) {
                }
            }
        }).start();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        PRC.destroy();
    }


    @Test(expected = RuntimeException.class)
    public void testReadAttribute_Failed() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy(TEST_TANGO);

        String result = (String) instance.readAttribute("string_scalarxxx");//no such attribute

        assertEquals("Some test value", result);
    }

    @Test
    public void testWriteReadAttribute_String() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy(TEST_TANGO);

        instance.writeAttribute("string_scalar", "Some test value");
        String result = (String) instance.readAttribute("string_scalar");

        assertEquals("Some test value", result);
    }

    @Test
    public void testWriteReadAttribute_DoubleArr() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy(TEST_TANGO);

        instance.writeAttribute("double_spectrum", new double[]{1.0, 1.1, 1.2, 1.3, 1.4});
        double[] result = (double[]) instance.readAttribute("double_spectrum");

        assertArrayEquals(new double[]{1.0, 1.1, 1.2, 1.3, 1.4}, result, 0.0);
    }

    @Test
    public void testWriteReadAttribute_ULong() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy(TEST_TANGO);

        instance.writeAttribute("ulong_scalar", 1234L);
        long result = (Long) instance.readAttribute("ulong_scalar");

        //TangoTest returns random number
        //assertEquals(1234, result);
    }

    @Test
    public void testWriteReadAttribute_DoubleSpectrum() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy(TEST_TANGO);

        int size = 256; //sys/tg_test/1 has hardcoded maximum value of 256
        double[] sinus = new double[size];
        double coeff = Math.PI / 180;
        for (int i = 0; i < size; i++) {
            sinus[i] = Math.sin(i * coeff);
        }

        instance.writeAttribute("double_spectrum", sinus);

        double[] result = (double[]) instance.readAttribute("double_spectrum");

        assertArrayEquals(sinus, result, 0.0);
    }

    @Test
    public void testWriteReadAttribute_Int() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy(TEST_TANGO);

        instance.writeAttribute("long_scalar_w", 1234);
        int result = instance.readAttributeInteger("long_scalar_w");

        assertEquals(1234, result);
    }

    @Test
    public void testWriteReadAttribute_Double() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy(TEST_TANGO);

        instance.writeAttribute("double_scalar_w", 3.14D);

        double result = instance.readAttributeDouble("double_scalar_w");

        assertEquals(3.14D, result);
    }

    @Test
    public void testWriteReadAttribute_FloatImage() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy(TEST_TANGO);

        instance.writeAttribute("float_image", new float[]{0.1F, 0.2F, 0.3F, 0.4F, 0.5F, 0.6F, 0.5F, 0.6F}, 2, 4);

        TangoImage<float[]> result = (TangoImage<float[]>) instance.readAttribute("float_image");

        assertArrayEquals(new float[]{0.1F, 0.2F, 0.3F, 0.4F, 0.5F, 0.6F, 0.5F, 0.6F}, Arrays.copyOfRange(result.getData(), 0, 2 * 4), 0.01F);
    }

    @Test
    public void testWriteReadAttribute_DoubleImage() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy(TEST_TANGO);


        TangoImage<double[]> image = new TangoImage<double[]>(new double[]{0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.5, 0.6},2,4);

        instance.writeAttribute("double_image", image.getData(), image.getWidth(), image.getHeight());

        TangoImage<double[]> result = (TangoImage<double[]>) instance.readAttribute("double_image");

        assertArrayEquals(new double[]{0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.5, 0.6}, Arrays.copyOfRange(result.getData(), 0, 2 * 4), 0.0);
    }

    @Test
    public void testWriteReadAttribute_Image() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy(TEST_TANGO);


        BufferedImage img = ImageIO.read(IDLDeviceProxy.class.getResourceAsStream("/images/check.png"));
        Raster imgRaster = img.getData();
        int imgWidth = imgRaster.getWidth(), imgHeight = imgRaster.getHeight();
        int[][] pixels = new int[img.getHeight()][img.getWidth()];
        for (int i = 0,
                     x = imgRaster.getMinX(), y = imgRaster.getMinY(),
                     imgSize = imgWidth * imgHeight;
             i < imgSize;
             i++, x = x < (imgWidth - 1) ? x + 1 : 0, y += x == 0 ? 1 : 0) {
            int[] RGBa = imgRaster.getPixel(x, y, new int[4]);
            int red = RGBa[0],
                    green = RGBa[1],
                    blue = RGBa[2],
                    alpha = RGBa[3];
            int rgb = 0;
            rgb = alpha;
            rgb = (rgb << 8) + red;
            rgb = (rgb << 8) + green;
            rgb = (rgb << 8) + blue;
            pixels[y][x] = rgb;
        }

        TangoImage<int[]> image = TangoImage.from2DArray(pixels);

        instance.writeAttribute("ushort_image", image.getData(), imgWidth, imgHeight);

        TangoImage<int[]> result = (TangoImage<int[]>) instance.readAttribute("ushort_image");

        RenderedImage imgResult = TangoImageUtils.toRenderedImage_sRGB(result.getData(), result.getWidth(), result.getHeight());
        ImageIO.write(imgResult, "png", new File("target/result_image.png"));
    }

    //@Test
    public void testReadAttribute_BigImage() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy("tango://hasgkssw2ctcam1b.desy.de:10000/w2/cameratest/ccdfli");
        TimeUnit nanos = TimeUnit.NANOSECONDS;

//        instance.writeAttribute("double_image",pixels);

        System.setProperty(ImageTangoDataTypes.TANGO_IMAGE_EXTRACTER_USES_MULTITHREADING, "true");
        for (int i = 0; i < 10; i++) {
            long startReadImage = System.nanoTime();
            int[][] result = (int[][]) instance.readAttribute("Image");
            long endReadImage = System.nanoTime();

            System.out.println("Read image time (millis):" + nanos.toMillis(endReadImage - startReadImage));
        }
//
//        assertArrayEquals(new double[]{0.1,0.2}, result[0],0.0);
//        assertArrayEquals(new double[]{0.3,0.4}, result[1],0.0);
//        assertArrayEquals(new double[]{0.5,0.6}, result[2],0.0);
    }

    @Test
    public void testExecuteCommand() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy(TEST_TANGO);

        String result = (String) instance.executeCommand("DevString", "Some test value");

        assertEquals("Some test value", result);
    }

    @Test
    public void testExecuteCommand_Void() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy(TEST_TANGO);

        Void result = (Void) instance.executeCommand("DevVoid");

        assertNull(result);
    }

    @Test
    public void testExecuteCommand_State() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy(TEST_TANGO);

        String result = instance.executeCommand("State").toString();

        assertEquals("RUNNING", result);
    }

    //@Test
    public void testAwaitUntil() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy("tango://localhost:10000/sys/tg_test/1");

        long nanosStart = System.nanoTime();
        instance.waitUntil("running", 3000, 100);
        long nanosEnd = System.nanoTime();
        long awaited = nanosEnd - nanosStart;
        assertTrue(awaited > 409343233);
        System.out.println("nano seconds awaited:" + awaited);
    }

    //@Test
    public void testAwaitUntilNot() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy("tango://localhost:10000/sys/tg_test/1");

        long nanosStart = System.nanoTime();
        //device is started in fault state
        instance.waitUntilNot("fault", 3000, 100);
        long nanosEnd = System.nanoTime();
        long awaited = nanosEnd - nanosStart;
        assertTrue(awaited > 409343233);
        System.out.println("nano seconds awaited:" + awaited);
    }

    @Test
    public void testSetTimeout() {
        IDLDeviceProxy instance = new IDLDeviceProxy(TEST_TANGO);

        instance.setTimeout(9000);

        //assume that if nothing bad has happened everything is fine
    }

    //    @Test
    public void testClientId() throws Exception {
        IDLDeviceProxy proxy1 = new IDLDeviceProxy("tango://hzgharwi3:10000/development/1.0.1-SNAPSHOT/0");
        proxy1.setSource(0);
        proxy1.executeCommand("Status");
        proxy1.writeAttributeBoolean("encode", (short)1);

        IDLDeviceProxy proxy2 = new IDLDeviceProxy("tango://hzgharwi3:10000/development/1.0.1-SNAPSHOT/0");
        proxy2.setSource(0);
        proxy2.executeCommand("Status");
        proxy2.writeAttribute("outputType", "JSON");
        boolean result = proxy2.readAttributeBoolean("encode");
        System.out.println(result);
        String outputType = proxy2.readAttributeString("outputType");
        System.out.println(outputType);
        outputType = proxy1.readAttributeString("outputType");
        System.out.println(outputType);
    }

    //@Test
    public void testDevEncoded() throws Exception {
        IDLDeviceProxy uca = new IDLDeviceProxy("tango://hzgpp05ctcam2:10000/p05/camera/kit");

        byte[] image = (byte[]) uca.readAttribute("image");

        Files.write(Files.createTempFile(Paths.get("/tmp"), "idl_", ".jpeg"),image);
    }

    @Test
    public void test_readAttribute() throws Exception {
        IDLDeviceProxy statusServer = new IDLDeviceProxy("tango://hzgxenvtest:10000/test/status_server/0");

        for(int i = 0; i<1000; ++i) {

            DevState state = (DevState) statusServer.readAttribute("State");
            System.out.println(state);
        }

    }

    @Test
    @Ignore
    public void test_executeCommand() throws Exception {
        IDLDeviceProxy statusServer = new IDLDeviceProxy("tango://hzgxenvtest:10000/test/status_server/0");

        for(int i = 0; i<1000; ++i) {

            DevState state = (DevState) statusServer.executeCommand("State");
            System.out.println(state);
        }

    }

    @Test
    public void test_waitUntil() throws Exception {
        IDLDeviceProxy statusServer = new IDLDeviceProxy("tango://hzgxenvtest:10000/test/status_server/0");

            statusServer.waitUntil("ON");

    }

    //@Test
    public void testToTangoProxy() throws Exception {
        IDLDeviceProxy idlDeviceProxy = new IDLDeviceProxy(TEST_TANGO);
        Object proxy = IDLDeviceProxyHelper.toTangoProxy(idlDeviceProxy);

        Method get_double_scalar = proxy.getClass().getDeclaredMethod("getdouble_scalar");
        assertTrue(get_double_scalar != null);

        System.out.println(get_double_scalar.invoke(proxy));
    }
}
