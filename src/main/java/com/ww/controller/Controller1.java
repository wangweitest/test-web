package com.ww.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.service.PersonService;
import com.ww.util.ZkCuratorLock;

@Controller
public class Controller1 {

	
	@Autowired
	PersonService personService;
	@Autowired
	ZkCuratorLock zkCuratorLock;
	
	public Controller1() {
		System.out.println("Controller1");
	}
	
	//直接放在tomcat上执行的
	//访问：http://localhost:8080/test-web2/test1
	@RequestMapping("/test1")
	@ResponseBody
	public String test1(){
		System.out.println("test1.... ");
		return "test1";
	}
	
	@RequestMapping("/testService")
	@ResponseBody
	public String testService(){
		System.out.println("testService.... :" + personService.testService());
		return personService.testService();
	}
	
	
	@RequestMapping("/testZkLock")
	@ResponseBody
	public String testCurator() {
		System.out.println(zkCuratorLock.getClient());
		System.out.println(zkCuratorLock.isConnect());
		
		zkCuratorLock.getLock();
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		zkCuratorLock.releaseLock();
		
		return "testZkLock";
	}
	
	
}
