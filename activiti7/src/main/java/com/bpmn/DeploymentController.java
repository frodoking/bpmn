package com.bpmn;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@RestController
@RequestMapping("/deploy")
public class DeploymentController {

    @Autowired
    private RepositoryService repositoryService;

    /**
     * 部署
     * @param resourcePath  ZIP压缩包文件
     * @param processName   流程名称
     * @return
     */
    @PostMapping("/upload")
    public String upload(@RequestParam("resourcePath") String resourcePath, @RequestParam("processName") String processName) {
        Deployment deployment = repositoryService.createDeployment().addClasspathResource(resourcePath).name(processName).deploy();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        return processDefinition.getId();
    }

    /**
     * 查看流程图
     * @param deploymentId  部署ID
     * @param resourceName  图片名称
     */
    @GetMapping("/getDiagram")
    public void getDiagram(@RequestParam("deploymentId") String deploymentId, @RequestParam("resourceName") String resourceName, HttpServletResponse response) {
        InputStream inputStream = repositoryService.getResourceAsStream(deploymentId, resourceName);
        try {
            IOUtils.copy(inputStream, response.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }
}
