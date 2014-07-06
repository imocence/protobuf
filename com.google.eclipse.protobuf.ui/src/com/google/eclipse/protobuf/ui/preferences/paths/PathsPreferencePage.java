/*
 * Copyright (c) 2011 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.protobuf.ui.preferences.paths;

import static java.util.Collections.unmodifiableList;

import static org.eclipse.core.resources.IncrementalProjectBuilder.FULL_BUILD;
import static org.eclipse.core.runtime.Status.OK_STATUS;
import static org.eclipse.core.runtime.jobs.Job.BUILD;
import static org.eclipse.xtext.util.Strings.concat;
import static org.eclipse.xtext.util.Strings.isEmpty;
import static org.eclipse.xtext.util.Strings.split;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.eclipse.protobuf.ui.preferences.pages.ButtonGroup.with;
import static com.google.eclipse.protobuf.ui.preferences.pages.binding.BindingToButtonSelection.bindSelectionOf;
import static com.google.eclipse.protobuf.ui.preferences.paths.Messages.errorNoDirectoryNames;
import static com.google.eclipse.protobuf.ui.preferences.paths.Messages.filesInMultipleDirectories;
import static com.google.eclipse.protobuf.ui.preferences.paths.Messages.filesInOneDirectoryOnly;
import static com.google.eclipse.protobuf.ui.preferences.paths.Messages.importedFilesPathResolution;
import static com.google.eclipse.protobuf.ui.preferences.paths.Messages.rebuildProjectNow;
import static com.google.eclipse.protobuf.ui.preferences.paths.Messages.settingsChanged;
import static com.google.eclipse.protobuf.ui.preferences.paths.PreferenceNames.DIRECTORY_PATHS;
import static com.google.eclipse.protobuf.ui.preferences.paths.PreferenceNames.FILES_IN_MULTIPLE_DIRECTORIES;
import static com.google.eclipse.protobuf.ui.preferences.paths.PreferenceNames.FILES_IN_ONE_DIRECTORY_ONLY;
import static com.google.eclipse.protobuf.ui.util.IStatusFactory.error;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.xtext.ui.PluginImageHelper;

import com.google.eclipse.protobuf.ui.preferences.pages.DataChangedListener;
import com.google.eclipse.protobuf.ui.preferences.pages.PreferenceAndPropertyPage;
import com.google.eclipse.protobuf.ui.preferences.pages.binding.Binding;
import com.google.eclipse.protobuf.ui.preferences.pages.binding.Preference;
import com.google.eclipse.protobuf.ui.preferences.pages.binding.PreferenceBinder;
import com.google.eclipse.protobuf.ui.preferences.pages.binding.PreferenceFactory;
import com.google.eclipse.protobuf.ui.validation.ValidationTrigger;
import com.google.inject.Inject;

/**
 * Preference page for import paths.
 *
 * @author alruiz@google.com (Alex Ruiz)
 */
public class PathsPreferencePage extends PreferenceAndPropertyPage {
  private static final String COMMA_DELIMITER = ",";
  private static final String PREFERENCE_PAGE_ID = PathsPreferencePage.class.getName();

  private Group grpResolutionOfImported;
  private Button btnOneDirectoryOnly;
  private Button btnMultipleDirectories;
  private DirectoryPathsEditor directoryPathsEditor;

  @Inject private PluginImageHelper imageHelper;
  @Inject private ValidationTrigger validation;

  private boolean stateChanged;

  @Override protected Composite contentParent(Composite parent) {
    Composite contents = new Composite(parent, SWT.NONE);
    contents.setLayout(new GridLayout(3, false));
    return contents;
  }

  @Override protected void doCreateContents(Composite parent) {
    // generated by WindowBuilder
    grpResolutionOfImported = new Group(parent, SWT.NONE);
    grpResolutionOfImported.setLayout(new GridLayout(1, false));
    grpResolutionOfImported.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    grpResolutionOfImported.setText(importedFilesPathResolution);

    btnOneDirectoryOnly = new Button(grpResolutionOfImported, SWT.RADIO);
    btnOneDirectoryOnly.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
    btnOneDirectoryOnly.setText(filesInOneDirectoryOnly);

    btnMultipleDirectories = new Button(grpResolutionOfImported, SWT.RADIO);
    btnMultipleDirectories.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
    btnMultipleDirectories.setText(filesInMultipleDirectories);

    directoryPathsEditor = new DirectoryPathsEditor(grpResolutionOfImported, project(), imageHelper);
    directoryPathsEditor.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

    addEventListeners();
  }

