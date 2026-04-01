package com.financedashboard.zorvyn.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum RolesEnum {
	
	VIEWER("VIEWER"),
	ANALYST("ANALYST"),
	ADMIN("ADMIN");
	
	private String Value;
	
	RolesEnum(String value) {
		this.Value = value;
	}
	
	public String getValue() {
		return Value;
	}
}
