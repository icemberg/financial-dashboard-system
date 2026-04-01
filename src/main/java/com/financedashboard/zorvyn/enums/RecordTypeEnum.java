package com.financedashboard.zorvyn.enums;


import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum RecordTypeEnum {
	
	INCOME("INCOME"),
	EXPENSE("EXPENSE");
	
	private String Value;
	
	RecordTypeEnum(String value) {
		this.Value = value;
	}
	
	
	public String getValue() {
		return Value;
	}
	
	
}
