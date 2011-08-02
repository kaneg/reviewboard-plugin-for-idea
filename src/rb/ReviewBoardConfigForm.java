package rb;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * The configuration form for the Code Review Plugin.
 */
public class ReviewBoardConfigForm {
    private JTextField serverTextField;
    private JTextField usernameTextField;
    private JPasswordField passwordField;
    private JPanel rootComponent;
    private JTabbedPane tabbedPane;

    private boolean passwordChanged;

    public ReviewBoardConfigForm() {

        passwordField.setText("******");
        passwordField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                passwordChanged = true;
            }

            public void removeUpdate(DocumentEvent e) {
                passwordChanged = true;
            }

            public void changedUpdate(DocumentEvent e) {
                passwordChanged = true;
            }
        });
    }

    public JComponent getRootComponent() {
        return rootComponent;
    }

    /**
     * Called by IDEA to apply the user's code review configuration to the form.
     */
    public void setData(ReviewBoardConfig config) {
        serverTextField.setText(config.getServer());
        usernameTextField.setText(config.getUsername());
    }

    /**
     * Called by IDEA to retrieve the user's settings from the form.
     */
    public void getData(ReviewBoardConfig config) throws ConfigurationException {
        String server = serverTextField.getText();
        if (server == null || server.trim().length() == 0) {
            setFocus(serverTextField);
            throw new ConfigurationException("server must be supplied.");
        }


        // update the configuration object
        if (server.endsWith("/")) {
            server = server.substring(0, server.length() - 1);
        }
        config.setServer(server);
        config.setUsername(usernameTextField.getText());
        if (passwordChanged) {
            String encodedPassword = PasswordMangler.encode(new String(passwordField.getPassword()));
            config.setEncodedPassword(encodedPassword);
        }
        passwordChanged = false;
    }

    /**
     * Sets the focus on to the given component.
     */
    private void setFocus(final JComponent component) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                component.requestFocus();
            }
        });
    }

    /**
     * Indicates whether or not any of the configuration has been changed by the user. This
     * determines whether or not the 'Apply' button should be enabled.
     */
    public boolean isModified(ReviewBoardConfig config) {
        if (serverTextField.getText() != null ? !serverTextField.getText().equals(config.getServer()) : config.getServer() != null) {
            return true;
        }

        if (usernameTextField.getText() != null ? !usernameTextField.getText().equals(config.getUsername()) : config.getUsername() != null) {
            return true;
        }
        return passwordChanged;
    }
}
