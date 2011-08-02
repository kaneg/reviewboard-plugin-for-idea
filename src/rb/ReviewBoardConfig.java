package rb;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ReviewBoardConfig implements ProjectComponent, Configurable {

    static final String COMPONENT_NAME = "ReviewBoardPlugin";
    private static final String RB__SERVER = "rb_server";
    private static final String RB__USERNAME = "rb_username";
    private static final String RB__ENCODED_PASSWORD = "rb_encodedPassword";

    private ReviewBoardConfigForm form;
    private String server;
    private String username;
    private String encodedPassword;
    private Project project;

    public ReviewBoardConfig(Project project) {
        this.project = project;
    }

    @NonNls
    @NotNull
    public String getComponentName() {
        return COMPONENT_NAME;
    }

    public void initComponent() {
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
        server = propertiesComponent.getValue(RB__SERVER);
        username = propertiesComponent.getValue(RB__USERNAME);
        encodedPassword = propertiesComponent.getValue(RB__ENCODED_PASSWORD);
    }

    public void disposeComponent() {
    }

    @Nls
    public String getDisplayName() {
        return "Review Board";
    }

    public Icon getIcon() {
        return null;
    }

    @Nullable
    @NonNls
    public String getHelpTopic() {
        return null;
    }

    public JComponent createComponent() {
        if (form == null) {
            form = new ReviewBoardConfigForm();
        }
        return form.getRootComponent();
    }

    public boolean isModified() {
        return form != null && form.isModified(this);
    }

    public void apply() throws ConfigurationException {
        if (form != null) {
            form.getData(this);
            PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(this.project);
            propertiesComponent.setValue(RB__SERVER, server);
            propertiesComponent.setValue(RB__USERNAME, username);
            propertiesComponent.setValue(RB__ENCODED_PASSWORD, encodedPassword);
        }

    }

    public void reset() {
        if (form != null) {
            form.setData(this);
        }
    }

    public void disposeUIResources() {
        form = null;
    }

    public String getServer() {
        return server;
    }

    public void setServer(final String server) {
        this.server = server;
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEncodedPassword() {
        return encodedPassword;
    }

    public void setEncodedPassword(String encodedPassword) {
        this.encodedPassword = encodedPassword;
    }

    @Override
    public void projectOpened() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void projectClosed() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
