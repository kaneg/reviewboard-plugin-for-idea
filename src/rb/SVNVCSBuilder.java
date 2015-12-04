package rb;

import com.intellij.openapi.diff.impl.patch.FilePatch;
import com.intellij.openapi.diff.impl.patch.UnifiedDiffWriter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.svn.SvnUtil;
import org.jetbrains.idea.svn.SvnVcs;
import org.tmatesoft.svn.core.SVNURL;

import java.io.File;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: gongze
 * Date: 3/21/2015
 * Time: 4:09 PM
 */
public class SVNVCSBuilder implements VCSBuilder {
    private AbstractVcs vcs;
    private String diff;
    private String basePath;
    private String repositoryURL;
    private String workingCopyDir;

    public SVNVCSBuilder(AbstractVcs vcs) {
        this.vcs = vcs;
    }

    @Override
    public AbstractVcs getVCS() {
        return vcs;
    }

    @Override
    public void build(Project project, VirtualFile[] vFiles) {
        getRepositoryRoot(project, vFiles);
        diff = generateDiff(project, vFiles);
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
        return basePath;
    }

    private void getRepositoryRoot(Project project, VirtualFile[] vFiles) {
        File localRootDir = null;
        String remoteRootUrl = null;
        String repositoryUrl = null;
        for (VirtualFile vf : vFiles) {
            if (vf != null) {
                vf.refresh(false, true);
                File workingCopyRoot = SvnUtil.getWorkingCopyRoot(new File(vf.getPath()));
                if (workingCopyRoot == null) {
                    workingCopyRoot = SvnUtil.getWorkingCopyRootNew(new File(vf.getPath()));
                }
                if (workingCopyRoot == null) {
                    Messages.showWarningDialog("Cann't get working copy root of the file:" + vf.getPath(), "Error");
                    return;
                }
                System.out.println("workcopyroot:" + workingCopyRoot);
                if (localRootDir == null) {
                    localRootDir = workingCopyRoot;
                }
                SvnVcs svnVcs = (SvnVcs) vcs;
                SVNURL url = SvnUtil.getUrl(svnVcs, workingCopyRoot);
                System.out.println("remoteRootUrl:" + url);
                if (url != null && remoteRootUrl == null) {
                    remoteRootUrl = url.toString();
                }
                SVNURL repositoryRoot = SvnUtil.getRepositoryRoot(svnVcs, workingCopyRoot);
                System.out.println("repository:" + repositoryRoot);
                if (repositoryRoot != null && repositoryUrl == null) {
                    repositoryUrl = repositoryRoot.toString();
                }
            }
        }
        assert remoteRootUrl != null;
        assert repositoryUrl != null;
        int i = remoteRootUrl.indexOf(repositoryUrl);
        final String basePathForReviewBoard;
        if (i != -1) {
            basePathForReviewBoard = remoteRootUrl.substring(i + repositoryUrl.length());
        } else {
            basePathForReviewBoard = "";
        }
        repositoryURL = repositoryUrl;
        this.workingCopyDir = localRootDir.getPath();
        basePath = basePathForReviewBoard;
    }


    private String generateDiff(Project project, VirtualFile[] vFiles) {
        try {
            List<Change> changes = getChanges(project, vFiles);
            List<FilePatch> filePatches = buildPatch(project, changes, this.workingCopyDir, false);
            if (filePatches == null) {
                Messages.showWarningDialog("Create diff error", "Alter");
                return null;
            }
            StringWriter w = new StringWriter();
            UnifiedDiffWriter.write(project, filePatches, w, "\r\n", null);
            w.close();
            return w.toString();
        } catch (Exception e) {
            Messages.showWarningDialog("Svn is still in refresh. Please try again later.", "Alter");
            return null;
        }
    }

    private List<Change> getChanges(Project project, VirtualFile[] vFiles) {
        List<Change> changes = new ArrayList<Change>();
        final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        for (VirtualFile vf : vFiles) {
            if (vf != null) {
                vf.refresh(false, true);
                Change change = changeListManager.getChange(vf);
                if (change != null && change.getType().equals(Change.Type.NEW)) {
                    final ContentRevision afterRevision = change.getAfterRevision();
                    change = new Change(null, new ContentRevision() {
                        @Override
                        public String getContent() throws VcsException {
                            return afterRevision.getContent();
                        }

                        @NotNull
                        @Override
                        public FilePath getFile() {
                            return afterRevision.getFile();
                        }

                        @NotNull
                        @Override
                        public VcsRevisionNumber getRevisionNumber() {
                            return new VcsRevisionNumber.Int(0);
                        }
                    }, change.getFileStatus()
                    );
                }
                changes.add(change);
            }
        }
        return changes;
    }

    private List<FilePatch> buildPatch(Project project, List<Change> changes, String localRootDir, boolean b) {
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
}
