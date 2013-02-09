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

import org.junit.Test;
import wpn.hdri.tango.data.EnumDevState;
import wpn.hdri.tango.data.type.ImageTangoDataTypes;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertArrayEquals;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 05.06.12
 */
public class IDLDeviceProxyTest {
    @Test(expected = RuntimeException.class)
    public void testReadAttribute_Failed() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy("sys/tg_test/1");

        String result = (String) instance.readAttribute("string_scalarxxx");//no such attribute

        assertEquals("Some test value", result);
    }

    @Test
    public void testWriteReadAttribute_String() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy("sys/tg_test/1");

        instance.writeAttribute("string_scalar","Some test value");
        String result = (String) instance.readAttribute("string_scalar");

        assertEquals("Some test value", result);
    }

    @Test
    public void testWriteReadAttribute_DoubleArr() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy("sys/tg_test/1");

        instance.writeAttribute("double_spectrum",new double[]{1.0,1.1,1.2,1.3,1.4});
        double[] result = (double[]) instance.readAttribute("double_spectrum");

        assertArrayEquals(new double[]{1.0, 1.1, 1.2, 1.3, 1.4}, result, 0.0);
    }

    @Test
    public void testWriteReadAttribute_DoubleSpectrum() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy("sys/tg_test/1");

        int size = 256; //sys/tg_test/1 has hardcoded maximum value of 256
        double [] sinus = new double[size];
        double coeff = Math.PI / 180;
        for(int i = 0; i<size; i++){
            sinus[i] = Math.sin(i * coeff);
        }

        instance.writeAttribute("double_spectrum",sinus);

        double[] result = (double[]) instance.readAttribute("double_spectrum");

        assertArrayEquals(sinus, result, 0.0);
    }

    @Test
    public void testWriteReadAttribute_Int() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy("sys/tg_test/1");

        instance.writeAttribute("long_scalar_w",1234);
        int result = instance.readAttributeInteger("long_scalar_w");

        assertEquals(1234, result);
    }

    @Test
    public void testWriteReadAttribute_Double() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy("sys/tg_test/1");

        instance.writeAttribute("double_scalar_w",3.14D);

        double result = instance.readAttributeDouble("double_scalar_w");

        assertEquals(3.14D, result);
    }

    @Test
    public void testWriteReadAttribute_FloatImage() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy("sys/tg_test/1");

        instance.writeAttribute("float_image",new float[][]{{0.1F,0.2F},{0.3F,0.4F},{0.5F,0.6F},{0.5F,0.6F}});

        float[][] result = (float[][]) instance.readAttribute("float_image");

        assertArrayEquals(new float[]{0.1F,0.2F}, result[0],0.0F);
        assertArrayEquals(new float[]{0.3F,0.4F}, result[1],0.0F);
        assertArrayEquals(new float[]{0.5F,0.6F}, result[2],0.0F);
        assertArrayEquals(new float[]{0.5F,0.6F}, result[3],0.0F);
    }

    @Test
     public void testWriteReadAttribute_DoubleImage() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy("sys/tg_test/1");

        instance.writeAttribute("double_image",new double[][]{{0.1,0.2},{0.3,0.4},{0.5,0.6},{0.5,0.6}});

        double[][] result = (double[][]) instance.readAttribute("double_image");

        assertArrayEquals(new double[]{0.1,0.2}, result[0],0.0);
        assertArrayEquals(new double[]{0.3,0.4}, result[1],0.0);
        assertArrayEquals(new double[]{0.5,0.6}, result[2],0.0);
        assertArrayEquals(new double[]{0.5,0.6}, result[3],0.0);
    }

    @Test
    public void testWriteReadAttribute_Image() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy("sys/tg_test/1");


        BufferedImage img = ImageIO.read(IDLDeviceProxy.class.getResourceAsStream("/images/check.png"));
        Raster imgRaster = img.getData();
        int imgWidth = imgRaster.getWidth(), imgHeight = imgRaster.getHeight();
        int [][] pixels = new int[img.getHeight()][img.getWidth()];
        for(int i = 0,
                    x = imgRaster.getMinX(), y = imgRaster.getMinY(),
                    imgSize = imgWidth*imgHeight;
            i < imgSize;
            i++, x = x<(imgWidth-1) ? x+1 : 0, y += x==0 ? 1:0){
            int[] RGBa = imgRaster.getPixel(x,y,new int[4]);
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

        instance.writeAttribute("ushort_image",pixels);

        int[][] result = (int[][]) instance.readAttribute("ushort_image");

        BufferedImage imgResult = new BufferedImage(imgWidth,imgHeight,BufferedImage.TYPE_USHORT_555_RGB);
        for(int i = 0,x = 0,y = 0,size = imgWidth*imgWidth;
            i<size;
            i++,x = x<(imgWidth-1) ? x+1 : 0, y += x==0 ? 1:0){
            imgResult.setRGB(x,y,result[y][x]);
        }

        ImageIO.write(imgResult,"png",new File("target/result_image.png"));
    }

    //@Test
    public void testReadAttribute_BigImage() throws Exception {
        IDLDeviceProxy instance = new IDLDeviceProxy("tango://hasgkssw2ctcam1b.desy.de:10000/w2/cameratest/ccdfli");
        TimeUnit nanos = TimeUnit.NANOSECONDS;

//        instance.writeAttribute("double_image",pixels);

        System.setProperty(ImageTangoDataTypes.TANGO_IMAGE_EXTRACTER_USES_MULTITHREADING,"true");
        for(int i = 0 ;i<10;i++){
        long startReadImage = System.nanoTime();
        int[][] result = (int[][]) instance.readAttribute("Image");
        long endReadImage = System.nanoTime();

        System.out.println("Read image time (millis):" + nanos.toMillis(endReadImage-startReadImage));
        }
//
//        assertArrayEquals(new double[]{0.1,0.2}, result[0],0.0);
//        assertArrayEquals(new double[]{0.3,0.4}, result[1],0.0);
//        assertArrayEquals(new double[]{0.5,0.6}, result[2],0.0);
    }

    @Test
    public void testExecuteCommand() throws Exception{
        IDLDeviceProxy instance = new IDLDeviceProxy("sys/tg_test/1");

        String result = (String) instance.executeCommand("DevString", "Some test value");

        assertEquals("Some test value", result);
    }

    @Test
    public void testExecuteCommand_Void() throws Exception{
        IDLDeviceProxy instance = new IDLDeviceProxy("sys/tg_test/1");

        Void result = (Void) instance.executeCommand("DevVoid");

        assertNull(result);
    }

    @Test
    public void testExecuteCommand_State() throws Exception{
        IDLDeviceProxy instance = new IDLDeviceProxy("sys/tg_test/1");

        EnumDevState result = (EnumDevState) instance.executeCommand("State");

        assertSame(EnumDevState.RUNNING, result);
    }

    //@Test
    public void testAwaitUntil() throws Exception{
        IDLDeviceProxy instance = new IDLDeviceProxy("sys/tg_test/1");

        long nanosStart = System.nanoTime();
        instance.waitUntil("running");
        long nanosEnd = System.nanoTime();
        long awaited = nanosEnd - nanosStart;
        assertTrue(awaited > 409343233);
        System.out.println("nano seconds awaited:" + awaited);
    }

    //@Test
    public void testAwaitUntilNot() throws Exception{
        IDLDeviceProxy instance = new IDLDeviceProxy("sys/tg_test/1");

        long nanosStart = System.nanoTime();
        //device is started in fault state
        instance.waitUntilNot("fault");
        long nanosEnd = System.nanoTime();
        long awaited = nanosEnd - nanosStart;
        assertTrue(awaited > 409343233);
        System.out.println("nano seconds awaited:" + awaited);
    }

    @Test
    public void testSetTimeout(){
        IDLDeviceProxy instance = new IDLDeviceProxy("sys/tg_test/1");

        instance.setTimeout(9000);

        //assume that if nothing bad has happened everything is fine
    }
}
