;
; Usage:
;    IDL> StatusServerIDLClient
;

pro StatusServerIDLClient

  SETENV, 'IDLJAVAB_CONFIG=D:\Projects\hzg.wpn.projects\idl2tango\src\test\idl\idljavabrc'

  ; Create a StatusServer proxy
  joDeviceProxy = OBJ_NEW("IDLJavaObject$hzg_wpn_idl_IDLDeviceProxy", "hzg.wpn.idl.IDLDeviceProxy","tango://hzgc103k:10000/test/p07/1.0.6")  


PRINT, joDeviceProxy.getVersion()

  ;IF (OBJ_CLASS(joStr) NE "IDLJAVAOBJECT$JAVA_LANG_STRING") THEN BEGIN
  ;  PRINT, '(ERR) creating java.lang.String.  joStr =', joStr
  ;ENDIF

  ; get the string and show it in IDL
  ;PRINT, joClientFactory->createClient("sys/tg_test/1")

joObject = joDeviceProxy->executeCommand("getLatestSnapshot")

PRINT, joObject.toString()

; delete the object
OBJ_DESTROY, joObject
OBJ_DESTROY, joDeviceProxy

end