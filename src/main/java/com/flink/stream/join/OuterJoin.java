package com.flink.stream.join;


import org.apache.flink.api.common.functions.JoinFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.fs.FileSystem;

@SuppressWarnings("serial")
public class OuterJoin {

    public static void main(String[] args) throws Exception {

        //The ExecutionEnvironment is the context in which a program is executed
        final ExecutionEnvironment environment = ExecutionEnvironment.getExecutionEnvironment();

        //Setting passed parameter in args array to ParameterTools for setting in the environment
        final ParameterTool params = ParameterTool.fromArgs(args);

        // Setting passed parameters available in the web interface
        environment.getConfig().setGlobalJobParameters(params);

        // Read timezone file and generate tuples out of each string read
        //This will create a Dataset with tuple2 containing type of element as <Integer,String>
        DataSet<Tuple2<Integer, String>> timeZoneSet = environment.readTextFile(params.get("input1"))
                .map(new MapFunction<String, Tuple2<Integer, String>>() {
                    public Tuple2<Integer, String> map(String value) {
                        String[] words = value.split(",");
                        return new Tuple2<Integer, String>(Integer.parseInt(words[0]), words[1]);
                    }
                });

        // Read timezonesecretecode file and generate tuples out of each string read
        //This will create a Dataset with tuple2 containing type of element as <Integer,String>
        // CreateTuple class used, another way of creating tuple
        DataSet<Tuple2<Integer, String>> secreteCodeSet = environment.readTextFile(params.get("input2")).
                map(new LeftOuterJoin.CreateTuple());

        // join datasets on id
        // joined format will be <id, timezone, secretcode>
        DataSet<Tuple3<Integer, String, String>> joined = timeZoneSet.fullOuterJoin(secreteCodeSet).where(0).equalTo(0)
                .with(new JoinFunction<Tuple2<Integer, String>, Tuple2<Integer, String>, Tuple3<Integer, String, String>>() {
                    public Tuple3<Integer, String, String> join(Tuple2<Integer, String> timeZone, Tuple2<Integer, String> timeZoneSecret) {
                        // check for nulls
                        if (timeZoneSecret == null) {
                            return new Tuple3<Integer, String, String>(timeZone.f0, timeZone.f1, "NULL");
                        }
                        // for rightOuterJoin
                        else if (timeZone == null)
                            return new Tuple3<Integer, String, String>(timeZoneSecret.f0, "NULL", timeZoneSecret.f1);

                        return new Tuple3<Integer, String, String>(timeZone.f0, timeZone.f1, timeZoneSecret.f1);
                    }
                });
        // Write data into the output file, file will get created in join folder of output
        joined.writeAsCsv(params.get("output"), "\n", " <==> ", FileSystem.WriteMode.OVERWRITE);
        environment.execute("Right Outer Join Execution");
    }

    // Creating separate class for Tuple2 generation using passed data in input2
    public static final class CreateTuple implements MapFunction<String, Tuple2<Integer, String>> {
        public Tuple2<Integer, String> map(String value) {
            String[] words = value.split(",");
            return new Tuple2(Integer.parseInt(words[0]), words[1]);
        }
    }
}


