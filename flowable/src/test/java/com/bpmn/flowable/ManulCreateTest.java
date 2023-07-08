package com.bpmn.flowable;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.assertj.core.util.Lists;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.*;
import org.flowable.engine.impl.dynamic.DynamicUserTaskBuilder;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Comment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StopWatch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author frodoking
 * @ClassName: ManulCreateTest
 * @date 2023/6/12
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class ManulCreateTest {

    private final Logger logger = LoggerFactory.getLogger(ManulCreateTest.class);

    private static final String PROCESSID = "flowable-0619";
    private static final String PROCESSNAME = "出差申请单-动态生成-0619";

    @Before
    public void setUp() {
        ProcessEngines.getDefaultProcessEngine().getProcessEngineConfiguration().getEventDispatcher().addEventListener(new FlowableEventListener() {
            @Override
            public void onEvent(FlowableEvent flowableEvent) {
                if (flowableEvent instanceof FlowableEntityEvent) {
                    if (flowableEvent.getType().name().contains("TASK")) {
                        logger.info("onEvent >> {}, {}", flowableEvent.getType(), ((FlowableEntityEvent) flowableEvent).getEntity());
                    }
                }
            }

            @Override
            public boolean isFailOnException() {
                return false;
            }

            @Override
            public boolean isFireOnTransactionLifecycleEvent() {
                return false;
            }

            @Override
            public String getOnTransaction() {
                return null;
            }
        });
    }

    @Test
    public void createBpmnTest() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();

        RuntimeService runtimeService = processEngine.getRuntimeService();
        RepositoryService repositoryService = processEngine.getRepositoryService();
        TaskService taskService = processEngine.getTaskService();
        ManagementService managementService = processEngine.getManagementService();
        IdentityService identityService = processEngine.getIdentityService();
        HistoryService historyService = processEngine.getHistoryService();
        FormService formService = processEngine.getFormService();
        DynamicBpmnService dynamicBpmnService = processEngine.getDynamicBpmnService();

        new BPMNBuilder().buildMultiInstance(PROCESSID, PROCESSNAME);
    }

    @Test
    public void exportBpmnTest() {
        String processDefinitionId = "flowable-0615-01:1:55c98e28-0b22-11ee-a11d-a85e455df905";
        new BPMNBuilder().exportBpmn(processDefinitionId);
    }

    @Test
    public void completeTaskTest() {
        String taskId = "28595701-0e50-11ee-83f2-a85e455df905";
        String processInstanceId = "2854ea06-0e50-11ee-83f2-a85e455df905";
        String message = "Task1完成00000";

        completeTask(taskId, processInstanceId, message);
    }

    @Test
    public void rollbackTaskTest() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        RuntimeService runtimeService = processEngine.getRuntimeService();
        String processInstanceId = "47d54bc6-08f3-11ee-af74-a85e455df905";
        runtimeService.createChangeActivityStateBuilder().processInstanceId(processInstanceId)
                .moveActivityIdsToSingleActivityId(Lists.list("task2_signal", "task3_signal"),"task1").changeState();
    }

    @Test
    public void skipTaskTest() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        DynamicBpmnService dynamicBpmnService = processEngine.getDynamicBpmnService();

        String processDefinitionId = "flowable-0612-01:1:38b3c121-08d3-11ee-a40b-a85e455df905";
        ObjectNode infoNode = dynamicBpmnService.enableSkipExpression();
        dynamicBpmnService.changeSkipExpression("task5", "${skip}", infoNode);
        dynamicBpmnService.saveProcessDefinitionInfo(processDefinitionId, infoNode);
    }

    @Test
    public void executeJobTest() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        ManagementService managementService = processEngine.getManagementService();
        managementService.executeJob("c1afa4a6-0e50-11ee-8538-a85e455df905");
    }

    @Test
    public void injectParallelUserTaskTest() {
        String processInstanceId = "d25384cf-0b4d-11ee-a728-a85e455df905";
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();

        DynamicBpmnService dynamicBpmnService = processEngine.getDynamicBpmnService();
        DynamicUserTaskBuilder dynamicUserTaskBuilder = new DynamicUserTaskBuilder();
        DynamicUserTaskBuilder taskBuilder = new DynamicUserTaskBuilder();
        taskBuilder.id(UUID.randomUUID().toString())
                .name("dTask")
                .assignee("kermit");
        dynamicBpmnService.injectUserTaskInProcessInstance(processInstanceId, dynamicUserTaskBuilder);
    }

    private void completeTask(String taskId, String processInstanceId, String message) {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService taskService = processEngine.getTaskService();
        Comment comment = taskService.addComment(taskId, processInstanceId, message);
        logger.info("completeTaskTest >> {}", comment);
        Evection evection = new Evection();
        evection.setNum(4d);
        Map<String, Object> map = new HashMap<>();
        map.put("evection", evection);
        taskService.complete(taskId, map);
        logger.info("complete >> {}", taskId);
    }

    /**
     * 启动流程
     */
    @Test
    public void startTest() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Evection evection = new Evection();
        evection.setNum(4d);
        Map<String, Object> map = new HashMap<>();
        map.put("evection", evection);
        map.put("me", "me-"+System.nanoTime());
        map.put("manager", "zsan");
        map.put("manager2", "lsi");
        map.put("manager3", "wwu");
        map.put("manager4", "wer");
        map.put("skip", false);
        map.put("userList", Arrays.asList("张三","李四","王二狗","赵天"));

        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();

        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESSID, map);
        stopWatch.stop();

        logger.info("流程实例名称：" + processInstance.getName());
        logger.info("流程定义ID：{}, take time {}", processInstance.getProcessInstanceId(), stopWatch.getTotalTimeSeconds());
    }

}
