package com.flink.join;


import org.apache.flink.api.common.functions.*;
import org.apache.flink.api.java.*;
import org.apache.flink.api.java.tuple.*;
import org.apache.flink.api.java.utils.*;
import scala.Int;


public class InnerJoin {

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
                .map(new MapFunction<String, Tuple2<Integer, String>>()
                {
                    public Tuple2<Integer, String> map(String value) {
                        String[] words = value.split(",");
                        return new Tuple2<Integer, String>(Integer.parseInt(words[0]), words[1]);
                    }
                });
        // Read timezonesecretecode file and generate tuples out of each string read
        //This will create a Dataset with tuple2 containing type of element as <Integer,String>
        // CreateTuple class used, another way of creating tuple
        DataSet<Tuple2<Integer, String>> secreteCodeSet = environment.readTextFile(params.get("input2")).
                map(new CreateTuple());

        // join datasets on id
        // joined format will be <id, timezone, secretcode>
        DataSet<Tuple3<Integer, String, String>> joined = timeZoneSet.join(secreteCodeSet).where(0).equalTo(0)
                .with(new JoinFunction<Tuple2<Integer, String>, Tuple2<Integer, String>, Tuple3<Integer, String, String>>() {
                    public Tuple3<Integer, String, String> join(Tuple2<Integer, String> timezone, Tuple2<Integer, String> timezonesecret) {
                        return new Tuple3<Integer, String, String>(timezone.f0, timezone.f1, timezonesecret.f1);         // returns tuple of (1 John DC)
                    }
                });
        // Write data into the output file, file will get created in join folder of output
        joined.writeAsCsv(params.get("output"), "\n", " <==> ");
        environment.execute("Inner Join Execution");
    }
    // Creating separate class for Tuple2 generation using passed data in input2
    public static final class CreateTuple implements MapFunction<String, Tuple2<Integer, String>>{
        public Tuple2<Integer, String> map(String value) {
            String[] words = value.split(",");
            return new Tuple2(Integer.parseInt(words[0]), words[1]);
        }
    }
}