  private void addEventListeners() {
    with(btnOneDirectoryOnly, btnMultipleDirectories).add(new SelectionAdapter() {
      @Override public void widgetSelected(SelectionEvent e) {
        boolean selected = btnMultipleDirectories.getSelection();
        directoryPathsEditor.setEnabled(selected);
        checkState();
      }
    });
    directoryPathsEditor.setDataChangedListener(new DataChangedListener() {
      @Override public void dataChanged() {
        checkState();
      }
    });
  }

  private void checkState() {
    stateChanged = true;
    if (directoryPathsEditor.isEnabled() && directoryPathsEditor.directoryPaths().isEmpty()) {
      pageIsNowInvalid(errorNoDirectoryNames);
      return;
    }
    pageIsNowValid();
  }

  @Override protected String enableProjectSettingsPreferenceName() {
    return null; // this page is always a "Project Properties" page
  }

  @Override protected void setupBinding(PreferenceBinder binder, PreferenceFactory factory) {
    binder.addAll(
        bindSelectionOf(btnOneDirectoryOnly).to(factory.newBooleanPreference(FILES_IN_ONE_DIRECTORY_ONLY)),
        bindSelectionOf(btnMultipleDirectories).to(factory.newBooleanPreference(FILES_IN_MULTIPLE_DIRECTORIES))
      );
    final Preference<String> directoryPaths = factory.newStringPreference(DIRECTORY_PATHS);
    binder.add(new Binding() {
      @Override public void applyPreferenceValueToTarget() {
        setDirectoryPaths(directoryPaths.value());
      }

      @Override public void applyDefaultPreferenceValueToTarget() {
        setDirectoryPaths(directoryPaths.defaultValue());
      }

      @Override public void savePreferenceValue() {
        directoryPaths.updateValue(directoryNames());
      }
    });
  }

  private String directoryNames() {
    List<String> paths = directoryPathsEditor.directoryPaths();
    if (paths.isEmpty())
     {
      return "";
    }
    return concat(COMMA_DELIMITER, paths);
  }

  private void setDirectoryPaths(String directoryPaths) {
    List<String> paths = newArrayList();
    for (String path : split(directoryPaths, COMMA_DELIMITER)) {
      if (!isEmpty(path)) {
        paths.add(path);
      }
    }
    directoryPathsEditor.directoryPaths(unmodifiableList(paths));
  }

  /** {@inheritDoc} */
  @Override protected void onProjectSettingsActivation(boolean active) {
    enableProjectOptions(active);
  }

  @Override protected void updateContents() {
    enableProjectOptions(true);
  }

  private void enableProjectOptions(boolean enabled) {
    grpResolutionOfImported.setEnabled(enabled);
    btnOneDirectoryOnly.setEnabled(enabled);
    btnMultipleDirectories.setEnabled(enabled);
    directoryPathsEditor.setEnabled(btnMultipleDirectories.getSelection() && enabled);
  }

  @Override protected String preferencePageId() {
    return PREFERENCE_PAGE_ID;
  }

  @Override protected void okPerformed() {
    // TODO check threading
    if (!stateChanged) {
      return;
    }
    stateChanged = false;
    if (shouldRebuild()) {
      rebuildProject();
      return;
    }
    validation.validateOpenEditors(project());
  }

  private boolean shouldRebuild() {
    MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
    messageBox.setText(settingsChanged);
    messageBox.setMessage(rebuildProjectNow);
    return messageBox.open() == SWT.YES;
  }

  private void rebuildProject() {
    Job job = new Job("Rebuilding project") {
      @Override protected IStatus run(IProgressMonitor monitor) {
        try {
          project().build(FULL_BUILD, monitor);
        } catch (CoreException e) {
          return error(e);
        }
        return OK_STATUS;
      }
    };
    job.setPriority(BUILD);
    job.schedule(); // start as soon as possible
  }
}
