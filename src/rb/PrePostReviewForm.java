package rb;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.TextFieldWithStoredHistory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by IntelliJ IDEA.
 * User: Gong Zeng
 * Date: 5/13/11
 * Time: 2:24 PM
 */
public class PrePostReviewForm extends DialogWrapper {
    private JPanel rootContainer;
    private JTextArea descTextArea;
    private JRadioButton newRequestRadioButton;
    private JRadioButton existingRequestRadioButton;
    private JTextField reviewIdTextField;
    private JTextField summaryTextField;
    private JTextField branchTextField;
    private JTextField bugTextField;
    private TextFieldWithStoredHistory groupTextField;
    private TextFieldWithStoredHistory peopleTextField;
    private JButton showDiffButton;
    private JButton loadReviewButton;
    private Project project;
    private JTextArea diffTextArea;

    public PrePostReviewForm(final Project project, String commitMessage, final String patch) {
        super(project);
        setTitle("Post Review");
        this.project = project;
        summaryTextField.setText(commitMessage);
        newRequestRadioButton.setSelected(true);
        reviewIdTextField.setEnabled(false);
        loadReviewButton.setEnabled(false);
        newRequestRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reviewIdTextField.setEnabled(false);
                loadReviewButton.setEnabled(false);
            }
        });
        existingRequestRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reviewIdTextField.setEnabled(true);
                loadReviewButton.setEnabled(true);
            }
        });
        setOKButtonText("Post");
        init();
        showDiffButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog dialog = new JDialog((Dialog) null, true);
                dialog.setTitle("Diff");
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                diffTextArea = new JTextArea(patch);
                JScrollPane sp = new JScrollPane(diffTextArea);
                dialog.add(sp);
                dialog.setSize(800, 600);
                dialog.setLocationRelativeTo(PrePostReviewForm.this.getPreviewComponent());
                dialog.setVisible(true);
            }
        });
        loadReviewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String reviewId = reviewIdTextField.getText();
                if (reviewId == null || "".equals(reviewId)) {
                    Messages.showWarningDialog(project, "Please fill review request id", null);
                    reviewIdTextField.grabFocus();
                    return;
                }
                try {
                    ReviewSettings setting = getSetting();
                    try {
                        ReviewBoardClient.loadReview(setting, reviewId);
                    } catch (Exception e1) {
                        Messages.showErrorDialog("No such review id:" + reviewId, "Error");
                        return;
                    }
                    if (setting.getDescription() != null) {
                        descTextArea.setText(setting.getDescription());
                    }
                    if (setting.getBranch() != null) {
                        branchTextField.setText(setting.getBranch());
                    }
                    if (setting.getBugsClosed() != null) {
                        bugTextField.setText(setting.getBugsClosed());
                    }
                    if (setting.getPeople() != null) {
                        peopleTextField.setText(setting.getPeople());
                    }
                    if (setting.getGroup() != null) {
                        groupTextField.setText(setting.getGroup());
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean isOKActionEnabled() {
        peopleTextField.addCurrentTextToHistory();
        if (reviewIdTextField.isEnabled()) {
            String reviewId = reviewIdTextField.getText();
            if (reviewId == null || "".equals(reviewId)) {
                Messages.showWarningDialog(project, "Please fill review request id", null);
                reviewIdTextField.grabFocus();
                return false;
            }
        }
        return true;
    }

    public JComponent getPreviewComponent() {
        return rootContainer;
    }

    public ReviewSettings getSetting() {
        ReviewSettings settings = new ReviewSettings();
        ReviewBoardConfig config = project.getComponent(ReviewBoardConfig.class);

        //from config
        settings.setServer(config.getServer());
        settings.setUsername(config.getUsername());
        settings.setPassword(PasswordMangler.decode(config.getEncodedPassword()));

        //from ui
        if (reviewIdTextField.isEnabled()) {
            settings.setReviewId(reviewIdTextField.getText());
        }
        settings.setSummary(summaryTextField.getText());
        settings.setBranch(branchTextField.getText());
        settings.setBugsClosed(bugTextField.getText());
        settings.setGroup(groupTextField.getText());
        settings.setPeople(peopleTextField.getText());
        settings.setDescription(descTextArea.getText());

        return settings;
    }

    @Override
    protected JComponent createCenterPanel() {
        return getPreviewComponent();
    }

    private void createUIComponents() {
        peopleTextField = new TextFieldWithStoredHistory("rb.people");

//        peopleTextField.addKeyboardListener(new KeyAdapter() {
//            @Override
//            public void keyTyped(KeyEvent e) {
//                System.out.println(e);
//                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
//                    peopleTextField.setSelectedIndex(-1);
////                    peopleTextField
//                }
//            }
//        });
        groupTextField = new TextFieldWithStoredHistory("rb.group");
    }
}

