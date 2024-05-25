package com.itangsoft.skeleton.starter.core.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * 项目启动完毕输出相关日志
 *
 * @author fushuwei
 */
public class BannerApplicationRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(BannerApplicationRunner.class);

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("\n\n服务启动完毕!\n");
    }
}
