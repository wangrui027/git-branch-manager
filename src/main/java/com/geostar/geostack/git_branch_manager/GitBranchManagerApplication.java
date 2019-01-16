package com.geostar.geostack.git_branch_manager;

import com.geostar.geostack.git_branch_manager.common.SpringUtils;
import com.geostar.geostack.git_branch_manager.config.MyConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class GitBranchManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitBranchManagerApplication.class, args);
        try {
            MyConfig config = SpringUtils.getBean(MyConfig.class);
            Runtime.getRuntime().exec("cmd /c start http://localhost:" + config.getPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
