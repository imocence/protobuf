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

import static org.eclipse.jface.window.Window.OK;

import static com.google.eclipse.protobuf.ui.preferences.paths.Messages.add;
import static com.google.eclipse.protobuf.ui.preferences.paths.Messages.directories;
import static com.google.eclipse.protobuf.ui.preferences.paths.Messages.down;
import static com.google.eclipse.protobuf.ui.preferences.paths.Messages.remove;
import static com.google.eclipse.protobuf.ui.preferences.paths.Messages.up;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.xtext.ui.PluginImageHelper;

import com.google.eclipse.protobuf.ui.preferences.pages.DataChangedListener;

/**
 * Editor where users can add/remove the directories to be used for URI resolution.
 *
 * @author alruiz@google.com (Alex Ruiz)
 */
public class DirectoryPathsEditor extends Composite {
  private final IProject project;

  private final Table tblDirectoryPaths;
  private final TableViewer tblVwrDirectoryPaths;
  private final Button btnAdd;
  private final Button btnRemove;
  private final Button btnUp;
  private final Button btnDown;

  private final LinkedList<String> importPaths = new LinkedList<String>();

  private DataChangedListener dataChangedListener;

  public DirectoryPathsEditor(Composite parent, IProject project, PluginImageHelper imageHelper) {
    super(parent, SWT.NONE);
    this.project = project;

    // generated by WindowBuilder
    setLayout(new GridLayout(2, false));

    tblVwrDirectoryPaths = new TableViewer(this, SWT.BORDER | SWT.FULL_SELECTION);
    tblVwrDirectoryPaths.setLabelProvider(new RichLabelProvider(imageHelper));
    tblVwrDirectoryPaths.setContentProvider(ArrayContentProvider.getInstance());

    tblDirectoryPaths = tblVwrDirectoryPaths.getTable();
    tblDirectoryPaths.setHeaderVisible(true);
    tblDirectoryPaths.setLinesVisible(true);
    tblDirectoryPaths.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

    TableViewerColumn tblclmnVwrPath = new TableViewerColumn(tblVwrDirectoryPaths, SWT.NONE);
    TableColumn tblclmnPath = tblclmnVwrPath.getColumn();
    tblclmnPath.setWidth(290);
    tblclmnPath.setResizable(true);
    tblclmnPath.setText(directories);
    tblclmnVwrPath.setLabelProvider(new ColumnLabelProvider() {
      @Override public String getText(Object element) {
        return element.toString();
      }
    });

    Composite composite = new Composite(this, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
    composite.setLayout(new GridLayout(1, false));

    btnAdd = new Button(composite, SWT.NONE);
    btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    btnAdd.setText(add);

    btnRemove = new Button(composite, SWT.NONE);
    btnRemove.setEnabled(false);
    btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    btnRemove.setText(remove);

    new Label(composite, SWT.NONE);

    btnUp = new Button(composite, SWT.NONE);
    btnUp.setEnabled(false);
    btnUp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    btnUp.setText(up);

    btnDown = new Button(composite, SWT.NONE);
    btnDown.setEnabled(false);
    btnDown.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    btnDown.setText(down);

    addEventListeners();
  }

  private void addEventListeners() {
    tblDirectoryPaths.addSelectionListener(new SelectionAdapter() {
      @Override public void widgetSelected(SelectionEvent e) {
        enableButtonsDependingOnTableSelection();
      }
    });
    btnAdd.addSelectionListener(new SelectionAdapter() {
      @Override public void widgetSelected(SelectionEvent e) {
        AddDirectoryDialog dialog = new AddDirectoryDialog(getShell(), project);
        if (dialog.open() == OK) {
          importPaths.add(dialog.selectedPath());
          updateTable();
          enableButtonsDependingOnTableSelection();
          notifyDataHasChanged();
        }
      }
    });
    btnRemove.addSelectionListener(new SelectionAdapter() {
      @Override public void widgetSelected(SelectionEvent e) {
        int index = tblDirectoryPaths.getSelectionIndex();
        if (index < 0) {
          return;
        }
        importPaths.remove(index);
        updateTable();
        enableButtonsDependingOnTableSelection();
        notifyDataHasChanged();
      }
    });
    btnUp.addSelectionListener(new SelectionAdapter() {
      @Override public void widgetSelected(SelectionEvent e) {
        swap(true);
      }
    });
    btnDown.addSelectionListener(new SelectionAdapter() {
      @Override public void widgetSelected(SelectionEvent e) {
        swap(false);
      }
    });
  }

  private void swap(boolean goUp) {
    int index = tblDirectoryPaths.getSelectionIndex();
    if (index < 0) {
      return;
    }
    int target = goUp ? index - 1 : index + 1;
    int[] selection = tblDirectoryPaths.getSelectionIndices();
    String path = importPaths.get(selection[0]);
    importPaths.remove(index);
    importPaths.add(target, path);
    updateTable();
    tblDirectoryPaths.setSelection(target);
    enableButtonsDependingOnTableSelection();
  }

  /** {@inheritDoc} */
  @Override public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    tblDirectoryPaths.setEnabled(enabled);
    btnAdd.setEnabled(enabled);
    if (enabled) {
      enableButtonsDependingOnTableSelection();
    } else {
      btnRemove.setEnabled(false);
      btnUp.setEnabled(false);
      btnDown.setEnabled(false);
    }
  }

  private void enableButtonsDependingOnTableSelection() {
    int selectionIndex = tblDirectoryPaths.getSelectionIndex();
    int size = tblDirectoryPaths.getItemCount();
    boolean hasSelection = selectionIndex >= 0;
    btnRemove.setEnabled(hasSelection);
    boolean hasElements = size > 1;
    btnUp.setEnabled(hasElements && selectionIndex > 0);
    btnDown.setEnabled(hasElements && hasSelection && selectionIndex < size - 1);
  }

  public List<String> directoryPaths() {
    return unmodifiableList(importPaths);
  }

  public void directoryPaths(Collection<String> paths) {
    importPaths.clear();
    importPaths.addAll(paths);
    updateTable();
  }

  private void updateTable() {
    tblVwrDirectoryPaths.setInput(importPaths.toArray());
    if (tblDirectoryPaths.getItemCount() > 0 && tblDirectoryPaths.getSelectionCount() == 0) {
      tblDirectoryPaths.setSelection(0);
    }
  }

  public void setDataChangedListener(DataChangedListener listener) {
    dataChangedListener = listener;
  }

  private void notifyDataHasChanged() {
    if (dataChangedListener != null) {
      dataChangedListener.dataChanged();
    }
  }

  private static class RichLabelProvider extends LabelProvider implements ITableLabelProvider {
    private final PluginImageHelper imageHelper;

    RichLabelProvider(PluginImageHelper imageHelper) {
      this.imageHelper = imageHelper;
    }

    @Override public Image getImage(Object element) {
      return getColumnImage(element, 0);
    }

    @Override public Image getColumnImage(Object element, int columnIndex) {
      DirectoryPath path = (DirectoryPath) element;
      String imageName = (path.isWorkspacePath()) ? "workspace.gif" : "folder.gif";
      return imageHelper.getImage(imageName);
    }

    @Override public String getText(Object element) {
      return getColumnText(element, 0);
    }

    @Override public String getColumnText(Object element, int columnIndex) {
      DirectoryPath path = (DirectoryPath) element;
      return path.value();
    }
  }
}
