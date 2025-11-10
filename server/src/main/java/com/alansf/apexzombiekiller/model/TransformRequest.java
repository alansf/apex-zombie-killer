package com.alansf.apexzombiekiller.model;

public class TransformRequest {
	public String apexCode;
	public Options options;

	public static class Options {
		public Boolean useHerokuConnect;
		public Boolean generateTests;
	}
}


