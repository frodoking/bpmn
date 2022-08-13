package com.bpmn;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.query.Pageable;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.model.payloads.ClaimTaskPayload;
import org.activiti.api.task.runtime.TaskRuntime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.junit4.SpringRunner;

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
    @WithUserDetails(value = "zsan", userDetailsServiceBeanName = "userDetailsService")
    public void claimTask() {
        ClaimTaskPayload claimTaskPayload = new ClaimTaskPayload();
        claimTaskPayload.setTaskId("4acb3a06-1964-11ed-a5d9-d6a488707b26");
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



    /**
     * 查询用户的任务列表
     */
    @Test
    public void taskQuery() {
        //根据流程定义的key,负责人assignee来实现当前用户的任务列表查询
        Page<Task> list = taskRuntime.tasks(Pageable.of(0,10));

        if (list != null && list.getTotalItems() > 0) {
            for (Task task : list.getContent()) {
                System.out.println("任务ID:" + task.getId());
                System.out.println("任务名称:" + task.getName());
                System.out.println("任务的创建时间:" + task.getCreatedDate());
                System.out.println("任务的办理人:" + task.getAssignee());
                System.out.println("流程实例ID：" + task.getProcessInstanceId());
                System.out.println("执行对象ID:" + task.getAssignee());
                System.out.println("流程定义ID:" + task.getProcessDefinitionId());
                System.out.println("getOwner:" + task.getOwner());
                System.out.println("getDescription:" + task.getDescription());
                System.out.println("getFormKey:" + task.getFormKey());
            }
        }
    }

    /**
     * 历史活动实例查询
     */
//    @Test
//    public void queryHistoryTask() {
//        List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery() // 创建历史活动实例查询
//                .processInstanceId("9671cdea-3367-11ea-a057-30b49ec7161f") // 执行流程实例id
//                .orderByTaskCreateTime()
//                .asc()
//                .list();
//        for (HistoricTaskInstance hai : list) {
//            System.out.println("活动ID:" + hai.getId());
//            System.out.println("流程实例ID:" + hai.getProcessInstanceId());
//            System.out.println("活动名称：" + hai.getName());
//            System.out.println("办理人：" + hai.getAssignee());
//            System.out.println("开始时间：" + hai.getStartTime());
//            System.out.println("结束时间：" + hai.getEndTime());
//        }
//    }
}