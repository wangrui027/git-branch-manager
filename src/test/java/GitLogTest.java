import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GitLogTest {

    @Test
    public void showMyLog() throws IOException, GitAPIException {
        System.out.println("输出王睿的Git提交日志");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Git git = Git.open(new File("D:\\OperationCenter-SNAPSHOT"));
        Iterable<RevCommit> it = git.log().call();
        for (RevCommit commit : it) {
            if (commit.getAuthorIdent().getEmailAddress().equals("wangrui1066@geostar.com.cn")) {
                String time = sdf.format(new Date(new Long(commit.getCommitTime()) * 1000));
                String message = commit.getShortMessage();
                System.out.println(time + "  --  " + message);
            }
        }
    }

}
