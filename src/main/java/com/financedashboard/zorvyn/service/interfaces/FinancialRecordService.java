package com.financedashboard.zorvyn.service.interfaces;


public interface FinancialRecordService {
	
	public void createRecord();
	
	public void readRecordById();
	
	public void readAllRecords();
	
	public void updateRecordById();
	
	public void softDeleteRecord();
}
