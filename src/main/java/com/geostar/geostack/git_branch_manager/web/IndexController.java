package com.geostar.geostack.git_branch_manager.web;

import com.geostar.geostack.git_branch_manager.pojo.GitProject;
import com.geostar.geostack.git_branch_manager.service.IGitRepositoryService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/")
public class IndexController {

    @Resource
    private IGitRepositoryService gitRepositoryService;

    /**
     * 首页展示
     *
     * @param model
     * @return
     */
    @RequestMapping("/")
    public String index(Model model) {
        List<GitProject> projects = gitRepositoryService.getAllGitProject();
        for (GitProject gitProject : projects) {
            try {
                gitRepositoryService.updateGitProjectInfo(gitProject);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        }
        List<String> branchIntersect = gitRepositoryService.getBranchIntersect(projects);
        model.addAttribute("projects", projects);
        model.addAttribute("branchIntersect", branchIntersect);
        return "index";
    }

    /**
     * 克隆或者拉取最新代码
     *
     * @param model
     * @return
     */
    @RequestMapping({"cloneOrPull", "/cloneOrPull/{name}"})
    public String cloneOrPull(Model model, @PathVariable(value = "name", required = false) String inputProjectName) {
        List<GitProject> projects = gitRepositoryService.getAllGitProject();
        for (GitProject gitProject : projects) {
            try {
                if (inputProjectName != null) {
                    if (inputProjectName.equals(gitProject.getName())) {
                        gitRepositoryService.cloneOrPull(gitProject);
                    }
                } else {
                    gitRepositoryService.cloneOrPull(gitProject);
                }
                gitRepositoryService.updateGitProjectInfo(gitProject);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        }
        List<String> branchIntersect = gitRepositoryService.getBranchIntersect(projects);
        model.addAttribute("projects", projects);
        model.addAttribute("branchIntersect", branchIntersect);
        return "index";
    }

    /**
     * 创建分支
     *
     * @param model
     * @param branchName
     * @return
     */
    @RequestMapping({"/createBranch/{branchName}", "/createBranch/{branchName}/{projectName}"})
    public String createBranch(Model model, @PathVariable("branchName") String branchName,
                               @PathVariable(value = "projectName", required = false) String inputProjectName) {
        List<GitProject> projects = gitRepositoryService.getAllGitProject();
        for (GitProject gitProject : projects) {
            try {
                if (inputProjectName != null) {
                    if (inputProjectName.equals(gitProject.getName())) {
                        gitRepositoryService.createBranch(gitProject, branchName);
                    }
                } else {
                    gitRepositoryService.createBranch(gitProject, branchName);
                }
                gitRepositoryService.updateGitProjectInfo(gitProject);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
            gitProject.getBranchList().add(branchName);
        }
        List<String> branchIntersect = gitRepositoryService.getBranchIntersect(projects);
        model.addAttribute("projects", projects);
        model.addAttribute("branchIntersect", branchIntersect);
        return "index";
    }

    /**
     * 切换分支
     *
     * @param model
     * @param branchName
     * @return
     */
    @RequestMapping({"/switchBranch/{branchName}", "/switchBranch/{branchName}/{projectName}"})
    public String switchBranchAll(Model model, @PathVariable("branchName") String branchName,
                                  @PathVariable(value = "projectName", required = false) String inputProjectName) {

        List<GitProject> projects = gitRepositoryService.getAllGitProject();
        for (GitProject gitProject : projects) {
            try {
                if (inputProjectName != null) {
                    if (inputProjectName.equals(gitProject.getName())) {
                        gitRepositoryService.switchBranch(gitProject, branchName);
                    }
                } else {
                    gitRepositoryService.switchBranch(gitProject, branchName);
                }
                gitRepositoryService.updateGitProjectInfo(gitProject);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        }
        List<String> branchIntersect = gitRepositoryService.getBranchIntersect(projects);
        model.addAttribute("projects", projects);
        model.addAttribute("branchIntersect", branchIntersect);
        return "index";
    }

    /**
     * 推送代码
     *
     * @param model
     * @return
     */
    @RequestMapping({"/push", "/push/{projectName}"})
    public String push(Model model, @PathVariable(value = "projectName", required = false) String inputProjectName) {

        List<GitProject> projects = gitRepositoryService.getAllGitProject();
        for (GitProject gitProject : projects) {
            try {
                if (inputProjectName != null) {
                    if (inputProjectName.equals(gitProject.getName())) {
                        gitRepositoryService.push(gitProject);
                    }
                } else {
                    gitRepositoryService.push(gitProject);
                }
                gitRepositoryService.updateGitProjectInfo(gitProject);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        }
        List<String> branchIntersect = gitRepositoryService.getBranchIntersect(projects);
        model.addAttribute("projects", projects);
        model.addAttribute("branchIntersect", branchIntersect);
        return "index";
    }

    /**
     * 删除项目的当前分支，不允许删除master和develop
     *
     * @param model
     * @param inputProjectName
     * @return
     */
    @RequestMapping({"/deleteBranch", "/deleteBranch/{projectName}"})
    public String deleteBranch(Model model, @PathVariable(value = "projectName", required = false) String inputProjectName) {
        List<GitProject> projects = gitRepositoryService.getAllGitProject();
        for (GitProject gitProject : projects) {
            try {
                if (inputProjectName != null) {
                    if (inputProjectName.equals(gitProject.getName())) {
                        gitRepositoryService.deleteBranch(gitProject);
                    }
                } else {
                    gitRepositoryService.deleteBranch(gitProject);
                }
                gitRepositoryService.updateGitProjectInfo(gitProject);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        }
        List<String> branchIntersect = gitRepositoryService.getBranchIntersect(projects);
        model.addAttribute("projects", projects);
        model.addAttribute("branchIntersect", branchIntersect);
        return "index";
    }

}