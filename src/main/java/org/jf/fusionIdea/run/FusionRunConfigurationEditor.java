package org.jf.fusionIdea.run;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.SdkListCellRenderer;
import com.intellij.openapi.roots.ui.configuration.ModulesAlphaComparator;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.PanelWithAnchor;
import com.jetbrains.extensions.python.FileChooserDescriptorExtKt;
import com.jetbrains.python.run.AbstractPythonRunConfiguration;
import com.jetbrains.python.sdk.PreferredSdkComparator;
import com.jetbrains.python.sdk.PythonSdkType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FusionRunConfigurationEditor extends SettingsEditor<FusionRunConfiguration> implements PanelWithAnchor {

    private JPanel panel;
    private JComponent anchor;
    private TextFieldWithBrowseButton scriptTextField;
    private JRadioButton myUseModuleSdkRadioButton;
    private ModulesComboBox myModuleComboBox;
    private JRadioButton myUseSpecifiedSdkRadioButton;
    private ComboBox myInterpreterComboBox;

    public FusionRunConfigurationEditor(FusionRunConfiguration configuration) {
        FileChooserDescriptor chooserDescriptor = FileChooserDescriptorExtKt.withPythonFiles(
                FileChooserDescriptorFactory.createSingleFileDescriptor().withTitle("Select Script"), true);

        TextBrowseFolderListener listener =
                new TextBrowseFolderListener(chooserDescriptor, configuration.getProject()) {
                    @Nullable @Override protected VirtualFile getInitialFile() {
                        VirtualFile initialFile = super.getInitialFile();
                        if (initialFile != null) {
                            return initialFile;
                        }

                        return LocalFileSystem.getInstance().findFileByPath(configuration.getWorkingDirectory());
                    }
                };
        scriptTextField.addBrowseFolderListener(listener);

        List<Module> validPythonModules = AbstractPythonRunConfiguration.getValidModules(configuration.getProject());
        Collections.sort(validPythonModules, new ModulesAlphaComparator());
        Module selection = validPythonModules.size() > 0 ? validPythonModules.get(0) : null;

        myModuleComboBox.setModules(validPythonModules);
        myModuleComboBox.setSelectedModule(selection);

        myInterpreterComboBox.setMinimumAndPreferredWidth(100);
        myInterpreterComboBox.setRenderer(new SdkListCellRenderer("<Project Default>"));

        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateControls();
            }
        };
        myUseSpecifiedSdkRadioButton.addActionListener(actionListener);
        myUseModuleSdkRadioButton.addActionListener(actionListener);
        myInterpreterComboBox.addActionListener(actionListener);
        myModuleComboBox.addActionListener(actionListener);

        setSdkHome(null);
    }

    protected void resetEditorFrom(@NotNull FusionRunConfiguration configuration) {
        setScript(configuration.getScript());
        setSdkHome(configuration.getSdkHome());
        setUseModuleSdk(configuration.useModuleSdk());

        if (configuration.useModuleSdk()) {
            setModule(configuration.getModule());
        }

        updateControls();
    }

    protected void applyEditorTo(@NotNull FusionRunConfiguration configuration) throws ConfigurationException {
        configuration.setScript(getScript());
        configuration.setSdkHome(getSdkHome());
        configuration.setUseModuleSdk(isUseModuleSdk());

        if (isUseModuleSdk()) {
            configuration.setModule(getModule());
        }
    }

    @NotNull
    protected JComponent createEditor() {
        return panel;
    }

    @Override
    public JComponent getAnchor() {
        return anchor;
    }

    @Override
    public void setAnchor(JComponent anchor) {
        this.anchor = anchor;
    }

    public void setScript(String script) {
        scriptTextField.setText(script);
    }

    @Nullable
    public String getScript() {
        return scriptTextField.getText();
    }

    private void updateControls() {
        myModuleComboBox.setEnabled(myUseModuleSdkRadioButton.isSelected());
        myInterpreterComboBox.setEnabled(myUseSpecifiedSdkRadioButton.isSelected());
    }

    @Nullable
    public String getSdkHome() {
        Sdk selectedSdk = (Sdk)myInterpreterComboBox.getSelectedItem();
        return selectedSdk == null ? null : selectedSdk.getHomePath();
    }

    public void setSdkHome(String sdkHome) {
        List<Sdk> sdkList = new ArrayList<>();
        sdkList.add(null);
        final List<Sdk> allSdks = PythonSdkType.getAllSdks();
        Collections.sort(allSdks, new PreferredSdkComparator());
        Sdk selection = null;
        for (Sdk sdk : allSdks) {
            String homePath = sdk.getHomePath();
            if (homePath != null && sdkHome != null && FileUtil.pathsEqual(homePath, sdkHome)) selection = sdk;
            sdkList.add(sdk);
        }

        myInterpreterComboBox.setModel(new CollectionComboBoxModel(sdkList, selection));
    }

    public Module getModule() {
        return myModuleComboBox.getSelectedModule();
    }

    public void setModule(Module module) {
        myModuleComboBox.setSelectedModule(module);
    }

    public boolean isUseModuleSdk() {
        return myUseModuleSdkRadioButton.isSelected();
    }

    public void setUseModuleSdk(boolean useModuleSdk) {
        if (useModuleSdk) {
            myUseModuleSdkRadioButton.setSelected(true);
        }
        else {
            myUseSpecifiedSdkRadioButton.setSelected(true);
        }
        updateControls();
    }
}
