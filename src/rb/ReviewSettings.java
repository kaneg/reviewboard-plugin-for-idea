package rb;

/**
 * Created by IntelliJ IDEA.
 * User: Gong Zeng
 * Date: 5/13/11
 * Time: 11:28 AM
 */
public class ReviewSettings {
    private String server;
    private String username;
    private String password;
    private String summary;
    private String description;
    private String branch;
    private String bugsClosed;
    private String group;
    private String people;
    private String reviewId;
    private String svnRoot;
    private String svnBasePath;
    private String diff;
    private String repoId;

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public String getBranch() {
        return branch;
    }

    public String getBugsClosed() {
        return bugsClosed;
    }

    public String getGroup() {
        return group;
    }

    public String getPeople() {
        return people;
    }

    public String getReviewId() {
        return reviewId;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public void setBugsClosed(String bugsClosed) {
        this.bugsClosed = bugsClosed;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setPeople(String people) {
        this.people = people;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public String getSvnRoot() {
        return svnRoot;
    }

    public String getSvnBasePath() {
        return svnBasePath;
    }

    public void setSvnRoot(String svnRoot) {
        this.svnRoot = svnRoot;
    }

    public void setSvnBasePath(String svnBasePath) {
        this.svnBasePath = svnBasePath;
    }

    public String getDiff() {
        return diff;
    }

    public void setDiff(String diff) {
        this.diff = diff;
    }

    public String getRepoId() {
        return repoId;
    }

    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }
}
