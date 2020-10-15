package com.geostar.geostack.git_branch_manager.web;

import com.geostar.geostack.git_branch_manager.common.Page;
import com.geostar.geostack.git_branch_manager.config.GitRepositoryConfig;
import com.geostar.geostack.git_branch_manager.pojo.GitLog;
import com.geostar.geostack.git_branch_manager.pojo.GitProject;
import com.geostar.geostack.git_branch_manager.service.IGitRepositoryService;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.*;

@Controller
@RequestMapping("/")
public class IndexController {

    private static final Logger logger = LoggerFactory.getLogger(IndexController.class);
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

    @PostMapping("/setGitAccount")
    public String setGitAccount(Model model, String username, String password) {
        gitRepositoryConfig.setGitUsername(username);
        gitRepositoryConfig.setGitPassword(password);
        List<GitProject> projects = gitRepositoryService.getAllGitProject();
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
        buildPom(projects);
        return INDEX_HTML;
    }

    /**
     * 创建分支
     *
     * @param model
     * @return
     */
    @RequestMapping({"/createBranch/{branchName}"})
    public String createBranch(Model model, @PathVariable(value = "branchName") String branchName) {
        try {
            branchName = URLDecoder.decode(branchName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
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
     * @return
     */
    @RequestMapping({"/switchBranch/{branchName}"})
    public String switchBranchAll(Model model, @PathVariable(value = "branchName") String branchName) {
        try {
            branchName = URLDecoder.decode(branchName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
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
        try {
            inputMessage = URLDecoder.decode(inputMessage, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
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
        try {
            tagName = URLDecoder.decode(tagName, "UTF-8");
            tagLog = URLDecoder.decode(tagLog, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
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
     * 从某标签检出代码到某新分支
     *
     * @param model
     * @param tagName
     * @param branchName
     * @return
     */
    @RequestMapping({"/createBranchByTag/{tagName}/{branchName}"})
    public String createBranchByTag(Model model, @PathVariable(value = "tagName") String tagName, @PathVariable(value = "branchName") String branchName) {
        try {
            tagName = URLDecoder.decode(tagName, "UTF-8");
            branchName = URLDecoder.decode(branchName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        List<GitProject> projects = gitRepositoryService.getAllGitProject();
        for (GitProject gitProject : projects) {
            try {
                gitRepositoryService.createBranchByTag(gitProject, tagName, branchName);
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
        try {
            tagName = URLDecoder.decode(tagName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
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
    @RequestMapping({"/mergeBranch/{currWorkBranch}/{sourceBranch}/{message}"})
    public String mergeBranch(Model model, @PathVariable(value = "currWorkBranch") String currWorkBranch,
                              @PathVariable(value = "sourceBranch") String sourceBranch,
                              @PathVariable(value = "message") String message) {
        try {
            currWorkBranch = URLDecoder.decode(currWorkBranch, "UTF-8");
            sourceBranch = URLDecoder.decode(sourceBranch, "UTF-8");
            message = URLDecoder.decode(message, "UTF-8");
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
     * 分页获取Git日志
     *
     * @param username
     * @return
     */
    @ResponseBody
    @RequestMapping({"/getCommitLogs", "/getCommitLogs/{username}", "/getCommitLogs/{username}/{projectName}"})
    public List<GitLog> getCommitLogs(
            @PathVariable(value = "username", required = false) String username,
            @PathVariable(value = "projectName", required = false) String projectName) {
        Page<GitLog> page = new Page<>();
        page.setPageIndex(0);
        try {
            gitRepositoryService.getCommitLogs(page, username, projectName);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        Set<String> userSet = new HashSet<>();
        for (GitLog log : page.getData()) {
            userSet.add(log.getUsername());
        }
        String[] userArr = new String[userSet.size()];
        userArr = userSet.toArray(userArr);
        Arrays.sort(userArr);
        return page.getAllData();
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
        model.addAttribute("gitRepositoryConfig", gitRepositoryConfig);
        model.addAllAttributes(Arrays.asList(objects));
    }

    /**
     * 构建最上层的pom文件
     *
     * @param projects
     */
    private void buildPom(List<GitProject> projects) {
        File file = new File(gitRepositoryConfig.getWorkHome());
        try {
            Document document = DocumentHelper.parseText(IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("pom_template.xml"), Charset.forName("UTF-8")));
            Element modules = document.getRootElement().element("modules");
            Iterator<Element> it = modules.elementIterator();
            while (it.hasNext()) {
                Element element = it.next();
                modules.remove(element);
            }
            for (GitProject project : projects) {
                Element module = modules.addElement("module");
                module.setText("../modules/" + project.getName());
            }
            String pomPath = file.getParent() + File.separator + "pom" + File.separator + "pom.xml";
            OutputFormat format = OutputFormat.createPrettyPrint();
            XMLWriter xmlWriter = new XMLWriter(new FileOutputStream(pomPath), format);
            xmlWriter.write(document);
            xmlWriter.close();
            logger.info("更新本地最上层pom文件，pom文件路径：" + pomPath);
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
