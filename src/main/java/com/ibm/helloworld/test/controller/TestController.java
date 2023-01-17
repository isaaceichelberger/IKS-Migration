package com.ibm.helloworld.test.controller;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ibm.helloworld.test.PGConnect;

@Controller
public class TestController {
	
	@RequestMapping("/hello")
	@ResponseBody
	public String helloWorld() {
		return "Hello World!";
	}
	
	@RequestMapping("/")
	@ResponseBody
	public String home(){
	    // PGConnect icd = new PGConnect();
	    // String string = "";

	    // try {
	    //     Connection connection = icd.connect();
	    //     Statement stmt = connection.createStatement();

	    //     ResultSet rs = stmt.executeQuery("SELECT * from pg_database");
	    //     while (rs.next()) {
	    //         System.out.println("DB Name: " + rs.getString(1));
	    //         string = string + rs.getString(1) + "\n";
	    //     }

	    // } catch (SQLException e) {
	    //     System.out.println(e.getMessage());
	    // }
	    // return string;
		return "Hello world!";
	}
}
