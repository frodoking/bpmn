package com.bpmn;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.ProcessDefinitionMeta;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.query.Pageable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class ProcessTest {

    private final Logger logger = LoggerFactory.getLogger(ProcessTest.class);
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Autowired
    private ProcessRuntime processRuntime;

    @Test // 查看流程
    @WithUserDetails(value = "admin", userDetailsServiceBeanName = "userDetailsService")
    public void getProcessDefinitionMeta() {
        ProcessDefinitionMeta processDefinitionMeta = processRuntime.processDefinitionMeta("Process_1:4:e3cb3117-3bbf-11ed-9380-f6335fb4fb12");
        logger.info("流程定义：{}" , gson.toJson(processDefinitionMeta));
    }

    @Test // 查看流程
    @WithUserDetails(value = "admin", userDetailsServiceBeanName = "userDetailsService")
    public void getProcessDefinitions() {
        Page<ProcessDefinition> processDefinitionPage = processRuntime.processDefinitions(Pageable.of(0, 10));
        System.err.println("已部署的流程个数：" + processDefinitionPage.getTotalItems());
        for (Object obj : processDefinitionPage.getContent()) {
            logger.info("流程定义：{}" , gson.toJson(obj));
        }
    }

    @Test // 启动流程
    @WithUserDetails(value = "admin", userDetailsServiceBeanName = "userDetailsService")
    public void startInstance() {
        Content content = pickRandomString();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yy HH:mm:ss");
        logger.info("> Starting process to process content: " + content + " at " + formatter.format(new Date()));
        ProcessInstance processInstance = processRuntime.start(ProcessPayloadBuilder
                .start()
                .withProcessDefinitionKey("Process_1")
                .withName("Processing Content: " + content)
                .withVariable("content", content)
                .build());
        logger.info(">>> Created Process Instance: " + processInstance);
    }

    @Test // 获取启动的流程
    @WithUserDetails(value = "admin", userDetailsServiceBeanName = "userDetailsService")
    public void getInstance() {
        Page<ProcessInstance> processInstancePage = processRuntime.processInstances(Pageable.of(0, 10));
        logger.info("已启动的流程个数：{}", processInstancePage.getTotalItems());
        for (Object obj : processInstancePage.getContent()) {
            logger.info("流程实例：{}" , gson.toJson(obj));
        }
    }

    private Content pickRandomString() {
        String[] texts = {"hello from london", "Hi there from activiti!", "all good news over here.", "I've tweeted about activiti today.",
                "other boring projects.", "activiti cloud - Cloud Native Java BPM"};
        return new Content(texts[new Random().nextInt(texts.length)],false,null);
    }

}