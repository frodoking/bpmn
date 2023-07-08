package com.bpmn.flowable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.*;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngines;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.validation.ProcessValidator;
import org.flowable.validation.ProcessValidatorFactory;
import org.flowable.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author frodoking
 * @ClassName: BPMNBuilder
 * @date 2023/6/15
 */
public class BPMNBuilder {
    private final Logger logger = LoggerFactory.getLogger(BPMNBuilder.class);

    public void build(String id, String name) {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();

        RepositoryService repositoryService = processEngine.getRepositoryService();
        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.setTargetNamespace("http://activiti.org/test");
        Process process = new Process();
        bpmnModel.addProcess(process);
        process.setId(id);
        process.setName(name);

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
        Deployment deploy = repositoryService.createDeployment().addBpmnModel(id + ".bpmn20.xml", bpmnModel).name(name).deploy();

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deploy.getId()).singleResult();

        logger.info("流程名称：" + processDefinition.getName());
        logger.info("流程定义ID：" + processDefinition.getId());

        exportBpmn(processDefinition.getId());
    }

    public String buildSimple(String id, String name) {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();

        RepositoryService repositoryService = processEngine.getRepositoryService();
        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.setTargetNamespace("http://activiti.org/test");
        Process process = new Process();
        bpmnModel.addProcess(process);
        process.setId(id);
        process.setName(name);

        // 添加开始节点
        process.addFlowElement(generateStartEvent());
        // 添加用户节点
        process.addFlowElement(generateUserTask("task1", "填写申请单", "${me}", "robot01", "activitiTeam"));
        process.addFlowElement(generateUserTask("task2", "技术经理", "${manager}", "robot02", ""));
        process.addFlowElement(generateUserTask("task3", "人事经理", "${manager2}", "robot03", ""));
        // 添加结束节点
        process.addFlowElement(generateEndEvent());

        // 设置连接线
        process.addFlowElement(generateSequenceFlow("startEvent", "task1", "", ""));
        process.addFlowElement(generateSequenceFlow("task1", "task2", "", ""));
        process.addFlowElement(generateSequenceFlow("task2", "task3", "", ""));
        process.addFlowElement(generateSequenceFlow("task3", "endEvent", "", ""));

        // 部署流程
        Deployment deploy = repositoryService.createDeployment().addBpmnModel(id + ".bpmn20.xml", bpmnModel).name(name).deploy();

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deploy.getId()).singleResult();

        logger.info("流程名称：" + processDefinition.getName());
        logger.info("流程定义ID：" + processDefinition.getId());

        exportBpmn(processDefinition.getId());
        return processDefinition.getId();
    }

    /**
     * MultiInstanceActivityBehavior
     */
    public String buildMultiInstance(String id, String name) {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();

        RepositoryService repositoryService = processEngine.getRepositoryService();
        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.setTargetNamespace("http://activiti.org/test");
        Process process = new Process();
        bpmnModel.addProcess(process);
        process.setId(id);
        process.setName(name);

        // 添加开始节点
        process.addFlowElement(generateStartEvent());
        // 添加用户节点
        UserTask userTask = generateUserTask("task1", "填写申请单", "${me}", "robot01", "activitiTeam");
        userTask.setAssignee("${user}");
        MultiInstanceLoopCharacteristics loopCharacteristics = new MultiInstanceLoopCharacteristics();
        loopCharacteristics.setSequential(false);
        loopCharacteristics.setInputDataItem("${userList}");
        loopCharacteristics.setElementVariable("user");
        loopCharacteristics.setCompletionCondition("${nrOfCompletedInstances/nrOfInstances >= 1}");
        loopCharacteristics.setLoopCardinality("3");
        userTask.setLoopCharacteristics(loopCharacteristics);
        process.addFlowElement(userTask);
        process.addFlowElement(generateUserTask("task2", "技术经理", "${manager}", "robot02", ""));
        process.addFlowElement(generateUserTask("task3", "人事经理", "${manager2}", "robot03", ""));
        // 添加结束节点
        process.addFlowElement(generateEndEvent());

        // 设置连接线
        process.addFlowElement(generateSequenceFlow("startEvent", "task1", "", ""));
        process.addFlowElement(generateSequenceFlow("task1", "task2", "", ""));
        process.addFlowElement(generateSequenceFlow("task2", "task3", "", ""));
        process.addFlowElement(generateSequenceFlow("task3", "endEvent", "", ""));

        ProcessValidator processValidator=new ProcessValidatorFactory().createDefaultProcessValidator();
        List<ValidationError> validationErrorList=processValidator.validate(bpmnModel);
        if (validationErrorList.size()>0){
            throw new RuntimeException("流程有误，请检查后重试");
        }

        validateBpmn(bpmnModel);

        // 部署流程
        Deployment deploy = repositoryService.createDeployment().addBpmnModel(id + ".bpmn20.xml", bpmnModel).name(name).deploy();

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deploy.getId()).singleResult();

        logger.info("流程名称：" + processDefinition.getName());
        logger.info("流程定义ID：" + processDefinition.getId());

        exportBpmn(processDefinition.getId());
        return processDefinition.getId();
    }

    /**
     * 校验bpmModel
     */
    private void validateBpmn(BpmnModel bpmnModel) {
        //校验bpmModel
        ProcessValidator processValidator=new ProcessValidatorFactory().createDefaultProcessValidator();
        List<ValidationError> validationErrorList=processValidator.validate(bpmnModel);
        if (validationErrorList.size()>0){
            throw new RuntimeException("流程有误，请检查后重试");
        }
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
        if (StringUtils.isNotEmpty(conditionExpression)) {
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

    public void exportBpmn(String processDefinitionId) {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        RepositoryService repositoryService = processEngine.getRepositoryService();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);

        BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();

        try {
            String path = ResourceUtils.getFile("classpath:bpmn").getAbsolutePath();
            File file = new File(path + bpmnModel.getMainProcess().getName() + ".bpmn20.xml");
            FileUtils.copyInputStreamToFile(new ByteArrayInputStream(bpmnXMLConverter.convertToXML(bpmnModel)), file);
            logger.info("exportBpmn {}", file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
