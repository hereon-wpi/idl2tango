# Welcome

The latest version is 1.1.2 and can be downloaded from [here](https://bintray.com/artifact/download/hzgde/hzg-wpn-projects/hzg/wpn/xenv/IDL2TangoJavaClient/1.1.2/IDL2TangoJavaClient-1.1.2.jar)

[JavaDoc](http://hzgwpn.bitbucket.org/idl2java)

# Release notes 

## 1.1.2

1) An external logging is added: idl2tango.log file is created in %user.home% or in %XENV_ROOT%/var/log/idl

Log looks like the following:

```
DEBUG 28-09-2015 12:48:15 [IDL - h.w.i.IDLDeviceProxy] Creating proxy for device [tango://hzgpp07ct1:10000/p07/dfa/status_server_beam_current,useEventsForWaitUntil=false]
TRACE 28-09-2015 12:48:15 [IDL - h.w.i.IDLDeviceProxy] Set p07/dfa/status_server_beam_current/timeout=10000
TRACE 28-09-2015 12:48:15 [IDL - h.w.i.IDLDeviceProxy] Set p07/dfa/status_server_beam_current/source=0 aka DEV
...
TRACE 28-09-2015 13:01:17 [IDL - h.w.i.IDLDeviceProxy] Waiting until not p07ct/attocubes/axis_y/moving
TRACE 28-09-2015 13:01:17 [IDL - h.w.i.IDLDeviceProxy] Done waiting.
TRACE 28-09-2015 13:01:17 [IDL - h.w.i.IDLDeviceProxy] Waiting until not p07ct/attocubes/axis_z/moving
TRACE 28-09-2015 13:01:17 [IDL - h.w.i.IDLDeviceProxy] Done waiting.
TRACE 28-09-2015 13:01:17 [IDL - h.w.i.IDLDeviceProxy] Reading p07ct/attocubes/axis_t/State
TRACE 28-09-2015 13:01:17 [IDL - h.w.i.IDLDeviceProxy] Reading p07ct/attocubes/axis_p/State
```

One can set output file using:

```
#!idl
joDeviceProxy->setLogFile, "D:\Projects\hzg.wpn.projects\idl2tango\target\log"
```

where joDeviceProxy is any IDLDeviceProxy

or set log level:

```
#!idl
joDeviceProxy->setLogLevel, "trace"
```

available log levels are: ERROR, INFO, DEBUG, TRACE

2) New error message in IDL's console:

```
% ProxyException in sys/tg_test/1
  ERR: exception test
      here is the exception you requested
      TangoTest::read_throw_exception
% Exception thrown
% Execution halted at: TANGOTESTIDLCLIENT   33 D:\Projects\hzg.wpn.projects\idl2tango\src\test\idl\TangoTestIDLClient.pro
%                      $MAIN$
```

3) ushort read/write is fixed