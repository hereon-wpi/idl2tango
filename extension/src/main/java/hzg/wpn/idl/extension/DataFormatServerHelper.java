package hzg.wpn.idl.extension;

import hzg.wpn.idl.IDLDeviceProxy;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 08.10.12
 */
public class DataFormatServerHelper {
    /**
     * this list is taken from DataFormatServer mapping.properties
     */
    private static final String[] PEDC_FIELDS = {"CRT_BEAMTIME_ID","CRT_USER_SCAN","CRT_USER_NAME",
            "dct_pixel_size_x","dct_pixel_size_y","instr_insrt_dvc_type","mono_energy",
            "mono_wave_length","fltr_1_desc","fltr_2_desc","fltr_3_desc","instr_distance",
            "instr_name","instr_probe","instr_type","sample_id","sample_desc",
            "ion_mode"};


    private final String userName;
    private final String scanName;

    /**
     *
     *
     * @param crtUser specifies user name whose experiment is being processed
     * @param crtScan specifies scan name which is being run
     */
    public DataFormatServerHelper(String crtUser, String crtScan){
        this.userName = crtUser;
        this.scanName = crtScan;
    }

    //TODO distribute PEDC_FIELDS and mapping
    public void writePreExperimentDataCollectorData(String preExperimentDataCollector, String dataFormatServer){
        IDLDeviceProxy predatorProxy = new IDLDeviceProxy(preExperimentDataCollector);

        predatorProxy.executeCommand("loadDataSet",new String[]{userName,scanName});

        StringBuilder dataBuilder = new StringBuilder();


        String timestamp = String.valueOf(System.currentTimeMillis());

        List<String> result = new ArrayList<String>(PEDC_FIELDS.length);

        for(String fldName : PEDC_FIELDS){
            String dataRow = readAttribute(predatorProxy, dataBuilder, timestamp, fldName);
            result.add(dataRow);
        }


        writeData(dataFormatServer, result);
    }

    public void writeStatusServerData(String statusServer, String dataFormatServer, long timestamp){
        IDLDeviceProxy ssProxy = new IDLDeviceProxy(statusServer);

        StringBuilder dataBuilder = new StringBuilder();
        List<String> result = new ArrayList<String>();

        String strTimestamp = String.valueOf(timestamp);

        Object data = ssProxy.executeCommand("getSnapshot",strTimestamp);

        Pattern pattern = Pattern.compile("(.+)\n@(\\d{13})\\[(.+)@(\\d{13})\\]");
        for(int i = 0; i < Array.getLength(data); i++){
            Object row = Array.get(data, i);


            Matcher m = pattern.matcher((String)row);
            if(m.matches()){
                String key = m.group(1);
                String type = "double";
                String value = m.group(3);
                String writeTimestamp = m.group(2);
                String readTimestamp = m.group(4);

                formDataRow(dataBuilder, key, type, value, writeTimestamp, readTimestamp);

                result.add(dataBuilder.toString());
                dataBuilder.setLength(0);
            } else {
                //Ah, oh
            }
        }

        writeData(dataFormatServer,result);
    }

    private void formDataRow(StringBuilder dataBuilder, String key, String type, String value, String writeTimestamp, String readTimestamp) {
        dataBuilder.append(key).append('=')
                .append('<').append(type).append('>')
                .append(value)
                .append('@').append(readTimestamp)
                .append('$').append(writeTimestamp);
    }

    private void writeData(String dataFormatServer, List<String> result) {
        IDLDeviceProxy dfsProxy = new IDLDeviceProxy(dataFormatServer);

        dfsProxy.executeCommand("CreateOutputFile",scanName);

        dfsProxy.executeCommand("AppendData",result.toArray(new String[result.size()]));
    }

    private String readAttribute(IDLDeviceProxy proxy, StringBuilder dataBuilder, String timestamp, String attrName) {
        try {
            Object value = proxy.readAttribute(attrName);
            String type = value.getClass().getComponentType().getSimpleName();
            formDataRow(dataBuilder, attrName, type, String.valueOf(Array.get(value,0)), timestamp, timestamp);

            return dataBuilder.toString();
        } finally {
            dataBuilder.setLength(0);
        }
    }
}
