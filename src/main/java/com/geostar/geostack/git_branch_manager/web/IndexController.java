package com.geostar.geostack.git_branch_manager.web;

import com.geostar.geostack.git_branch_manager.config.GitRepositoryConfig;
import com.geostar.geostack.git_branch_manager.pojo.GitProject;
import com.geostar.geostack.git_branch_manager.service.IGitRepositoryService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/")
public class IndexController {

    /**
     * 首页对应的模板名称
     */
    private static final String INDEX_HTML = "list";
    /**
     * 未提交文件view的URI路径前缀
     */
    private static final String UNTRACKED_FILE_VIEW_PATH_PREFIX = "/untrackedFileView/";
    @Resource
    private IGitRepositoryService gitRepositoryService;
    @Autowired
    private GitRepositoryConfig gitRepositoryConfig;

    /**
     * 首页展示
     *
     * @return
     */
    @RequestMapping("/")
    public String index(Model model) {
        list(model);
        return INDEX_HTML;
    }

    /**
     * 列表展示
     *
     * @param model
     * @return
     */
    @RequestMapping("/list")
    public String list(Model model) {
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
        modelBuild(model, projects);
        return "list";
    }

    /**
     * 克隆或者拉取最新代码
     *
     * @param model
     * @return
     */
    @RequestMapping({"cloneOrPull"})
    public String cloneOrPull(Model model) {
        List<GitProject> projects = gitRepositoryService.getAllGitProject();
        for (GitProject gitProject : projects) {
            try {
                gitRepositoryService.cloneOrPull(gitProject);
                gitRepositoryService.updateGitProjectInfo(gitProject);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        }
        modelBuild(model, projects);
        return INDEX_HTML;
    }

    /**
     * 创建分支
     *
     * @param model
     * @param branchName
     * @return
     */
    @RequestMapping({"/createBranch/{branchName}"})
    public String createBranch(Model model, @PathVariable("branchName") String branchName) {
        List<GitProject> projects = gitRepositoryService.getAllGitProject();
        for (GitProject gitProject : projects) {
            try {
                gitRepositoryService.createBranch(gitProject, branchName);
                gitRepositoryService.updateGitProjectInfo(gitProject);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
            gitProject.getBranchList().add(branchName);
        }
        modelBuild(model, projects);
        return INDEX_HTML;
    }

    /**
     * 切换分支
     *
     * @param model
     * @param branchName
     * @return
     */
    @RequestMapping({"/switchBranch/{branchName}"})
    public String switchBranchAll(Model model, @PathVariable("branchName") String branchName) {
        List<GitProject> projects = gitRepositoryService.getAllGitProject();
        for (GitProject gitProject : projects) {
            try {
                gitRepositoryService.switchBranch(gitProject, branchName);
                gitRepositoryService.updateGitProjectInfo(gitProject);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        }
        modelBuild(model, projects);
        return INDEX_HTML;
    }

    /**
     * 推送代码
     *
     * @param model
     * @return
     */
    @RequestMapping({"/push/{message}"})
    public String push(Model model, @PathVariable(value = "message") String inputMessage) {
        List<GitProject> projects = gitRepositoryService.getAllGitProject();
        for (GitProject gitProject : projects) {
            try {
                gitRepositoryService.push(gitProject, inputMessage);
                gitRepositoryService.updateGitProjectInfo(gitProject);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        }
        modelBuild(model, projects);
        return INDEX_HTML;
    }

    /**
     * 删除项目的当前分支，不允许删除master和develop
     *
     * @param model
     * @return
     */
    @RequestMapping({"/deleteBranch"})
    public String deleteBranch(Model model) {
        List<GitProject> projects = gitRepositoryService.getAllGitProject();
        for (GitProject gitProject : projects) {
            try {
                gitRepositoryService.deleteBranch(gitProject);
                gitRepositoryService.updateGitProjectInfo(gitProject);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        }
        modelBuild(model, projects);
        return INDEX_HTML;
    }

    /**
     * 创建标签
     *
     * @param model
     * @param tagName
     * @param tagLog
     * @return
     */
    @RequestMapping({"/createTag/{tagName}/{tagLog}"})
    public String createTag(Model model, @PathVariable(value = "tagName") String tagName, @PathVariable(value = "tagLog") String tagLog) {
        List<GitProject> projects = gitRepositoryService.getAllGitProject();
        for (GitProject gitProject : projects) {
            try {
                gitRepositoryService.createTag(gitProject, tagName, tagLog);
                gitRepositoryService.updateGitProjectInfo(gitProject);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        }
        modelBuild(model, projects);
        return INDEX_HTML;
    }

    /**
     * 文件状态列表详情
     *
     * @param model
     * @param projectName
     * @return
     */
    @RequestMapping({"/fileListDetails/{name}"})
    public String fileListDetails(Model model, @PathVariable(value = "name") String projectName) {
        List<GitProject> projects = gitRepositoryService.getAllGitProject();
        for (GitProject gitProject : projects) {
            if (projectName.equals(gitProject.getName())) {
                try {
                    gitRepositoryService.updateGitProjectInfo(gitProject);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (GitAPIException e) {
                    e.printStackTrace();
                }
                model.addAttribute("project", gitProject);
            }
        }
        return "fileListDetails";
    }

    /**
     * 新增文件预览
     *
     * @param model
     * @param request
     * @return
     */
    @RequestMapping({UNTRACKED_FILE_VIEW_PATH_PREFIX + "{projectName}/**"})
    public String untrackedFileView(Model model, @PathVariable(value = "projectName") String projectName, HttpServletRequest request) {
        String path = request.getRequestURI().substring(UNTRACKED_FILE_VIEW_PATH_PREFIX.length(), request.getRequestURI().length());
        model.addAttribute("exception", false);
        List<GitProject> projects = gitRepositoryService.getAllGitProject();
        String workHome = gitRepositoryConfig.getWorkHome();
        for (GitProject gitProject : projects) {
            if (projectName.equals(gitProject.getName())) {
                try {
                    String fileName = URLDecoder.decode(path.substring(projectName.length() + 1, path.length()), "UTF-8");
                    String filePath = workHome + File.separator + gitProject.getName() + File.separator + fileName;
                    String fileContent = gitRepositoryService.getFileContent(gitProject, fileName);
                    if (fileName.contains(File.separator)) {
                        fileName = fileName.substring(fileName.lastIndexOf(File.separator), fileName.length());
                    }
                    model.addAttribute("filePath", filePath);
                    model.addAttribute("fileContent", fileContent);
                    model.addAttribute("fileName", fileName);
                } catch (IOException e) {
                    e.printStackTrace();
                    model.addAttribute("exception", true);
                    model.addAttribute("exceptionMessage", e.getMessage());
                }

            }
        }
        return "untrackedFileView";
    }

    /**
     * 删除标签
     *
     * @param model
     * @param tagName
     * @return
     */
    @RequestMapping({"/deleteTag/{tagName}"})
    public String deleteTag(Model model, @PathVariable(value = "tagName") String tagName) {
        List<GitProject> projects = gitRepositoryService.getAllGitProject();
        for (GitProject gitProject : projects) {
            try {
                gitRepositoryService.deleteTag(gitProject, tagName);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        }
        modelBuild(model, projects);
        return INDEX_HTML;
    }

    /**
     * 合并分支，将被合并分支的修改并入当前工作分支
     *
     * @param model
     * @param currWorkBranch 当前工作分支
     * @param sourceBranch   被合并分支
     * @return
     */
    @RequestMapping({"/mergeBranch/{currWorkBranch}/{sourceBranch}/**"})
    public String mergeBranch(Model model, @PathVariable(value = "currWorkBranch") String currWorkBranch,
                              @PathVariable(value = "sourceBranch") String sourceBranch,
                              HttpServletRequest request) {
        String message = null;
        try {
            message = URLDecoder.decode(request.getRequestURI().substring(("/mergeBranch/"+currWorkBranch+"/"+sourceBranch+"/").length(), request.getRequestURI().length()), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        List<GitProject> projects = gitRepositoryService.getAllGitProject();
        for (GitProject gitProject : projects) {
            try {
                gitRepositoryService.mergeBranch(gitProject, currWorkBranch, sourceBranch, message);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        }
        modelBuild(model, projects);
        return INDEX_HTML;
    }

    /**
     * 构建model属性
     *
     * @param model
     * @param projects
     * @param objects
     */
    private void modelBuild(Model model, List<GitProject> projects, Object... objects) {
        /**
         * 添加项目集合属性
         */
        model.addAttribute("projects", projects);
        /**
         * 添加分支属性
         */
        List<String> branchIntersect = gitRepositoryService.getBranchIntersect(projects);
        model.addAttribute("branchIntersect", branchIntersect);
        /**
         * 添加标签属性
         */
        List<String> tagIntersect = gitRepositoryService.getTagIntersect(projects);
        model.addAttribute("tagIntersect", tagIntersect);
        model.addAllAttributes(Arrays.asList(objects));
    }

}
