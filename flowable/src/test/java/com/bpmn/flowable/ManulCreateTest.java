package com.bpmn.flowable;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.*;
import org.flowable.engine.*;
import org.flowable.bpmn.model.Process;
import  org.flowable.bpmn.*;

import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Comment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author frodoking
 * @ClassName: ManulCreateTest
 * @date 2023/6/12
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class ManulCreateTest {

    private final Logger logger = LoggerFactory.getLogger(ManulCreateTest.class);


    private static final String PROCESSID = "flowable-0612-01";
    private static final String PROCESSNAME = "出差申请单-动态生成-0612-01";

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

        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.setTargetNamespace("http://activiti.org/test");
        Process process = new Process();
        bpmnModel.addProcess(process);
        process.setId(PROCESSID);
        process.setName(PROCESSNAME);

        // 添加开始节点
        process.addFlowElement(generateStartEvent());

        // 添加用户节点
        process.addFlowElement(generateUserTask("task1", "填写申请单", "${me}", "robot01", "activitiTeam"));

        process.addFlowElement(generateIntermediateCatchEvent("task2_signal", "技术经理Signal", false));
        process.addFlowElement(generateUserTask("task2", "技术经理", "${manager}", "robot02", ""));

        process.addFlowElement(generateIntermediateCatchEvent("task3_signal", "人事经理MsgSignal", true));
        process.addFlowElement(generateUserTask("task3", "人事经理", "${manager2}", "robot03", ""));

        process.addFlowElement(generateIntermediateCatchEvent("task4_signal", "项目经理MsgSignal", true));
        process.addFlowElement(generateUserTask("task4", "项目经理", "${manager3}", "robot04", ""));

        process.addFlowElement(generateUserTask("task5", "总经理", "${manager4}", "robot05", ""));

        // 添加包含网关
        process.addFlowElement(generateInclusiveGateway("inclusiveGateway1"));
        process.addFlowElement(generateInclusiveGateway("inclusiveGateway2"));

        // 添加排他网关
        process.addFlowElement(generateExclusiveGateway("exclusiveGateway1"));

        // 添加结束节点
        process.addFlowElement(generateEndEvent());

        // 设置连接线
        process.addFlowElement(generateSequenceFlow("startEvent", "task1", "", ""));

        process.addFlowElement(generateSequenceFlow("task1", "inclusiveGateway1", "", ""));

        process.addFlowElement(generateSequenceFlow("inclusiveGateway1", "task2_signal", "大于等于3天", "${evection.num>=3}"));
        process.addFlowElement(generateSequenceFlow("task2_signal", "task2", "", ""));

        process.addFlowElement(generateSequenceFlow("inclusiveGateway1", "task3_signal", "", ""));
        process.addFlowElement(generateSequenceFlow("task3_signal", "task3", "", ""));

        process.addFlowElement(generateSequenceFlow("inclusiveGateway1", "task4_signal", "小于3天", "${evection.num<3}"));
        process.addFlowElement(generateSequenceFlow("task4_signal", "task4", "", ""));

        process.addFlowElement(generateSequenceFlow("task2", "inclusiveGateway2", "", ""));
        process.addFlowElement(generateSequenceFlow("task3", "inclusiveGateway2", "", ""));
        process.addFlowElement(generateSequenceFlow("task4", "inclusiveGateway2", "", ""));

        process.addFlowElement(generateSequenceFlow("inclusiveGateway2", "exclusiveGateway1", "", ""));
        process.addFlowElement(generateSequenceFlow("exclusiveGateway1", "task5", "大于等于3天", "${evection.num>=3}"));
        process.addFlowElement(generateSequenceFlow("exclusiveGateway1", "endEvent", "小于3天", "${evection.num<3}"));
        process.addFlowElement(generateSequenceFlow("task5", "endEvent", "", ""));

        // 部署流程
        Deployment deploy = repositoryService.createDeployment().addBpmnModel(PROCESSID + ".bpmn20.xml", bpmnModel).name(PROCESSNAME).deploy();

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deploy.getId()).singleResult();

        logger.info("流程名称：" + processDefinition.getName());
        logger.info("流程定义ID：" + processDefinition.getId());

        exportBpmn(processDefinition.getId());
    }

    @Test
    public void exportBpmnTest() {
        String processDefinitionId = "flowable-0612-01:1:38b3c121-08d3-11ee-a40b-a85e455df905";
        exportBpmn(processDefinitionId);
    }

    public void exportBpmn(String processDefinitionId) {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        RepositoryService repositoryService = processEngine.getRepositoryService();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);

        BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();
        File file = new File("E:\\workspaces\\workspace_frodo_opensource\\bpmn\\flowable\\src\\main\\resources\\bpmn\\" + processDefinitionId.replace(":", "") + ".bpmn20.xml");

        try {
            FileUtils.copyInputStreamToFile(new ByteArrayInputStream(bpmnXMLConverter.convertToXML(bpmnModel)), file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void completeTaskTest() {
        String taskId = "024766f7-090e-11ee-b32e-8e117451b592";
        String processInstanceId = "47d54bc6-08f3-11ee-af74-a85e455df905";
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
        Evection evection = new Evection();
        evection.setNum(4d);
        Map<String, Object> map = new HashMap<>();
        map.put("evection", evection);
        map.put("me", "me-"+System.nanoTime());
        map.put("manager", "zsan");
        map.put("manager2", "lsi");
        map.put("manager3", "wwu");
        map.put("manager4", "wer");

        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();

        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESSID, map);

        logger.info("流程实例名称：" + processInstance.getName());
        logger.info("流程定义ID：" + processInstance.getProcessDefinitionId());
    }

    /**
     * 生成开始节点
     */
    private StartEvent generateStartEvent() {
        StartEvent startEvent = new StartEvent();
        startEvent.setId("startEvent");
        return startEvent;
    }

    /**
     * 生成任务节点
     * 不填的项传 ""
     */
    private UserTask generateUserTask(String id, String name, String assignee, String candidateUsers, String candidateGroups) {
        List<String> candidateUserList = new ArrayList<>();
        String[] users = candidateUsers.split(",");
        for (String user : users) {
            candidateUserList.add(user);
        }

        List<String> candidateGroupList = new ArrayList<>();
        String[] groups = candidateGroups.split(",");
        for (String group : groups) {
            candidateGroupList.add(group);
        }

        UserTask userTask = new UserTask();
        userTask.setId(id);
        userTask.setName(name);
        userTask.setAssignee(assignee);
        userTask.setCandidateUsers(candidateUserList);
        userTask.setCandidateGroups(candidateGroupList);
        userTask.setSkipExpression("${skip}");

        return userTask;
    }

    private IntermediateCatchEvent generateIntermediateCatchEvent(String id, String name, boolean msg) {
        IntermediateCatchEvent intermediateCatchEvent = new IntermediateCatchEvent();
        intermediateCatchEvent.setName(name);
        intermediateCatchEvent.setId(id);
        List<EventDefinition> eventDefinitions = new ArrayList<>();
        intermediateCatchEvent.setEventDefinitions(eventDefinitions);

        if (msg) {
            MessageEventDefinition messageEventDefinition = new MessageEventDefinition();
            messageEventDefinition.setMessageRef(name);
            eventDefinitions.add(messageEventDefinition);
        } else {
            SignalEventDefinition signalEventDefinition = new SignalEventDefinition();
            eventDefinitions.add(signalEventDefinition);
            signalEventDefinition.setSignalRef(name);
        }

        return intermediateCatchEvent;
    }

    /**
     * 创建包含网关
     */
    private InclusiveGateway generateInclusiveGateway(String id) {
        InclusiveGateway inclusiveGateway = new InclusiveGateway();
        inclusiveGateway.setId(id);
        return inclusiveGateway;
    }

    /**
     * 创建排他网关
     */
    private ExclusiveGateway generateExclusiveGateway(String id) {
        ExclusiveGateway exclusiveGateway = new ExclusiveGateway();
        exclusiveGateway.setId(id);
        return exclusiveGateway;
    }

    /**
     * 创建连接线
     */
    private SequenceFlow generateSequenceFlow(String from, String to, String name, String conditionExpression) {
        SequenceFlow sequenceFlow = new SequenceFlow();
        sequenceFlow.setSourceRef(from);
        sequenceFlow.setTargetRef(to);
        sequenceFlow.setName(name);
        if(StringUtils.isNotEmpty(conditionExpression)) {
            sequenceFlow.setConditionExpression(conditionExpression);
        }

        return sequenceFlow;
    }

    /**
     * 创建结束节点
     */
    private EndEvent generateEndEvent() {
        EndEvent endEvent = new EndEvent();
        endEvent.setId("endEvent");
        return endEvent;
    }
}
