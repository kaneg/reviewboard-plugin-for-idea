package rb;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.commands.GitCommand;
import git4idea.commands.GitSimpleHandler;
import git4idea.repo.GitRepository;

import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: gongze
 * Date: 3/21/2015
 * Time: 4:08 PM
 */
public class GitVCSBuilder implements VCSBuilder {

    private AbstractVcs vcs;
    private String repositoryURL;
    private String diff;
    private VirtualFile workingCopy;

    public GitVCSBuilder(AbstractVcs vcs) {
        this.vcs = vcs;
    }

    @Override
    public AbstractVcs getVCS() {
        return vcs;
    }

    @Override
    public String getDiff() {
        return diff;
    }

    @Override
    public String getRepositoryURL() {
        return repositoryURL;
    }

    @Override
    public String getBasePath() {
        return "";
    }

    public void build(Project project, VirtualFile[] vFiles) {
        getRepositoryRoot(project, vFiles);
        diff = generateDiff(project, workingCopy, vFiles);
    }

    private void getRepositoryRoot(Project project, VirtualFile[] vFiles) {
        for (VirtualFile vf : vFiles) {
            if (vf != null) {
                vf.refresh(false, true);
                GitRepository repositoryForFile = GitUtil.getRepositoryManager(project).getRepositoryForFile(vf);
                assert repositoryForFile != null;
                repositoryURL = repositoryForFile.getRemotes().iterator().next().getFirstUrl();
                workingCopy = repositoryForFile.getRoot();
                break;
            }
        }

    }

    private String generateDiff(Project project, VirtualFile root, VirtualFile[] vFiles) {
        try {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                public void run() {
                    FileDocumentManager.getInstance().saveAllDocuments();
                }
            });
            GitSimpleHandler handler = new GitSimpleHandler(project, root, GitCommand.DIFF);
            handler.addParameters("HEAD");
            handler.setSilent(true);
            handler.setStdoutSuppressed(true);
            handler.addRelativeFiles(Arrays.asList(vFiles));
            System.out.println(handler.printableCommandLine());
            return handler.run();
        } catch (Exception e) {
            Messages.showWarningDialog("Svn is still in refresh. Please try again later.", "Alter");
        }
        return null;
    }
}
