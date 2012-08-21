package com.cloudera.sa.tera.compress;

public class TeraMain
{

	public static void main(String[] args) throws Exception
	{
		if (args.length < 1 || args[0].contains("help"))
		{
			help();
			return;
		}
		
		String command = args[0];
		
		String[] subCommand = args;
		String[] subCommandArgs = new String[subCommand.length-1];
		System.arraycopy( subCommand, 1, subCommandArgs, 0, subCommandArgs.length );
		
		if (command.equals("gen"))
		{
			TeraGenCompressed.main(subCommandArgs);
		}else if (command.equals("sort"))
		{
			TeraSortCompressed.main(subCommandArgs);
		}else if (command.equals("validate"))
		{
			TeraValidateCompressed.main(subCommandArgs);
		}else
		{
			help();
			return;
		}
	}

	private static void help()
	{
		System.out.println("Tera Compress Tools");
		System.out.println("");
		System.out.println("Frist parameter must be one of the following:");
		System.out.println("gen : for generating data to be sorted");
		System.out.println("sort : for sorting generated data");
		System.out.println("validate : for validating sort data");
	}

}
