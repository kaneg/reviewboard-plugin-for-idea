package rb;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diff.impl.patch.FilePatch;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.util.PopupUtil;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.InvokeAfterUpdateMode;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.GitVcs;
import git4idea.commands.GitCommand;
import git4idea.commands.GitSimpleHandler;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Gong Zeng
 * Date: 5/13/11
 * Time: 4:17 PM
 */
public class PostReviewAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent event) {
        final Project project = event.getData(PlatformDataKeys.PROJECT);
//        final SvnVcs svnVcs = SvnVcs.getInstance(project);
        final AbstractVcs vcs = GitVcs.getInstance(project);
        final VirtualFile[] vFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
        if (vFiles == null || vFiles.length == 0) {
            Messages.showMessageDialog("No file to be review", "Alert", null);
            return;
        }
        if (!ProjectLevelVcsManager.getInstance(project).checkAllFilesAreUnder(vcs, vFiles)) {
            setActionEnable(event, true);
            Messages.showWarningDialog("Some of selected files are not under control of SVN.", "Warning");
            return;
        }

        final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        changeListManager.invokeAfterUpdate(new Runnable() {
            @Override
            public void run() {
                System.out.println("Executing...");
                try {
                    execute(project, vcs, vFiles, changeListManager);
                } catch (VcsException e) {
                    e.printStackTrace();
                }
            }
        }, InvokeAfterUpdateMode.SYNCHRONOUS_CANCELLABLE, "Refresh VCS", ModalityState.current());
    }

    @Override
    public void update(AnActionEvent event) {
        final Project project = event.getData(PlatformDataKeys.PROJECT);

        final VirtualFile[] vFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
        if (vFiles == null || vFiles.length == 0) {
            setActionEnable(event, false);
            return;
        }
        final ChangeListManager changeListManager = ChangeListManager.getInstance(project);

        int enableCount = 0;
        for (VirtualFile vf : vFiles) {

            if (vf != null) {
                vf.refresh(false, true);
                Change change = changeListManager.getChange(vf);
                if (change != null) {
                    if (change.getType().equals(Change.Type.NEW)) {
                        enableCount++;
                        continue;
                    }
                    ContentRevision beforeRevision = change.getBeforeRevision();
                    if (beforeRevision != null) {
                        VcsRevisionNumber revisionNumber = beforeRevision.getRevisionNumber();
                        if (!revisionNumber.equals(VcsRevisionNumber.NULL)) {
                            enableCount++;
                        }
                    }
                }
            }
        }
        setActionEnable(event, enableCount == vFiles.length);
    }

    private void setActionEnable(AnActionEvent event, boolean isEnable) {
        event.getPresentation().setEnabled(isEnable);
    }

    private void execute(final Project project, AbstractVcs vcs, VirtualFile[] vFiles, ChangeListManager changeListManager) throws VcsException {
        List<Change> changes = new ArrayList<Change>();
        String changeMessage = null;
        String localRootDir = null;
        String remoteRootUrl = null;
        String repositoryUrl = null;
        VirtualFile root = null;
        final String patch;
        for (VirtualFile vf : vFiles) {
            if (vf != null) {
                vf.refresh(false, true);
                GitRepository repositoryForFile = GitUtil.getRepositoryManager(project).getRepositoryForFile(vf);
                repositoryUrl = repositoryForFile.getRemotes().iterator().next().getFirstUrl();
                root = repositoryForFile.getRoot();
                localRootDir = root.getPath();
                break;
            }
        }


        try {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                public void run() {
                    FileDocumentManager.getInstance().saveAllDocuments();
                }
            });
            GitSimpleHandler handler = new GitSimpleHandler(project, root, GitCommand.DIFF);
            handler.addParameters(new String[]{"HEAD"});
            handler.setSilent(true);
            handler.setStdoutSuppressed(true);
            handler.addRelativeFiles(Arrays.asList(vFiles));
            System.out.println(handler.printableCommandLine());
            String diffOutput = handler.run();
            patch = diffOutput;
        } catch (Exception e) {
            Messages.showWarningDialog("Svn is still in refresh. Please try again later.", "Alter");
            return;
        }

        ReviewBoardConfig config = project.getComponent(ReviewBoardConfig.class);
        if (config.getServer() == null || config.getServer().isEmpty()) {
            Messages.showMessageDialog(project, "Please set the review board server address in config panel", "Info", null);
            return;
        }
        if (config.getUsername() == null || "".equals(config.getUsername())) {
            Messages.showMessageDialog(project, "Please set the review board user name in config panel", "Info", null);
            return;
        }
        if (config.getEncodedPassword() == null || config.getEncodedPassword().isEmpty()) {
            Messages.showMessageDialog(project, "Please set the view board password in config panel", "Info", null);
            return;
        }

        ReviewBoardClient reviewBoardClient = new ReviewBoardClient(config.getServer(), config.getUsername(), PasswordMangler.decode(config.getEncodedPassword()));
        Repository[] repositories;
        try {
            repositories = reviewBoardClient.getRepositories().repositories;
        } catch (Exception e) {
            PopupUtil.showBalloonForActiveFrame("Error to list repository", MessageType.ERROR);
            return;
        }


        int possibleRepoIndex = getPossibleRepoIndex(repositoryUrl, repositories);

        final String finalRepositoryUrl = repositoryUrl;
        final PrePostReviewForm prePostReviewForm = new PrePostReviewForm(project, "", patch, repositories, possibleRepoIndex) {

            @Override
            protected void doOKAction() {
                if (!isOKActionEnabled()) {
                    return;
                }
                final ReviewSettings setting = this.getSetting();
                if (setting == null) {
                    return;
                }

                setting.setSvnBasePath("");
                setting.setSvnRoot(finalRepositoryUrl);
                if (this.getDiff() != null) {
                    setting.setDiff(this.getDiff());
                } else {
                    setting.setDiff(patch);
                }
                Task.Backgroundable task = new Task.Backgroundable(project, "running", false, new PerformInBackgroundOption() {
                    @Override
                    public boolean shouldStartInBackground() {
                        return false;
                    }

                    @Override
                    public void processSentToBackground() {
                    }
                }) {
                    boolean result;

                    @Override
                    public void onSuccess() {
                        if (result) {
                            String url = setting.getServer() + "/r/" + setting.getReviewId() + "/diff/";
                            int success = Messages.showYesNoDialog("The review url is " + url + "\r\n" +
                                    "Open the url?", "Success", null);
                            if (success == 0) {
                                BrowserUtil.launchBrowser(url);
                            }
                        } else {
//                            Messages.showErrorDialog("Post review failure", "Error");
                            PopupUtil.showBalloonForActiveFrame("Post review failure", MessageType.ERROR);
                        }
                    }

                    @Override
                    public void run(@NotNull ProgressIndicator progressIndicator) {
                        progressIndicator.setIndeterminate(true);
                        result = ReviewBoardClient.postReview(setting, progressIndicator);
                    }
                };
                ProgressManager.getInstance().run(task);
                super.doOKAction();
            }
        };
        prePostReviewForm.show();
    }

    private int getPossibleRepoIndex(@NotNull String repositoryUrl, Repository[] repositories) {
        int possibleRepoIndex = -1;
        for (int j = 0; j < repositories.length; j++) {
            if (repositoryUrl.equals(repositories[j].mirror_path)) {
                possibleRepoIndex = j;
                break;
            }
        }
        if (possibleRepoIndex == -1) {
            int i = repositoryUrl.lastIndexOf('/');
            if (i > -1) {
                String shortName = repositoryUrl.substring(i + 1);
                for (int j = 0; j < repositories.length; j++) {
                    if (shortName.equals(repositories[j].name)) {
                        possibleRepoIndex = j;
                        break;
                    }
                }
            }
        }

        return possibleRepoIndex;
    }


    private List<FilePatch> buildPatch(Project project, List<Change> changes, String localRootDir, boolean b) {
        //      List<FilePatch> filePatches = IdeaTextPatchBuilder.buildPatch(project, changes, localRootDir, false);
//    List<FilePatch> filePatches = TextPatchBuilder.buildPatch(changes, localRootDir, false);
        Object result = null;
        try {//invoke the api in 10.x
            Class c = Class.forName("com.intellij.openapi.diff.impl.patch.IdeaTextPatchBuilder");
            Method buildPatchMethod = c.getMethod("buildPatch", Project.class, Collection.class, String.class, boolean.class);
            result = buildPatchMethod.invoke(null, project, changes, localRootDir, b);
        } catch (ClassNotFoundException e) {
            try {//API in 9.0x
                Class c = Class.forName("com.intellij.openapi.diff.impl.patch.TextPatchBuilder");
                Method buildPatchMethod = c.getMethod("buildPatch", Collection.class, String.class, boolean.class);
                result = buildPatchMethod.invoke(null, changes, localRootDir, b);
            } catch (Exception e1) {
                Messages.showErrorDialog("The current version doesn't support the review", "Not support");
                return null;
            }
        } catch (Exception e) {
            Messages.showErrorDialog("The current version doesn't support the review", "Not support");
        }
        if (result != null && result instanceof List) {
            return (List<FilePatch>) result;
        }
        return null;
    }

    public boolean isDumbAware() {
        return true;
    }
}
