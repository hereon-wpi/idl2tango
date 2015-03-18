package hzg.wpn.idl;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.AttributeInfo;
import fr.esrf.TangoApi.CommandInfo;
import fr.esrf.TangoApi.DeviceProxy;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtNewMethod;
import org.tango.client.ez.data.type.TangoDataType;
import org.tango.client.ez.data.type.TangoDataTypes;
import org.tango.client.ez.proxy.TangoProxies;
import org.tango.client.ez.proxy.TangoProxy;
import org.tango.client.ez.util.TangoUtils;


/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 18.03.2015
 */
public class IDLDeviceProxyHelper {
    private IDLDeviceProxyHelper(){}

    @SuppressWarnings("unchecked")
    public static <T extends TangoProxy> T toTangoProxy(IDLDeviceProxy proxy) throws Exception {
        try {
            DeviceProxy deviceProxy = proxy.proxy.toDeviceProxy();

            String className = deviceProxy.get_class();

            AttributeInfo[] attrInfos = deviceProxy.get_attribute_info();
            CommandInfo[] cmdInfos    = deviceProxy.command_list_query();

            ClassPool classPool = ClassPool.getDefault();

            CtClass _super = classPool.makeInterface(TangoProxy.class.getCanonicalName());
            CtClass _clazz = classPool.makeInterface(className + (int)(Math.random()*1000),_super);


            for(AttributeInfo attr : attrInfos){
                TangoDataType<?> type = TangoDataTypes.forTangoDevDataType(attr.data_type);
                CtClass rt = classPool.makeClass(type.getDataType().getCanonicalName());
                _clazz.addMethod(CtNewMethod.abstractMethod(rt, "get" + attr.name, new CtClass[0], new CtClass[0], _clazz));
            }


            //TODO read tango class
            //TODO generate java class
            //TODO load generated class
            //TODO create interface specific proxy
            Class<? extends TangoProxy> clazz = _clazz.toClass();
            return (T)TangoProxies.newTangoProxy(proxy.proxy.getName(), clazz);
        } catch (DevFailed devFailed) {
            throw TangoUtils.convertDevFailedToException(devFailed);
        }
    }
}
