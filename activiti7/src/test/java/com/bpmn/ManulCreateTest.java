package com.bpmn;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minidev.json.JSONUtil;
import org.activiti.api.task.model.payloads.AssignTaskPayload;
import org.activiti.api.task.runtime.TaskRuntime;
import org.activiti.bpmn.BpmnAutoLayout;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.*;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.bpmn.behavior.MultiInstanceActivityBehavior;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ExecutionQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.TaskQuery;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * link: https://blog.csdn.net/zhy0414/article/details/126466216
 * @author frodoking
 * @ClassName: ManulCreateTest
 * @date 2023/5/24
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class ManulCreateTest {

    private final Logger logger = LoggerFactory.getLogger(ManulCreateTest.class);

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final String PROCESSID = "evection-generate-0602-01";
    private static final String PROCESSNAME = "出差申请单-动态生成-0602-01";

    @Autowired
    private TaskService taskService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;

    /**
     * 创建流程定义及部署
     */
    @Test
    public void generateProcessTest() {
        logger.info("--------- start ---------");
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

        new BpmnAutoLayout(bpmnModel).execute();

        // 部署流程
        Deployment deploy = repositoryService.createDeployment().addBpmnModel(PROCESSID + ".bpmn", bpmnModel).name(PROCESSNAME).deploy();

        logger.info("流程名称：" + deploy.getName());
        logger.info("流程定义ID：" + deploy.getId());
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

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESSID, map);

        logger.info("流程实例名称：" + processInstance.getName());
        logger.info("流程定义ID：" + processInstance.getProcessDefinitionId());
    }

    @Test
    public void findBpmnTest() {
        String processDefinitionId = "evection-generate2:2:8f464302-fdf3-11ed-ae5b-a85e455df905";
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        List<Process> processes = bpmnModel.getProcesses();
        for (Process process: processes) {
            logger.info("process >> {}", process.getId());
        }
    }

    @Test
    public void setTaskAssigneeTest() {
        AssignTaskPayload assignTaskPayload = new AssignTaskPayload();
        assignTaskPayload.setAssignee("zsan");
        assignTaskPayload.setTaskId("d662c284-fa1f-11ed-98c2-a85e455df905");
        taskService.setAssignee(assignTaskPayload.getTaskId(), assignTaskPayload.getAssignee());
    }

    @Test
    public void signalEventReceivedTest() {
        String processInstanceId = "26bef6a7-fdf4-11ed-b421-a85e455df905";
        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId)
                .signalEventSubscriptionName("技术经理Signal")
                .singleResult();
        runtimeService.signalEventReceived("技术经理Signal", execution.getId());
    }

    @Test
    public void triggerEventTest() {
        String processInstanceId = "697a15b4-00f5-11ee-a2a0-a85e455df905";
        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId)
                .signalEventSubscriptionName("resumeFlow")
                .singleResult();
        runtimeService.trigger(execution.getId());
    }

    @Test
    public void messageEventReceivedTest() {
        String processInstanceId = "697a15b4-00f5-11ee-a2a0-a85e455df905";
        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId)
                .messageEventSubscriptionName("人事经理MsgSignal")
                .singleResult();
        runtimeService.messageEventReceived("人事经理MsgSignal", execution.getId());
    }

    @Test
    public void jumpTaskTest() {
        String taskId = "4e4ab86c-faa9-11ed-82be-a85e455df905";
        String targetTaskKey = "4e4ab86c-faa9-11ed-82be-a85e455df905";
        org.activiti.engine.task.Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            //确定流程是否存在和是否在流转
            throw new RuntimeException("该任务不存在，无法执行该操作");
        }

        //获得当前流程的活动ID
        ExecutionQuery executionQuery = runtimeService.createExecutionQuery();
        Execution execution = executionQuery.executionId(task.getExecutionId()).singleResult();
        String activityId = execution.getActivityId();


        //获取当前节点和目标节点的节点信息
        FlowNode currentFlowNode = getFlowNode(task.getProcessDefinitionId(), activityId);
        FlowNode targetFlowNode = getFlowNode(task.getProcessDefinitionId(), targetTaskKey);

        if (targetFlowNode == null) {
            throw new RuntimeException("目标节点不存在，无法执行该操作");
        }
        if (currentFlowNode.getBehavior() instanceof MultiInstanceActivityBehavior) {
            throw new RuntimeException("当前节点为会签节点不支持该操作");
        }

        //获取当前节点的向下流转信息并备份
        List<SequenceFlow> oldFlowNodeOutgoingFlows = currentFlowNode.getOutgoingFlows();

        //清空当前节点流转走向,若清空的话流程图的获取可能会出现流转线消失的情况
        //创建新的流程走向并把当前节点执行目标节点
        SequenceFlow sequenceFlow = new SequenceFlow();
        sequenceFlow.setId("newTempSequenceId");
        sequenceFlow.setSourceFlowElement(currentFlowNode);
        sequenceFlow.setTargetFlowElement(targetFlowNode);
        List<SequenceFlow> newSequenceFlows = new ArrayList<>();
        newSequenceFlows.add(sequenceFlow);
        currentFlowNode.setOutgoingFlows(newSequenceFlows);
        taskService.complete(task.getId());
        taskService.deleteTask(task.getId(), false);
        //最后恢复原来的流转走向
        currentFlowNode.setOutgoingFlows(oldFlowNodeOutgoingFlows);
    }

    /**
     * 获取流节点
     *
     * @param processDefinitionId 流程定义id
     * @param activityId          activityId
     * @return org.activiti.bpmn.model.FlowNode
     */
    private FlowNode getFlowNode(String processDefinitionId, String activityId) {
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        return (FlowNode) bpmnModel.getMainProcess().getFlowElement(activityId);
    }

    @Test
    public void rollbackTaskTest() {
        String taskId = "4e4ab86c-faa9-11ed-82be-a85e455df905";
        String processInstanceId = "cad3f67c-faca-11ed-9d59-a85e455df905";
        org.activiti.engine.task.Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).list().get(0);

        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        HistoryService historyService = processEngine.getHistoryService();
        RepositoryService repositoryService = processEngine.getRepositoryService();
        TaskService taskService = processEngine.getTaskService();

        //  获取所有历史任务（按创建时间降序）
        List<HistoricTaskInstance> hisTaskList = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByTaskCreateTime()
                .desc()
                .list();

        List<HistoricActivityInstance> hisActivityList = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId).list();

        if (CollectionUtils.isEmpty(hisTaskList) || hisTaskList.size() < 2) {
            return;
        }

        //  当前任务
        HistoricTaskInstance currentTask = hisTaskList.get(0);
        //  前一个任务
        HistoricTaskInstance lastTask = hisTaskList.get(1);
        //  当前活动
        HistoricActivityInstance currentActivity = hisActivityList.stream().filter(e -> currentTask.getId().equals(e.getTaskId())).collect(Collectors.toList()).get(0);
        //  前一个活动
        HistoricActivityInstance lastActivity = hisActivityList.stream().filter(e -> lastTask.getId().equals(e.getTaskId())).collect(Collectors.toList()).get(0);

        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());

        //  获取前一个活动节点
        FlowNode lastFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(lastActivity.getActivityId());
        //  获取当前活动节点
        FlowNode currentFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(currentActivity.getActivityId());

        //  临时保存当前活动的原始方向
        List<SequenceFlow> originalSequenceFlowList = new ArrayList<>();
        originalSequenceFlowList.addAll(currentFlowNode.getOutgoingFlows());
        //  清理活动方向
        currentFlowNode.getOutgoingFlows().clear();

        //  建立新方向
        SequenceFlow newSequenceFlow = new SequenceFlow();
        newSequenceFlow.setId("newSequenceFlowId");
        newSequenceFlow.setSourceFlowElement(currentFlowNode);
        newSequenceFlow.setTargetFlowElement(lastFlowNode);
        List<SequenceFlow> newSequenceFlowList = new ArrayList<>();
        newSequenceFlowList.add(newSequenceFlow);
        //  当前节点指向新的方向
        currentFlowNode.setOutgoingFlows(newSequenceFlowList);

        //  完成当前任务
        taskService.complete(task.getId());

        //  重新查询当前任务
        org.activiti.engine.task.Task nextTask = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        if (null != nextTask) {
            taskService.setAssignee(nextTask.getId(), lastTask.getAssignee());
        }

        //  恢复原始方向
        currentFlowNode.setOutgoingFlows(originalSequenceFlowList);
    }


    /**
     * 会签的种类：
     *
     * 按数量通过： 达到一定数量的通过表决后，会签通过。
     * 按比例通过： 达到一定比例的通过表决后，会签通过。
     * 一票否决： 只要有一个表决时否定的，会签通过。
     * 一票通过： 只要有一个表决通过的，会签通过。
     *
     * 每个实例有以下变量：
     *
     * Sequential: true串行；false并行
     * nrOfInstances: 实例总数
     * nrOfActiveInstances: 当前激活的（未完成的）实例总数。 如果串行执行，则改值永远是1
     *
     * nrOfCompletedInstances: 已完成的实例总数
     * 条件${nrOfInstances == nrOfCompletedInstances}表示所有人员审批完成后会签结束。
     *
     * 条件${ nrOfCompletedInstances == 1}表示一个人完成审批，该会签就结束。
     */
    @Test
    public void multiInstanceTest() {

    }

    @Test
    public void completeTaskTest() {
        String taskId = "698a907e-00f5-11ee-a2a0-a85e455df905";
        String processInstanceId = "697a15b4-00f5-11ee-a2a0-a85e455df905";
        String message = "Task1完成00000";

        completeTask(taskId, processInstanceId, message);
    }

    private void completeTask(String taskId, String processInstanceId, String message) {
        Comment comment = taskService.addComment(taskId, processInstanceId, message);
        logger.info("completeTaskTest >> {}", comment);
        taskService.complete(taskId);
        logger.info("complete >> {}", taskId);
    }

    /**
     * 生成 bpmn 文件
     */
    @Test
    public void generateBpmnTest() throws IOException {
        // 生成 bpmn 文件
        InputStream inputStream = repositoryService.getResourceAsStream("dfaee139-fdf8-11ed-b24a-a85e455df905", PROCESSID + ".bpmn");
        FileUtils.copyInputStreamToFile(inputStream, new File("src/main/resources/bpmn/" + PROCESSID + ".bpmn"));
    }

    /**
     * 生成 svg 图片文件
     * activiti-image-generator 7 的版本生成 png 的图片无法打开，可以生成 svg 图片，或者采用 5.19.0.2 版本的 activiti-image-generator 生成 png 图片
     */
    @Test
    public void generateSVGTest() throws IOException {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(PROCESSID).singleResult();

        BpmnModel bpmnModel = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());

        if(bpmnModel != null && bpmnModel.getLocationMap().size() > 0){
            DefaultProcessDiagramGenerator ge = new DefaultProcessDiagramGenerator();

            InputStream inputStream = ge.generateDiagram(bpmnModel, runtimeService.getActiveActivityIds(processInstance.getId()), new ArrayList<>(), "宋体", "宋体", null, false);

            FileUtils.copyInputStreamToFile(inputStream, new File("src/main/resources/bpmn/" + PROCESSID + ".svg"));
        } else {
            logger.info("bpmnModel 为空！");
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
