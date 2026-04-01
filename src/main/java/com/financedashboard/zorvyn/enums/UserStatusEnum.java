package com.financedashboard.zorvyn.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum UserStatusEnum {
	
	ACTIVE("ACTIVE"),
	INACTIVE("INACTIVE");
	
	private String Value;
	
	UserStatusEnum(String value) {
		this.Value = value;
	}
	
	
	public String getValue() {
		return Value;
	}
	
}
