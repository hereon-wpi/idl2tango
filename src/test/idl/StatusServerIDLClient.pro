;
; Usage:
;    IDL> StatusServerIDLClient
;

pro StatusServerIDLClient

  SETENV, 'IDLJAVAB_CONFIG=D:\Projects\hzg.wpn.projects\idl2tango\src\test\idl\idljavabrc'



file = FILEPATH('hzg_p07_01a_nexus.h5')

file_id = H5F_OPEN(file)

dataset_id1 = H5D_OPEN(file_id, '/entry/scan/images/dark/value')

print, dataset_id1  

etwa = H5D_READ(dataset_id1)
;etwa = h5_getdata(file, '/entry/scan/images/dark/value');


print,etwa[1] 


  ; Create a StatusServer proxy
  ;joDeviceProxy = OBJ_NEW("IDLJavaObject$hzg_wpn_idl_IDLDeviceProxy", "hzg.wpn.idl.IDLDeviceProxy","tango://hzgc103k:10000/test/p07/1.0.6")
  ;dfs_proxy = OBJ_NEW("IDLJavaObject$hzg_wpn_idl_IDLDeviceProxy", "hzg.wpn.idl.IDLDeviceProxy", 'tango://hzgpp07ct1:10000/p07/dfs/0')  

  ;dfs_proxy->writeAttribute, "NXpath", "/entry/scan/n_img/value"
  ;dfs_proxy->executeCommand, "writeInteger", LONG(1200)
  ;dfs_proxy->writeAttribute, "NXpath", "/entry/scan/n_img/time"
  ;dfs_proxy->executeCommand, "writeLong", 1000001


;PRINT, joDeviceProxy.getVersion()

  ;IF (OBJ_CLASS(joStr) NE "IDLJAVAOBJECT$JAVA_LANG_STRING") THEN BEGIN
  ;  PRINT, '(ERR) creating java.lang.String.  joStr =', joStr
  ;ENDIF

  ; get the string and show it in IDL
  ;PRINT, joClientFactory->createClient("sys/tg_test/1")

;joObject = joDeviceProxy->executeCommand("getLatestSnapshot")

;PRINT, joObject.toString()

; delete the object
;OBJ_DESTROY, joObject
;OBJ_DESTROY, joDeviceProxy

end