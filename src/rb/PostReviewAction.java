package rb;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
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
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.net.URI;

/**
 * Created by IntelliJ IDEA.
 * User: Gong Zeng
 * Date: 5/13/11
 * Time: 4:17 PM
 */
public class PostReviewAction extends AnAction {
    String changeMessage = "";

    @Override
    public void actionPerformed(AnActionEvent event) {
        final Project project = event.getData(PlatformDataKeys.PROJECT);
        final VirtualFile[] vFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
        if (vFiles == null || vFiles.length == 0) {
            Messages.showMessageDialog("No file to be review", "Alert", null);
            return;
        }
        final AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(vFiles[0]);
        if (!ProjectLevelVcsManager.getInstance(project).checkAllFilesAreUnder(vcs, vFiles)) {
            setActionEnable(event, true);
            Messages.showWarningDialog("Some of selected files are not under control of SVN.", "Warning");
            return;
        }

        final ChangeListManager changeListManager = ChangeListManager.getInstance(project);

        for (VirtualFile vf : vFiles) {
            LocalChangeList changeList = changeListManager.getChangeList(vf);
            if (changeList != null) {
                changeMessage = changeList.getName();
                break;
            }
        }

        changeListManager.invokeAfterUpdate(new Runnable() {
            @Override
            public void run() {
                System.out.println("Executing...");
                try {
                    VCSBuilder builder = VCSBuilder.Factory.getBuilder(vcs);
                    if (builder != null) {
                        execute(project, builder, vFiles, changeListManager);
                    }
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

    private void execute(final Project project, final VCSBuilder vcsBuilder, VirtualFile[] vFiles, ChangeListManager changeListManager) throws VcsException {
        vcsBuilder.build(project, vFiles);
        final String diff = vcsBuilder.getDiff();
        if (diff == null) {
            Messages.showMessageDialog(project, "No diff generated", "Warn", null);
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

        final ReviewBoardClient reviewBoardClient = new ReviewBoardClient(config.getServer(), config.getUsername(), PasswordMangler.decode(config.getEncodedPassword()));
        Task.Backgroundable task = new Task.Backgroundable(project, "Query repository...", false, new PerformInBackgroundOption() {
            @Override
            public boolean shouldStartInBackground() {
                return false;
            }

            @Override
            public void processSentToBackground() {
            }
        }) {

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);
                Repository[] repositories = null;
                try {
                    repositories = reviewBoardClient.getRepositories().repositories;
                } catch (Exception e) {
                    PopupUtil.showBalloonForActiveFrame("Error to list repository:"+e.getMessage(), MessageType.ERROR);
                    throw new RuntimeException(e);
                }
                if (repositories != null) {
                    final Repository[] finalRepositories = repositories;
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            showPostForm(project, vcsBuilder, finalRepositories);
                        }
                    });

                }
            }
        };
        ProgressManager.getInstance().run(task);
    }

    private void showPostForm(final Project project, final VCSBuilder vcsBuilder, Repository[] repositories) {
        int possibleRepoIndex = getPossibleRepoIndex(vcsBuilder.getRepositoryURL(), repositories);
        final PrePostReviewForm prePostReviewForm = new PrePostReviewForm(project, changeMessage, vcsBuilder.getDiff(), repositories, possibleRepoIndex) {

            @Override
            protected void doOKAction() {
                if (!isOKActionEnabled()) {
                    return;
                }
                final ReviewSettings setting = this.getSetting();
                if (setting == null) {
                    return;
                }
                if (vcsBuilder.getBasePath() == null) {
                    setting.setSvnBasePath("");
                } else {
                    setting.setSvnBasePath(vcsBuilder.getBasePath());
                }
                setting.setSvnRoot(vcsBuilder.getRepositoryURL());
                if (this.getDiff() != null) {
                    setting.setDiff(this.getDiff());
                } else {
                    setting.setDiff(vcsBuilder.getDiff());
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
        if (possibleRepoIndex == -1) {
            String path = URI.create(repositoryUrl).getPath();
            String[] repos = new String[repositories.length];
            for (int i = 0; i < repos.length; i++) {
                repos[i] = repositories[i].name;
            }
            possibleRepoIndex = LevenshteinDistance.getClosest(path, repos);
        }

        return possibleRepoIndex;
    }


    public boolean isDumbAware() {
        return true;
    }
}
