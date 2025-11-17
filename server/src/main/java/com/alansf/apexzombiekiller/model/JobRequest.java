package com.alansf.apexzombiekiller.model;

public class JobRequest {
	public Integer batchSize;
	public Integer maxConcurrency;
	public DateRange dateRange;
	public Boolean dryRun;

	public static class DateRange {
		public String from;
		public String to;
	}
}




