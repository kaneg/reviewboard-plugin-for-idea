package rb;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: gongze
 * Date: 12/3/13
 * Time: 2:24 PM
 */
@State(name = "ReviewBoardSettings", storages = {@com.intellij.openapi.components.Storage(file = StoragePathMacros.APP_CONFIG + "/RBS.xml")})
public class ReviewBoardSettings implements PersistentStateComponent<ReviewBoardSettings.State>, Configurable {
    public static final String SETTING_NAME = "Review Board";
    State myState = new State();
    JTextField serverField = new JTextField();
    JButton loginButton = new JButton("Login");
    JButton logoutButton = new JButton("Logout");

    JPanel actionButton = new JPanel(new CardLayout());

    {
        actionButton.add(loginButton, "login");
        actionButton.add(logoutButton, "logout");
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getRBSession(serverField.getText());

                switchButton();
            }
        });

        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getSettings().getState().cookie = null;
                switchButton();
            }
        });
    }

    public String getRBSession(String server) {
        JPasswordField pf = new JPasswordField();
        pf.grabFocus();
        JTextField username = new JTextField();
        JPanel panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("User Name:", username)
                .addLabeledComponent("Password:", pf)
                .getPanel();
        int okCxl = JOptionPane.showConfirmDialog(null, panel, "Enter Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (okCxl == JOptionPane.OK_OPTION) {
            String password = new String(pf.getPassword());
            try {
                String cookie = ReviewBoardClient.login(server, username.getText(), password);
                if (cookie != null) {
                    getSettings().getState().cookie = cookie;
                    return cookie;
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(null, e1.getMessage());
            }
        }
        return null;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return SETTING_NAME;
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        JPanel panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("Server URL:", serverField)
                .addLabeledComponent("Action:", actionButton)
                .getPanel();
        switchButton();
        return panel;
    }

    private void switchButton() {
        CardLayout mgr = (CardLayout) actionButton.getLayout();
        if (getSettings().getState().cookie == null) {
            mgr.show(actionButton, "login");
        } else {
            mgr.show(actionButton, "logout");
        }
    }

    @Override
    public boolean isModified() {
        return !serverField.getText().equals(getSettings().getState().server);
    }

    @Override
    public void apply() throws ConfigurationException {
        getSettings().getState().server = serverField.getText();
    }

    @Override
    public void reset() {
        serverField.setText(getSettings().getState().server);
    }

    @Override
    public void disposeUIResources() {

    }

    public static class State {
        public String server;
        public String cookie;
    }

    @NotNull
    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(State state) {
        if (state != null) {
            myState = state;
        }
    }

    public static ReviewBoardSettings getSettings() {
        return ServiceManager.getService(ReviewBoardSettings.class);
    }
}
