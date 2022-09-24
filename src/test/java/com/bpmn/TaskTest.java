package com.bpmn;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.query.Pageable;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.model.payloads.ClaimTaskPayload;
import org.activiti.api.task.runtime.TaskRuntime;
import org.activiti.engine.history.HistoricTaskInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class TaskTest {

    private final Logger logger = LoggerFactory.getLogger(TaskTest.class);
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Autowired
    private TaskRuntime taskRuntime;

    /**
     * 查询Task
     */
    @Test
    @WithUserDetails(value = "zsan", userDetailsServiceBeanName = "userDetailsService")
    public void queryTask() {
        Page<Task> taskPage = taskRuntime.tasks(Pageable.of(0,10));
        logger.info("Number of tasks : {}",  taskPage.getTotalItems());
        for (Task t : taskPage.getContent()) {
            logger.info("任务实例：{}" , gson.toJson(t));
        }
    }

    /**
     * 认领Task
     */
    @Test
    @WithUserDetails(value = "admin", userDetailsServiceBeanName = "userDetailsService")
    public void claimTask() {
        ClaimTaskPayload claimTaskPayload = new ClaimTaskPayload();
        claimTaskPayload.setTaskId("782469ed-3bcd-11ed-ab41-f6335fb4fb12");
        Task task = taskRuntime.claim(claimTaskPayload);
        logger.info("Task >> {}", task);
    }

    /**
     * 启动一个实例
     */
    @Test
    @WithUserDetails(value = "bob", userDetailsServiceBeanName = "userDetailsService")
    public void startProcessInstance() {
        Page<Task> taskPage = taskRuntime.tasks(Pageable.of(0,10));
        System.out.println("Number of process definitions : " + taskPage.getTotalItems());
        System.out.println("Number of tasks : " +  taskPage.getTotalItems());
        taskRuntime.claim(TaskPayloadBuilder.claim().withTaskId(taskPage.getContent().get(0).getId()).build());
        taskRuntime.complete(TaskPayloadBuilder.complete().withTaskId(taskPage.getContent().get(0).getId()).build());
    }

}