package com.alansf.apexzombiekiller.service;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {
	private static final ObjectMapper MAPPER = new ObjectMapper();
	public static String toJson(Object obj) {
		try {
			return MAPPER.writeValueAsString(obj);
		} catch (Exception e) {
			return "{}";
		}
	}
}


