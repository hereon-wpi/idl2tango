
pro TangoTestIDLClient

  SETENV, 'IDLJAVAB_CONFIG=D:\Projects\hzg.wpn.projects\idl2tango\src\test\idl\idljavabrc'
  
  ; Create a StatusServer proxy
  joDeviceProxy = OBJ_NEW("IDLJavaObject$hzg_wpn_idl_IDLDeviceProxy", "hzg.wpn.idl.IDLDeviceProxy","tango://hzgcttest:10000/sys/tg_test/1")
  
  joDeviceProxy->setLogFile, "D:\Projects\hzg.wpn.projects\idl2tango\target\log"
  joDeviceProxy->setLogLevel, "TRACE"
  
  print, joDeviceProxy->getVersion()
  print, joDeviceProxy->getTangoVersion()

  joDeviceProxy->waitUntil, "RUNNING"
  
  joDeviceProxy->writeAttribute, "double_scalar_w", 3.14
  joDeviceProxy->writeAttribute, "float_scalar", 3.14  
  joDeviceProxy->writeAttribute, "ulong_scalar", 123
  joDeviceProxy->writeAttribute, "float_scalar", 3.14D

  ;IF (OBJ_CLASS(joStr) NE "IDLJAVAOBJECT$JAVA_LANG_STRING") THEN BEGIN
  ;  PRINT, '(ERR) creating java.lang.String.  joStr =', joStr
  ;ENDIF
  
  ; get the string and show it in IDL
  ;PRINT, joClientFactory->createClient("sys/tg_test/1")
  ;joObject = joDeviceProxy->readAttribute("throw_exception")
  
  ;print, joDeviceProxy->readAttributeState() eq "RUNNING"
  ;print, joDeviceProxy->readAttributeDouble("double_scalar")
  
  ;joDeviceProxy->setLogLevel, "error"
  
  
  ;print, joDeviceProxy->readAttributeInteger("long_scalar")
  
  ;joDeviceProxy->readAttribute, "throw_exception"
  
  ;joObject = joDeviceProxy->readAttribute("ushort_image_ro")
  
  ;joImage = joObject->toRenderedImage_sRGB()
  
  ;im = IMAGE(joObject->getData(), joobject->getWidth(), joobject->getHeight())
  
     
  ;joDeviceProxy->writeTangoImageAsJPEG, "D:\Projects\hzg.wpn.projects\idl2tango\target\ushort_image_ro.jpeg", joObject
  
  ;PRINT, joObject.toString()
  
  ; delete the object
  ;OBJ_DESTROY, joObject
  OBJ_DESTROY, joDeviceProxy
  
end