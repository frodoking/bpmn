package com.bpmn.flowable;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngines;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author frodoking
 * @ClassName: FlowableController
 * @date 2023/6/15
 */
@Controller("/flowable")
public class FlowableController {

    private final Logger logger = LoggerFactory.getLogger(FlowableController.class);

    @GetMapping("/startAndComplete/{processDefinitionId}")
    public String startAndComplete(@PathVariable("processDefinitionId") String processDefinitionId) {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();

        RuntimeService runtimeService = processEngine.getRuntimeService();
        Evection evection = new Evection();
        evection.setNum(4d);
        Map<String, Object> map = new HashMap<>();
        map.put("evection", evection);
        map.put("me", "me-" + System.nanoTime());
        map.put("manager", "zsan");
        map.put("manager2", "lsi");
        map.put("manager3", "wwu");
        map.put("manager4", "wer");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionId, map);

        String processInstanceId = processInstance.getProcessInstanceId();

        TaskService taskService = processEngine.getTaskService();
        for (; ; ) {
            Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
            if (task != null) {
                taskService.complete(task.getId());
                logger.info("processInstanceId {}, complete task >> {}", processInstanceId, task.getName());
            } else {
                break;
            }
        }
        return processInstanceId;
    }
}
