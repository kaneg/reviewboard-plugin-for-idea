package rb;

import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.TextFieldWithStoredHistory;
import org.jetbrains.annotations.NotNull;

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
    private JComboBox<RepoComboItem> comboBoxRepository;
    private Project project;
    private JTextArea diffTextArea;

    static class RepoComboItem {
        private Repository repo;

        public RepoComboItem(Repository repo) {
            this.repo = repo;
        }

        @Override
        public String toString() {
            return String.format("%s(%s)", repo.name, repo.id);
        }
    }

    public PrePostReviewForm(final Project project, String commitMessage, final String patch, Repository[] repositories, int possibleRepoIndex) {
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

        for (Repository repo : repositories) {
            comboBoxRepository.addItem(new RepoComboItem(repo));
        }
        if (possibleRepoIndex > -1) {
            comboBoxRepository.setSelectedIndex(possibleRepoIndex);
        }


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
                final String reviewId = reviewIdTextField.getText();
                if (reviewId == null || "".equals(reviewId)) {
                    Messages.showWarningDialog(project, "Please fill review request id", null);
                    reviewIdTextField.grabFocus();
                    return;
                }
                try {
                    final ReviewSettings setting = getSetting();
                    if (setting == null) {
                        return;
                    }

                    Task.Backgroundable task = new Task.Backgroundable(project, "Load review details", false, new PerformInBackgroundOption() {
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
                            try {
                                ReviewBoardClient.loadReview(setting, reviewId);
                            } catch (Exception e1) {
                                Messages.showErrorDialog("No such review id:" + reviewId, "Error");
                                return;
                            }
                            if (setting.getSummary() != null) {
                                summaryTextField.setText(setting.getSummary());
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
                        }
                    };
                    ProgressManager.getInstance().run(task);

                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean isOKActionEnabled() {
        peopleTextField.addCurrentTextToHistory();
        groupTextField.addCurrentTextToHistory();
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

    public String getDiff() {
        if (diffTextArea != null) {
            return diffTextArea.getText();
        } else {
            return null;
        }
    }

    public ReviewSettings getSetting() {
        ReviewSettings settings = new ReviewSettings();
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
        settings.setRepoId(((RepoComboItem) comboBoxRepository.getSelectedItem()).repo.id);
//        if (settings.getServer() == null || "".equals(settings.getServer())) {
//            Messages.showMessageDialog(project, "Please set the review board server address in config panel", "Info", null);
//            return null;
//        }
//        if (settings.getUsername() == null || "".equals(settings.getUsername())) {
//            Messages.showMessageDialog(project, "Please set the review board user name in config panel", "Info", null);
//            return null;
//        }
//        if (settings.getPassword() == null || "".equals(settings.getPassword())) {
//            JPasswordField pf = new JPasswordField();
//            pf.grabFocus();
//            int okCxl = JOptionPane.showConfirmDialog(null, pf, "Enter Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
//
//            if (okCxl == JOptionPane.OK_OPTION) {
//                String password = new String(pf.getPassword());
//                settings.setPassword(password);
//            } else {
//                return null;
//            }
//        }
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

