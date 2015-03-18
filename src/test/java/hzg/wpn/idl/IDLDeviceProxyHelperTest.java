package hzg.wpn.idl;


import org.junit.Test;

import java.lang.reflect.Method;

import static junit.framework.Assert.assertTrue;

public class IDLDeviceProxyHelperTest {

    @Test
    public void testToTangoProxy() throws Exception {
        IDLDeviceProxy idlDeviceProxy = new IDLDeviceProxy("tango://localhost:10000/sys/tg_test/1");
        Object proxy = IDLDeviceProxyHelper.toTangoProxy(idlDeviceProxy);

        Method get_double_scalar = proxy.getClass().getDeclaredMethod("getdouble_scalar");
        assertTrue(get_double_scalar != null);

        System.out.println(get_double_scalar.invoke(proxy));
    }
}