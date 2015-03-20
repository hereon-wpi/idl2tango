package hzg.wpn.tango.ss.client.test;

import hzg.wpn.idl.IDLDeviceProxy;

import java.io.BufferedWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 20.03.2015
 */
public class StatusServerClientTest {
    public static void main(String[] args) throws Exception{
        IDLDeviceProxy proxy = new IDLDeviceProxy("tango://hzgc103k:10000/test/p07/1.0.6");
        BufferedWriter writer = null;
        try {
            writer = Files.newBufferedWriter(Paths.get("testStatusServerClient.out"), Charset.forName("UTF-8"));
            while (true) {

                long start = System.nanoTime();
                String[] data = (String[]) proxy.executeCommand("getLatestSnapshot");
                long end = System.nanoTime();
                for (String s : data)
                    writer.append(s);

                writer.append('\n').append("Respond in (ms):").append(
                        Long.toString(
                                TimeUnit.MILLISECONDS.convert(end - start, TimeUnit.NANOSECONDS))).append('\n');
                Thread.sleep(10);
            }
        }finally {
            writer.close();
        }
    }
}
