package com.kee.model.test.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kee.common.reptiles.config.WebDriverAcquisitionConfig;
import com.kee.common.reptiles.dev.concrete.ChromeDevTools;
import com.kee.common.reptiles.utils.WebDriverAcquisitionUtil;
import com.kee.model.test.domain.Test;
import com.kee.model.test.mapper.TestMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.SneakyThrows;
import org.activiti.engine.HistoryService;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.Task;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description : Object
 * @author: zeng.maosen
 */
@RestController
@RequestMapping
@Api(tags = {"测试模块"})
public class TestController {

    @Resource
    private WebDriverAcquisitionConfig config;

    @Resource
    private TaskService taskService;

    @Resource
    private HistoryService historyService;

    @Resource
    private TestMapper testMapper;

    @SneakyThrows
    @Override
    @GetMapping("/test")
    @ApiOperation(value = "测试")
    public String toString() {
        System.out.println(config.getAcquisition().getLocalPath());
        WebDriver webDriver = WebDriverAcquisitionUtil.chromeDriver(config.getAcquisition().getLocalPath());
        ChromeDriver chromeDriver = (ChromeDriver)webDriver;
        ChromeDevTools chromeDevTools = new ChromeDevTools(chromeDriver);
        chromeDevTools.listenAllRequest();
        chromeDriver.get("https://www.baidu.com/");
        Thread.sleep(1000);
        chromeDevTools.disable();
        System.out.println(testMapper.selectList(Wrappers.lambdaQuery(Test.class)));
        return chromeDevTools.getRequestList().get(0).getUrl();
    }


    @GetMapping("/task")
    public List<Task> task(){
        return taskService.createTaskQuery().list();
    }


}