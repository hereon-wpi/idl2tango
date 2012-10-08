package hzg.wpn.idl.extension;

import org.junit.Test;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 08.10.12
 */
public class DataFormatServerHelperTest {
    @Test
    public void testWritePreExperimentDataCollectorData() throws Exception {
        DataFormatServerHelper instance = new DataFormatServerHelper("khokhria","someScan");

        instance.writePreExperimentDataCollectorData("tango://hzgpp07ct1.desy.de:10000/p07/pedc-1.7/0",
                "tango://hzgharwi3:10000/development/dfs/0");


    }

    @Test
    public void testWriteStatusServerData(){
        DataFormatServerHelper instance = new DataFormatServerHelper("khokhria","someScan");

        instance.writeStatusServerData("tango://hzgharwi3:10000/development/ss/0",
                "tango://hzgharwi3:10000/development/dfs/0",System.currentTimeMillis());
    }
}
