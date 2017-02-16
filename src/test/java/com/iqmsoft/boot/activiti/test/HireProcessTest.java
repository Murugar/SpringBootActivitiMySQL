package com.iqmsoft.boot.activiti.test;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.subethamail.wiser.Wiser;

import com.iqmsoft.boot.activiti.MainApp;
import com.iqmsoft.boot.activiti.model.Applicant;
import com.iqmsoft.boot.activiti.repos.ApplicantRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {MainApp.class})
@WebAppConfiguration
@IntegrationTest
public class HireProcessTest {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ApplicantRepository applicantRepository;

    private Wiser wiser;

    @Before
    public void setup() {
        wiser = new Wiser();
        wiser.setPort(1025);
        wiser.start();
    }

    @After
    public void cleanup() {
        wiser.stop();
    }

    @Test
    public void testHappyPath() {
    	
    	long count1 = historyService.createHistoricProcessInstanceQuery().finished().count();

        Applicant applicant = new Applicant("John Doe", "john@activiti.org", "12344");
        applicantRepository.save(applicant);

        // Start process instance
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("applicant", applicant);
        ProcessInstance processInstance = runtimeService.
        		startProcessInstanceByKey("hireProcessWithJpa", variables);

    
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .taskCandidateGroup("prod-managers")
                .singleResult();
        Assert.assertEquals("Telephone Interview", task.getName());
      
      
        Map<String, Object> taskVariables = new HashMap<String, Object>();
        taskVariables.put("telephoneInterviewOutcome", true);
        taskService.complete(task.getId(), taskVariables);

        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .orderByTaskName().asc()
                .list();
        Assert.assertEquals(3, tasks.size());
        Assert.assertEquals("Fix Perks", tasks.get(0).getName());
        Assert.assertEquals("Fix Salary", tasks.get(1).getName());
        Assert.assertEquals("Tech Interview", tasks.get(2).getName());

        taskVariables = new HashMap<String, Object>();
        taskVariables.put("techOk", true);
        taskService.complete(tasks.get(0).getId(), taskVariables);

        taskVariables = new HashMap<String, Object>();
        taskVariables.put("financialOk", true);
        taskService.complete(tasks.get(1).getId(), taskVariables);
        
        taskVariables = new HashMap<String, Object>();
        taskVariables.put("compOk", true);
        taskService.complete(tasks.get(2).getId(), taskVariables);

        // Verify email
        Assert.assertEquals(1, wiser.getMessages().size());
        
        

        long count = historyService.createHistoricProcessInstanceQuery().finished().count();
        
        // Verify process completed
        Assert.assertEquals(count1 + 1, count);

    }

}
