package com.report;

import com.report.service.ReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;



@SpringBootApplication
public class ReportingToolCronJobApplication implements CommandLineRunner {

	@Autowired
	private  ReportingService reportingService;


	public static void main(String[] args) {
		SpringApplication.run(ReportingToolCronJobApplication.class, args);
	}


	public void run(String... args) throws Exception {
		String dateForExecution = "2024-07-31";

	    String objectId="11924";//""45342";
		String request = reportingService.requestCreator(Integer.parseInt(objectId), 4210L, dateForExecution);

		System.out.println(request);
	}

}
