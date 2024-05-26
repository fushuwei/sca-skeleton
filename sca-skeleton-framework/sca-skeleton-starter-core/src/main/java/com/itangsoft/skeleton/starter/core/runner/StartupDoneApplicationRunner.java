package com.itangsoft.skeleton.starter.core.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 服务启动完毕运行程序
 *
 * @author fushuwei
 */
@Component
public class StartupDoneApplicationRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(StartupDoneApplicationRunner.class);

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("(♥◠‿◠)ﾉﾞ   服务启动成功   ლ(´ڡ`ლ)ﾞ");
    }
}
