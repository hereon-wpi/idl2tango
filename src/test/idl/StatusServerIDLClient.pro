;
; Usage:
;    IDL> StatusServerIDLClient
;

pro StatusServerIDLClient

  SETENV, 'IDLJAVAB_CONFIG=D:\Projects\hzg.wpn.projects\idl2tango\src\test\idl\idljavabrc'

  CATCH, error_status
  IF error_status NE 0 THEN BEGIN
    ; Use session object to get our Exception
    oJExc = oJBridgeSession->GetException()

    PRINT, 'Exception thrown:', oJExc->ToString()
    oJExc->PrintStackTrace
    ; Cleanup
    OBJ_DESTROY, oJExc
   ENDIF
  ; Create a StatusServer proxy
  joStatusServer = OBJ_NEW("IDLJavaObject$WPN_HDRI_SS_TANGO_STATUSSERVER", "wpn.hdri.ss.tango.StatusServer")  


  ;IF (OBJ_CLASS(joStr) NE "IDLJAVAOBJECT$JAVA_LANG_STRING") THEN BEGIN
  ;  PRINT, '(ERR) creating java.lang.String.  joStr =', joStr
  ;ENDIF

  ; get the string and show it in IDL
  ;PRINT, joClientFactory->createClient("sys/tg_test/1")

joObject = joStatusServer->getLatestSnapshot()

PRINT, joObject.toString()

; delete the object
OBJ_DESTROY, joObject
OBJ_DESTROY, joStatusServer

end