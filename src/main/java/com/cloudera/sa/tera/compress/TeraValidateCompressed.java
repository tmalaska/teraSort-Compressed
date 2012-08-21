package com.cloudera.sa.tera.compress;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class TeraValidateCompressed extends Configured implements Tool
{
	private static final Text error = new Text("error");

	static class ValidateMapper extends MapReduceBase implements Mapper<Text, Text, Text, Text>
	{
		private Text lastKey;
		private OutputCollector<Text, Text> output;
		private String filename;

		/**
		 * Get the final part of the input name
		 * 
		 * @param split
		 *            the input split
		 * @return the "part-00000" for the input
		 */
		private String getFilename(FileSplit split)
		{
			return split.getPath().getName();
		}

		public void map(Text key, Text value, OutputCollector<Text, Text> output, Reporter reporter) throws IOException
		{
			if (lastKey == null)
			{
				filename = getFilename((FileSplit) reporter.getInputSplit());
				output.collect(new Text(filename + ":begin"), key);
				lastKey = new Text();
				this.output = output;
			} else
			{
				if (key.compareTo(lastKey) < 0)
				{
					output.collect(error, new Text("misorder in " + filename + " last: '" + lastKey + "' current: '" + key + "'"));
				}
			}
			lastKey.set(key);
		}

		public void close() throws IOException
		{
			if (lastKey != null)
			{
				output.collect(new Text(filename + ":end"), lastKey);
			}
		}
	}

	/**
	 * Check the boundaries between the output files by making sure that the
	 * boundary keys are always increasing. Also passes any error reports along
	 * intact.
	 */
	static class ValidateReducer extends MapReduceBase implements Reducer<Text, Text, Text, Text>
	{
		private boolean firstKey = true;
		private Text lastKey = new Text();
		private Text lastValue = new Text();

		public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException
		{
			if (error.equals(key))
			{
				while (values.hasNext())
				{
					output.collect(key, values.next());
				}
			} else
			{
				Text value = values.next();
				if (firstKey)
				{
					firstKey = false;
				} else
				{
					if (value.compareTo(lastValue) < 0)
					{
						output.collect(error, new Text("misordered keys last: " + lastKey + " '" + lastValue + "' current: " + key + " '" + value + "'"));
					}
				}
				lastKey.set(key);
				lastValue.set(value);
			}
		}

	}

	public int run(String[] args) throws Exception
	{
		JobConf job = (JobConf) getConf();
		
		
		String compressionType = args[2];
	    
	    if (compressionType.equals("none"))
	    {
	    	TeraInputCompressedFormat.setInputPaths(job, new Path(args[0]));
	    	job.setInputFormat(TeraInputCompressedFormat.class);
	    }else 
	    {
		    job.setInputFormat(SequenceFileInputFormat.class);
		    SequenceFileInputFormat.setInputPaths(job, new Path(args[0]));
	    }
		
		
		
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		job.setJobName("TeraValidate");
		job.setJarByClass(TeraValidateCompressed.class);
		job.setMapperClass(ValidateMapper.class);
		job.setReducerClass(ValidateReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		// force a single reducer
		job.setNumReduceTasks(1);
		// force a single split
		job.setLong("mapred.min.split.size", Long.MAX_VALUE);
		JobClient.runJob(job);
		return 0;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception
	{
		if (args.length < 3)
		{
			System.out.println("TeraValidate - Compressed");
			System.out.println("");
			System.out.println("Parameters:");
			System.out.println("{input file} {output file} {compression code (none, snappy, gzip)}");
			System.out.println("");
			System.out.println("All compress is done in sequence files");
			
			return;
		}
		
		int res = ToolRunner.run(new JobConf(), new TeraValidateCompressed(), args);
		System.exit(res);
	}

}
