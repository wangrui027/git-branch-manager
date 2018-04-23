package com.geostar.geostack.git_branch_manager.config;

import com.geostar.geostack.git_branch_manager.pojo.GitProject;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@ConfigurationProperties
public class GitRepositoryConfig {

    private String workHome;

    private final List<GitProject> gitProjects = new ArrayList<>();

    public String getWorkHome() {
        return workHome;
    }

    public List<GitProject> getProjects() {
        return gitProjects;
    }

    public void setWorkHome(String workHome) {
        this.workHome = workHome;
    }

    public void setProjects(List<String> projects) {
        gitProjects.clear();
        String[] remoteUrls = new String[projects.size()];
        projects.toArray(remoteUrls);
        Arrays.sort(remoteUrls);
        for (String url : remoteUrls) {
            for (String remoteUrl : projects) {
                if(url.equals(remoteUrl)){
                    GitProject gitProject = new GitProject();
                    String projectName = remoteUrl.substring(remoteUrl.lastIndexOf("/") + 1, remoteUrl.length() - ".git".length());
                    gitProject.setName(projectName);
                    gitProject.setRemoteUrl(remoteUrl);
                    gitProjects.add(gitProject);
                    break;
                }
            }
        }
    }

}
