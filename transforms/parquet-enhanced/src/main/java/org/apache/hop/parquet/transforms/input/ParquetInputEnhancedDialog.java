/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.parquet.transforms.input;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.hop.core.Const;
import org.apache.hop.core.Props;
import org.apache.hop.core.RowMetaAndData;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.logging.LogChannel;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.row.value.ValueMetaFactory;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.core.vfs.HopVfs;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransformMeta;
import org.apache.hop.pipeline.transform.ITransformDialog;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.dialog.BaseDialog;
import org.apache.hop.ui.core.dialog.EnterSelectionDialog;
import org.apache.hop.ui.core.dialog.ErrorDialog;
import org.apache.hop.ui.core.dialog.MessageBox;
import org.apache.hop.ui.core.gui.GuiResource;
import org.apache.hop.ui.core.gui.WindowProperty;
import org.apache.hop.ui.core.widget.ColumnInfo;
import org.apache.hop.ui.core.widget.TableView;
import org.apache.hop.ui.core.widget.TextVar;
import org.apache.hop.ui.pipeline.transform.BaseTransformDialog;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class ParquetInputEnhancedDialog extends BaseTransformDialog implements ITransformDialog {

  public static final Class<?> PKG = ParquetInputEnhancedMeta.class;

  //  public static final String SYSTEM_COMBO_YES = "System.Combo.Yes";
  //  public static final String SYSTEM_COMBO_NO = "System.Combo.No";
  public static final String SYSTEM_DIALOG_ERROR_TITLE = "System.Dialog.Error.Title";

  private static final String[] YES_NO_COMBO =
      new String[] {
        BaseMessages.getString(PKG, "System.Combo.No"),
        BaseMessages.getString(PKG, "System.Combo.Yes")
      };

  protected ParquetInputEnhancedMeta input;
  private ModifyListener lsMod;

  private TableView wFields;
  private Label wlFilename;
  private Button wbbFilename; // Browse: add file or directory
  private Button wbdFilename; // Delete
  private Button wbeFilename; // Edit
  private Button wbaFilename; // Add or change
  private TextVar wFilename;

  private Label wlFilenameList;
  private TableView wFilenameList;

  private Label wlFilemask;
  private TextVar wFilemask;
  private Label wlExcludeFilemask;
  private TextVar wExcludeFilemask;
  private Label wlAddFileResult;
  private Button wAddFileResult;

  private Button wbShowFiles;
  private Button wAccFilenames;

  // private Label wlPassThruFields;
  // private Button wPassThruFields;

  private Label wlAccField;
  private CCombo wAccField;

  private Label wlAccTransform;
  private CCombo wAccTransform;
  private Text wInclFilenameField;
  private Text wInclRowNumField;
  private Text wShortFileFieldName;
  private Text wPathFieldName;

  private Text wIsHiddenName;
  private Text wLastModificationTimeName;
  private Text wUriName;
  private Text wRootUriName;
  private Text wExtensionFieldName;
  private Text wSizeFieldName;

  private String returnValue;

  public ParquetInputEnhancedDialog(
      Shell parent,
      IVariables variables,
      Object in,
      PipelineMeta pipelineMeta,
      String transformName) {
    super(parent, variables, (BaseTransformMeta) in, pipelineMeta, transformName);
    input = (ParquetInputEnhancedMeta) in;
  }

  @Override
  public String open() {

    Shell parent = getParent();

    shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
    PropsUi.setLook(shell);
    setShellImage(shell, input);
    lsMod = e -> input.setChanged();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = PropsUi.getFormMargin();
    formLayout.marginHeight = PropsUi.getFormMargin();

    shell.setLayout(formLayout);
    shell.setText(BaseMessages.getString(PKG, "ParquetInput.Name"));

    int middle = props.getMiddlePct();
    int margin = props.getMargin();

    // Some buttons at the bottom
    wOk = new Button(shell, SWT.PUSH);
    wOk.setText(BaseMessages.getString(PKG, "System.Button.OK"));
    wOk.addListener(SWT.Selection, e -> ok());
    wGet = new Button(shell, SWT.PUSH);
    wGet.setText(BaseMessages.getString(PKG, "System.Button.GetFields"));
    wGet.addListener(SWT.Selection, e -> getFields());
    wCancel = new Button(shell, SWT.PUSH);
    wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));
    wCancel.addListener(SWT.Selection, e -> cancel());
    setButtonPositions(new Button[] {wOk, wGet, wCancel}, margin, null);

    // TransformName line
    wlTransformName = new Label(shell, SWT.RIGHT);
    wlTransformName.setText(BaseMessages.getString(PKG, "ParquetInputDialog.TransformName.Label"));
    PropsUi.setLook(wlTransformName);
    fdlTransformName = new FormData();
    fdlTransformName.left = new FormAttachment(0, 0);
    fdlTransformName.right = new FormAttachment(middle, -margin);
    fdlTransformName.top = new FormAttachment(0, margin);
    wlTransformName.setLayoutData(fdlTransformName);
    wTransformName = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wTransformName.setText(transformName);
    PropsUi.setLook(wTransformName);
    fdTransformName = new FormData();
    fdTransformName.left = new FormAttachment(middle, 0);
    fdTransformName.top = new FormAttachment(wlTransformName, 0, SWT.CENTER);
    fdTransformName.right = new FormAttachment(100, 0);
    wTransformName.setLayoutData(fdTransformName);

    CTabFolder wTabFolder = new CTabFolder(shell, SWT.BORDER);
    PropsUi.setLook(wTabFolder, Props.WIDGET_STYLE_TAB);

    addFilesTab(wTabFolder, middle, margin);
    addContentTab(wTabFolder, middle, margin);
    addFieldsTab(wTabFolder, middle, margin);
    addAdditionalFieldsTab(wTabFolder, middle, margin);

    FormData fdTabFolder = new FormData();
    fdTabFolder.left = new FormAttachment(0, 0);
    fdTabFolder.top = new FormAttachment(wTransformName, margin);
    fdTabFolder.right = new FormAttachment(100, 0);
    fdTabFolder.bottom = new FormAttachment(wOk, -2 * margin);
    wTabFolder.setLayoutData(fdTabFolder);

    wGet.addListener(SWT.Selection, e -> get());

    // Add the file to the list of files...
    SelectionAdapter selA =
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent arg0) {
            if (wFilename.getText() == null || wFilename.getText().isEmpty()) {
              displayErrorDialog(
                  new HopException(
                      BaseMessages.getString(
                          PKG, "ParquetInputDialog.ErrorAddingFile.ErrorMessage")),
                  "ParquetInputDialog.ErrorAddingFile.DialogMessage");
              return;
            }
            wFilenameList.add(
                wFilename.getText(),
                wFilemask.getText(),
                wExcludeFilemask.getText(),
                ParquetInputEnhancedMeta.RequiredFilesCode[0],
                ParquetInputEnhancedMeta.RequiredFilesCode[0]);
            wFilename.setText("");
            wFilemask.setText("");
            wExcludeFilemask.setText("");
            wFilenameList.removeEmptyRows();
            wFilenameList.setRowNums();
            wFilenameList.optWidth(true);
            // checkCompressedFile();
          }
        };
    wbaFilename.addSelectionListener(selA);
    wFilename.addSelectionListener(selA);

    // Delete files from the list of files...
    wbdFilename.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent arg0) {
            int[] idx = wFilenameList.getSelectionIndices();
            wFilenameList.remove(idx);
            wFilenameList.removeEmptyRows();
            wFilenameList.setRowNums();
            // checkCompressedFile();
          }
        });

    // Edit the selected file & remove from the list...
    wbeFilename.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent arg0) {
            int idx = wFilenameList.getSelectionIndex();
            if (idx >= 0) {
              String[] string = wFilenameList.getItem(idx);
              wFilename.setText(string[0]);
              wFilemask.setText(string[1]);
              wExcludeFilemask.setText(string[2]);
              wFilenameList.remove(idx);
            }
            wFilenameList.removeEmptyRows();
            wFilenameList.setRowNums();
          }
        });

    // Show the files that are selected at this time...
    wbShowFiles.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            showFiles();
          }
        });

    SelectionAdapter lsFlags =
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            setFlags();
          }
        };

    // Enable/disable the right fields...
    wAccFilenames.addSelectionListener(lsFlags);

    wbbFilename.addListener(
        SWT.Selection,
        e ->
            BaseDialog.presentFileDialog(
                shell,
                wFilename,
                variables,
                new String[] {"*.parquet", "*"},
                new String[] {
                  BaseMessages.getString(PKG, "ParquetInput.FileType.CSVFiles"),
                  BaseMessages.getString(PKG, "System.FileType.AllFiles")
                },
                true));

    getData(input);

    wTabFolder.setSelection(0);

    BaseDialog.defaultShellHandling(shell, c -> ok(), c -> cancel());
    return returnValue;
  }

  private void addContentTab(CTabFolder wTabFolder, int middle, int margin) {

    // ////////////////////////
    // START OF CONTENT TAB///
    // /
    CTabItem wContentTab = new CTabItem(wTabFolder, SWT.NONE);
    wContentTab.setFont(GuiResource.getInstance().getFontDefault());
    wContentTab.setText(BaseMessages.getString(PKG, "ParquetInputDialog.ContentTab.TabTitle"));

    FormLayout contentLayout = new FormLayout();
    contentLayout.marginWidth = 3;
    contentLayout.marginHeight = 3;

    ScrolledComposite wContentSComp =
        new ScrolledComposite(wTabFolder, SWT.V_SCROLL | SWT.H_SCROLL);
    wContentSComp.setLayout(new FillLayout());

    Composite wContentComp = new Composite(wContentSComp, SWT.NONE);
    PropsUi.setLook(wContentComp);
    wContentComp.setLayout(contentLayout);

    // ///////////////////////////////
    // START OF AddFileResult GROUP //
    // ///////////////////////////////

    Group gAddFileResult = new Group(wContentComp, SWT.SHADOW_NONE);
    PropsUi.setLook(gAddFileResult);
    gAddFileResult.setText(BaseMessages.getString(PKG, "ParquetInputDialog.gAddFileResult.Label"));

    FormLayout addFileResultGroupLayout = new FormLayout();
    addFileResultGroupLayout.marginWidth = 10;
    addFileResultGroupLayout.marginHeight = 10;
    gAddFileResult.setLayout(addFileResultGroupLayout);

    wlAddFileResult = new Label(gAddFileResult, SWT.RIGHT);
    wlAddFileResult.setText(BaseMessages.getString(PKG, "ParquetInputDialog.AddFileResult.Label"));
    PropsUi.setLook(wlAddFileResult);
    FormData fdlAddFileResult = new FormData();
    fdlAddFileResult.left = new FormAttachment(0, 0);
    fdlAddFileResult.top = new FormAttachment(wFilenameList, margin);
    fdlAddFileResult.right = new FormAttachment(middle, -margin);
    wlAddFileResult.setLayoutData(fdlAddFileResult);

    wAddFileResult = new Button(gAddFileResult, SWT.CHECK);
    PropsUi.setLook(wAddFileResult);
    wAddFileResult.setToolTipText(
        BaseMessages.getString(PKG, "ParquetInputDialog.AddFileResult.Tooltip"));
    FormData fdAddFileResult = new FormData();
    fdAddFileResult.left = new FormAttachment(middle, 0);
    fdAddFileResult.top = new FormAttachment(wlAddFileResult, 0, SWT.CENTER);
    wAddFileResult.setLayoutData(fdAddFileResult);

    FormData fdgAddFileResult = new FormData();
    fdgAddFileResult.left = new FormAttachment(0, margin);
    fdgAddFileResult.top = new FormAttachment(0, margin);
    fdgAddFileResult.right = new FormAttachment(100, -margin);
    gAddFileResult.setLayoutData(fdgAddFileResult);

    wContentComp.pack();
    // What's the size:
    Rectangle bounds = wContentComp.getBounds();

    wContentSComp.setContent(wContentComp);
    wContentSComp.setExpandHorizontal(true);
    wContentSComp.setExpandVertical(true);
    wContentSComp.setMinWidth(bounds.width);
    wContentSComp.setMinHeight(bounds.height);

    FormData fdContentComp = new FormData();
    fdContentComp.left = new FormAttachment(0, 0);
    fdContentComp.top = new FormAttachment(0, 0);
    fdContentComp.right = new FormAttachment(100, 0);
    fdContentComp.bottom = new FormAttachment(100, 0);
    wContentComp.setLayoutData(fdContentComp);

    wContentTab.setControl(wContentSComp);
  }

  private void addAdditionalFieldsTab(CTabFolder wTabFolder, int middle, int margin) {
    //
    // START OF ADDITIONAL FIELDS TAB /
    //
    CTabItem wAdditionalFieldsTab = new CTabItem(wTabFolder, SWT.NONE);
    wAdditionalFieldsTab.setFont(GuiResource.getInstance().getFontDefault());
    wAdditionalFieldsTab.setText(
        BaseMessages.getString(PKG, "ParquetInputDialog.AdditionalFieldsTab.TabTitle"));

    Composite wAdditionalFieldsComp = new Composite(wTabFolder, SWT.NONE);
    PropsUi.setLook(wAdditionalFieldsComp);

    FormLayout fieldsLayout = new FormLayout();
    fieldsLayout.marginWidth = 3;
    fieldsLayout.marginHeight = 3;
    wAdditionalFieldsComp.setLayout(fieldsLayout);

    Label wlInclFilenameField = new Label(wAdditionalFieldsComp, SWT.RIGHT);
    wlInclFilenameField.setText(
        BaseMessages.getString(PKG, "ParquetInputDialog.InclFilenameField.Label"));
    PropsUi.setLook(wlInclFilenameField);
    FormData fdlInclFilenameField = new FormData();
    fdlInclFilenameField.left = new FormAttachment(0, 0);
    fdlInclFilenameField.top = new FormAttachment(wTransformName, margin);
    fdlInclFilenameField.right = new FormAttachment(middle, -margin);
    wlInclFilenameField.setLayoutData(fdlInclFilenameField);
    wInclFilenameField = new Text(wAdditionalFieldsComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wInclFilenameField);
    FormData fdInclFilenameField = new FormData();
    fdInclFilenameField.left = new FormAttachment(middle, 0);
    fdInclFilenameField.top = new FormAttachment(wTransformName, margin);
    fdInclFilenameField.right = new FormAttachment(100, 0);
    wInclFilenameField.setLayoutData(fdInclFilenameField);

    // ShortFileFieldName line
    Label wlShortFileFieldName = new Label(wAdditionalFieldsComp, SWT.RIGHT);
    wlShortFileFieldName.setText(
        BaseMessages.getString(PKG, "ParquetInputDialog.ShortFileFieldName.Label"));
    PropsUi.setLook(wlShortFileFieldName);
    FormData fdlShortFileFieldName = new FormData();
    fdlShortFileFieldName.left = new FormAttachment(0, 0);
    fdlShortFileFieldName.top = new FormAttachment(wInclRowNumField, margin);
    fdlShortFileFieldName.right = new FormAttachment(middle, -margin);
    wlShortFileFieldName.setLayoutData(fdlShortFileFieldName);

    wShortFileFieldName = new Text(wAdditionalFieldsComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wShortFileFieldName);
    FormData fdShortFileFieldName = new FormData();
    fdShortFileFieldName.left = new FormAttachment(middle, 0);
    fdShortFileFieldName.right = new FormAttachment(100, -margin);
    fdShortFileFieldName.top = new FormAttachment(wInclRowNumField, margin);
    wShortFileFieldName.setLayoutData(fdShortFileFieldName);

    // ExtensionFieldName line
    Label wlExtensionFieldName = new Label(wAdditionalFieldsComp, SWT.RIGHT);
    wlExtensionFieldName.setText(
        BaseMessages.getString(PKG, "ParquetInputDialog.ExtensionFieldName.Label"));
    PropsUi.setLook(wlExtensionFieldName);
    FormData fdlExtensionFieldName = new FormData();
    fdlExtensionFieldName.left = new FormAttachment(0, 0);
    fdlExtensionFieldName.top = new FormAttachment(wShortFileFieldName, margin);
    fdlExtensionFieldName.right = new FormAttachment(middle, -margin);
    wlExtensionFieldName.setLayoutData(fdlExtensionFieldName);

    wExtensionFieldName = new Text(wAdditionalFieldsComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wExtensionFieldName);
    FormData fdExtensionFieldName = new FormData();
    fdExtensionFieldName.left = new FormAttachment(middle, 0);
    fdExtensionFieldName.right = new FormAttachment(100, -margin);
    fdExtensionFieldName.top = new FormAttachment(wShortFileFieldName, margin);
    wExtensionFieldName.setLayoutData(fdExtensionFieldName);

    // PathFieldName line
    Label wlPathFieldName = new Label(wAdditionalFieldsComp, SWT.RIGHT);
    wlPathFieldName.setText(BaseMessages.getString(PKG, "ParquetInputDialog.PathFieldName.Label"));
    PropsUi.setLook(wlPathFieldName);
    FormData fdlPathFieldName = new FormData();
    fdlPathFieldName.left = new FormAttachment(0, 0);
    fdlPathFieldName.top = new FormAttachment(wExtensionFieldName, margin);
    fdlPathFieldName.right = new FormAttachment(middle, -margin);
    wlPathFieldName.setLayoutData(fdlPathFieldName);

    wPathFieldName = new Text(wAdditionalFieldsComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wPathFieldName);
    FormData fdPathFieldName = new FormData();
    fdPathFieldName.left = new FormAttachment(middle, 0);
    fdPathFieldName.right = new FormAttachment(100, -margin);
    fdPathFieldName.top = new FormAttachment(wExtensionFieldName, margin);
    wPathFieldName.setLayoutData(fdPathFieldName);

    // SizeFieldName line
    Label wlSizeFieldName = new Label(wAdditionalFieldsComp, SWT.RIGHT);
    wlSizeFieldName.setText(BaseMessages.getString(PKG, "ParquetInputDialog.SizeFieldName.Label"));
    PropsUi.setLook(wlSizeFieldName);
    FormData fdlSizeFieldName = new FormData();
    fdlSizeFieldName.left = new FormAttachment(0, 0);
    fdlSizeFieldName.top = new FormAttachment(wPathFieldName, margin);
    fdlSizeFieldName.right = new FormAttachment(middle, -margin);
    wlSizeFieldName.setLayoutData(fdlSizeFieldName);

    wSizeFieldName = new Text(wAdditionalFieldsComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wSizeFieldName);
    FormData fdSizeFieldName = new FormData();
    fdSizeFieldName.left = new FormAttachment(middle, 0);
    fdSizeFieldName.right = new FormAttachment(100, -margin);
    fdSizeFieldName.top = new FormAttachment(wPathFieldName, margin);
    wSizeFieldName.setLayoutData(fdSizeFieldName);

    // IsHiddenName line
    Label wlIsHiddenName = new Label(wAdditionalFieldsComp, SWT.RIGHT);
    wlIsHiddenName.setText(BaseMessages.getString(PKG, "ParquetInputDialog.IsHiddenName.Label"));
    PropsUi.setLook(wlIsHiddenName);
    FormData fdlIsHiddenName = new FormData();
    fdlIsHiddenName.left = new FormAttachment(0, 0);
    fdlIsHiddenName.top = new FormAttachment(wSizeFieldName, margin);
    fdlIsHiddenName.right = new FormAttachment(middle, -margin);
    wlIsHiddenName.setLayoutData(fdlIsHiddenName);

    wIsHiddenName = new Text(wAdditionalFieldsComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wIsHiddenName);
    FormData fdIsHiddenName = new FormData();
    fdIsHiddenName.left = new FormAttachment(middle, 0);
    fdIsHiddenName.right = new FormAttachment(100, -margin);
    fdIsHiddenName.top = new FormAttachment(wSizeFieldName, margin);
    wIsHiddenName.setLayoutData(fdIsHiddenName);

    // LastModificationTimeName line
    Label wlLastModificationTimeName = new Label(wAdditionalFieldsComp, SWT.RIGHT);
    wlLastModificationTimeName.setText(
        BaseMessages.getString(PKG, "ParquetInputDialog.LastModificationTimeName.Label"));
    PropsUi.setLook(wlLastModificationTimeName);
    FormData fdlLastModificationTimeName = new FormData();
    fdlLastModificationTimeName.left = new FormAttachment(0, 0);
    fdlLastModificationTimeName.top = new FormAttachment(wIsHiddenName, margin);
    fdlLastModificationTimeName.right = new FormAttachment(middle, -margin);
    wlLastModificationTimeName.setLayoutData(fdlLastModificationTimeName);

    wLastModificationTimeName = new Text(wAdditionalFieldsComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wLastModificationTimeName);
    FormData fdLastModificationTimeName = new FormData();
    fdLastModificationTimeName.left = new FormAttachment(middle, 0);
    fdLastModificationTimeName.right = new FormAttachment(100, -margin);
    fdLastModificationTimeName.top = new FormAttachment(wIsHiddenName, margin);
    wLastModificationTimeName.setLayoutData(fdLastModificationTimeName);

    // UriName line
    Label wlUriName = new Label(wAdditionalFieldsComp, SWT.RIGHT);
    wlUriName.setText(BaseMessages.getString(PKG, "ParquetInputDialog.UriName.Label"));
    PropsUi.setLook(wlUriName);
    FormData fdlUriName = new FormData();
    fdlUriName.left = new FormAttachment(0, 0);
    fdlUriName.top = new FormAttachment(wLastModificationTimeName, margin);
    fdlUriName.right = new FormAttachment(middle, -margin);
    wlUriName.setLayoutData(fdlUriName);

    wUriName = new Text(wAdditionalFieldsComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wUriName);
    FormData fdUriName = new FormData();
    fdUriName.left = new FormAttachment(middle, 0);
    fdUriName.right = new FormAttachment(100, -margin);
    fdUriName.top = new FormAttachment(wLastModificationTimeName, margin);
    wUriName.setLayoutData(fdUriName);

    // RootUriName line
    Label wlRootUriName = new Label(wAdditionalFieldsComp, SWT.RIGHT);
    wlRootUriName.setText(BaseMessages.getString(PKG, "ParquetInputDialog.RootUriName.Label"));
    PropsUi.setLook(wlRootUriName);
    FormData fdlRootUriName = new FormData();
    fdlRootUriName.left = new FormAttachment(0, 0);
    fdlRootUriName.top = new FormAttachment(wUriName, margin);
    fdlRootUriName.right = new FormAttachment(middle, -margin);
    wlRootUriName.setLayoutData(fdlRootUriName);

    wRootUriName = new Text(wAdditionalFieldsComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wRootUriName);
    FormData fdRootUriName = new FormData();
    fdRootUriName.left = new FormAttachment(middle, 0);
    fdRootUriName.right = new FormAttachment(100, -margin);
    fdRootUriName.top = new FormAttachment(wUriName, margin);
    wRootUriName.setLayoutData(fdRootUriName);

    FormData fdAdditionalFieldsComp = new FormData();
    fdAdditionalFieldsComp.left = new FormAttachment(0, 0);
    fdAdditionalFieldsComp.top = new FormAttachment(wTransformName, margin);
    fdAdditionalFieldsComp.right = new FormAttachment(100, 0);
    fdAdditionalFieldsComp.bottom = new FormAttachment(100, 0);
    wAdditionalFieldsComp.setLayoutData(fdAdditionalFieldsComp);

    wAdditionalFieldsComp.layout();
    wAdditionalFieldsTab.setControl(wAdditionalFieldsComp);

    //
    // / END OF ADDITIONAL FIELDS TAB
    //
  }

  private void addFilesTab(CTabFolder wTabFolder, int middle, int margin) {
    // ////////////////////////
    // START OF FILE TAB ///
    // ////////////////////////

    CTabItem wFileTab = new CTabItem(wTabFolder, SWT.NONE);
    wFileTab.setFont(GuiResource.getInstance().getFontDefault());
    wFileTab.setText(BaseMessages.getString(PKG, "ParquetInputDialog.FileTab.TabTitle"));

    ScrolledComposite wFileSComp = new ScrolledComposite(wTabFolder, SWT.V_SCROLL | SWT.H_SCROLL);
    wFileSComp.setLayout(new FillLayout());

    Composite wFileComp = new Composite(wFileSComp, SWT.NONE);
    PropsUi.setLook(wFileComp);

    FormLayout fileLayout = new FormLayout();
    fileLayout.marginWidth = 3;
    fileLayout.marginHeight = 3;
    wFileComp.setLayout(fileLayout);

    // Filename line
    wlFilename = new Label(wFileComp, SWT.RIGHT);
    wlFilename.setText(BaseMessages.getString(PKG, "ParquetInputDialog.Filename.Label"));
    PropsUi.setLook(wlFilename);
    FormData fdlFilename = new FormData();
    fdlFilename.left = new FormAttachment(0, 0);
    fdlFilename.top = new FormAttachment(0, 0);
    fdlFilename.right = new FormAttachment(middle, -margin);
    wlFilename.setLayoutData(fdlFilename);

    wbbFilename = new Button(wFileComp, SWT.PUSH | SWT.CENTER);
    PropsUi.setLook(wbbFilename);
    wbbFilename.setText(BaseMessages.getString(PKG, "System.Button.Browse"));
    wbbFilename.setToolTipText(
        BaseMessages.getString(PKG, "System.Tooltip.BrowseForFileOrDirAndAdd"));
    FormData fdbFilename = new FormData();
    fdbFilename.right = new FormAttachment(100, 0);
    fdbFilename.top = new FormAttachment(0, 0);
    wbbFilename.setLayoutData(fdbFilename);

    wbaFilename = new Button(wFileComp, SWT.PUSH | SWT.CENTER);
    PropsUi.setLook(wbaFilename);
    wbaFilename.setText(BaseMessages.getString(PKG, "ParquetInputDialog.FilenameAdd.Button"));
    wbaFilename.setToolTipText(
        BaseMessages.getString(PKG, "ParquetInputDialog.FilenameAdd.Tooltip"));
    FormData fdbaFilename = new FormData();
    fdbaFilename.right = new FormAttachment(wbbFilename, -margin);
    fdbaFilename.top = new FormAttachment(0, 0);
    wbaFilename.setLayoutData(fdbaFilename);

    wFilename = new TextVar(variables, wFileComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wFilename);
    wFilename.addModifyListener(lsMod);
    FormData fdFilename = new FormData();
    fdFilename.left = new FormAttachment(middle, 0);
    fdFilename.right = new FormAttachment(wbaFilename, -margin);
    fdFilename.top = new FormAttachment(0, 0);
    wFilename.setLayoutData(fdFilename);

    wlFilemask = new Label(wFileComp, SWT.RIGHT);
    wlFilemask.setText(BaseMessages.getString(PKG, "ParquetInputDialog.Filemask.Label"));
    PropsUi.setLook(wlFilemask);
    FormData fdlFilemask = new FormData();
    fdlFilemask.left = new FormAttachment(0, 0);
    fdlFilemask.top = new FormAttachment(wFilename, margin);
    fdlFilemask.right = new FormAttachment(middle, -margin);
    wlFilemask.setLayoutData(fdlFilemask);

    wFilemask = new TextVar(variables, wFileComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);

    PropsUi.setLook(wFilemask);
    wFilemask.addModifyListener(lsMod);
    FormData fdFilemask = new FormData();
    fdFilemask.left = new FormAttachment(middle, 0);
    fdFilemask.top = new FormAttachment(wFilename, margin);
    fdFilemask.right = new FormAttachment(wbaFilename, -margin);
    wFilemask.setLayoutData(fdFilemask);

    wlExcludeFilemask = new Label(wFileComp, SWT.RIGHT);
    wlExcludeFilemask.setText(
        BaseMessages.getString(PKG, "ParquetInputDialog.ExcludeFilemask.Label"));
    PropsUi.setLook(wlExcludeFilemask);
    FormData fdlExcludeFilemask = new FormData();
    fdlExcludeFilemask.left = new FormAttachment(0, 0);
    fdlExcludeFilemask.top = new FormAttachment(wFilemask, margin);
    fdlExcludeFilemask.right = new FormAttachment(middle, -margin);
    wlExcludeFilemask.setLayoutData(fdlExcludeFilemask);

    wExcludeFilemask = new TextVar(variables, wFileComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wExcludeFilemask);
    wExcludeFilemask.addModifyListener(lsMod);
    FormData fdExcludeFilemask = new FormData();
    fdExcludeFilemask.left = new FormAttachment(middle, 0);
    fdExcludeFilemask.top = new FormAttachment(wFilemask, margin);
    fdExcludeFilemask.right = new FormAttachment(wFilename, 0, SWT.RIGHT);
    wExcludeFilemask.setLayoutData(fdExcludeFilemask);

    // Filename list line
    wlFilenameList = new Label(wFileComp, SWT.RIGHT);
    wlFilenameList.setText(BaseMessages.getString(PKG, "ParquetInputDialog.FilenameList.Label"));
    PropsUi.setLook(wlFilenameList);
    FormData fdlFilenameList = new FormData();
    fdlFilenameList.left = new FormAttachment(0, 0);
    fdlFilenameList.top = new FormAttachment(wExcludeFilemask, margin);
    fdlFilenameList.right = new FormAttachment(middle, -margin);
    wlFilenameList.setLayoutData(fdlFilenameList);

    // Buttons to the right of the screen...
    wbdFilename = new Button(wFileComp, SWT.PUSH | SWT.CENTER);
    PropsUi.setLook(wbdFilename);
    wbdFilename.setText(BaseMessages.getString(PKG, "ParquetInputDialog.FilenameDelete.Button"));
    wbdFilename.setToolTipText(
        BaseMessages.getString(PKG, "ParquetInputDialog.FilenameDelete.Tooltip"));
    FormData fdbdFilename = new FormData();
    fdbdFilename.right = new FormAttachment(100, 0);
    fdbdFilename.top = new FormAttachment(wExcludeFilemask, margin);
    wbdFilename.setLayoutData(fdbdFilename);

    wbeFilename = new Button(wFileComp, SWT.PUSH | SWT.CENTER);
    PropsUi.setLook(wbeFilename);
    wbeFilename.setText(BaseMessages.getString(PKG, "ParquetInputDialog.FilenameEdit.Button"));
    wbeFilename.setToolTipText(
        BaseMessages.getString(PKG, "ParquetInputDialog.FilenameEdit.Tooltip"));
    FormData fdbeFilename = new FormData();
    fdbeFilename.right = new FormAttachment(100, 0);
    fdbeFilename.left = new FormAttachment(wbdFilename, 0, SWT.LEFT);
    fdbeFilename.top = new FormAttachment(wbdFilename, margin);
    wbeFilename.setLayoutData(fdbeFilename);

    wbShowFiles = new Button(wFileComp, SWT.PUSH | SWT.CENTER);
    PropsUi.setLook(wbShowFiles);
    wbShowFiles.setText(BaseMessages.getString(PKG, "ParquetInputDialog.ShowFiles.Button"));
    FormData fdbShowFiles = new FormData();
    fdbShowFiles.left = new FormAttachment(middle, 0);
    fdbShowFiles.bottom = new FormAttachment(100, 0);
    wbShowFiles.setLayoutData(fdbShowFiles);

    // Accepting filenames group
    //

    Group gAccepting = new Group(wFileComp, SWT.SHADOW_ETCHED_IN);
    gAccepting.setText(BaseMessages.getString(PKG, "ParquetInputDialog.AcceptingGroup.Label"));
    FormLayout acceptingLayout = new FormLayout();
    acceptingLayout.marginWidth = 3;
    acceptingLayout.marginHeight = 3;
    gAccepting.setLayout(acceptingLayout);
    PropsUi.setLook(gAccepting);

    // Accept filenames from previous transforms?
    //
    Label wlAccFilenames = new Label(gAccepting, SWT.RIGHT);
    wlAccFilenames.setText(BaseMessages.getString(PKG, "ParquetInputDialog.AcceptFilenames.Label"));
    PropsUi.setLook(wlAccFilenames);
    FormData fdlAccFilenames = new FormData();
    fdlAccFilenames.top = new FormAttachment(0, margin);
    fdlAccFilenames.left = new FormAttachment(0, 0);
    fdlAccFilenames.right = new FormAttachment(middle, -margin);
    wlAccFilenames.setLayoutData(fdlAccFilenames);
    wAccFilenames = new Button(gAccepting, SWT.CHECK);
    wAccFilenames.setToolTipText(
        BaseMessages.getString(PKG, "ParquetInputDialog.AcceptFilenames.Tooltip"));
    PropsUi.setLook(wAccFilenames);
    FormData fdAccFilenames = new FormData();
    fdAccFilenames.top = new FormAttachment(wlAccFilenames, 0, SWT.CENTER);
    fdAccFilenames.left = new FormAttachment(middle, 0);
    fdAccFilenames.right = new FormAttachment(100, 0);
    wAccFilenames.setLayoutData(fdAccFilenames);

    // Accept filenames from previous transforms?
    //
    //    wlPassThruFields = new Label(gAccepting, SWT.RIGHT);
    //    wlPassThruFields.setText(
    //        BaseMessages.getString(PKG, "ParquetInputDialog.PassThruFields.Label"));
    //    PropsUi.setLook(wlPassThruFields);
    //    FormData fdlPassThruFields = new FormData();
    //    fdlPassThruFields.top = new FormAttachment(wAccFilenames, margin);
    //    fdlPassThruFields.left = new FormAttachment(0, 0);
    //    fdlPassThruFields.right = new FormAttachment(middle, -margin);
    //    wlPassThruFields.setLayoutData(fdlPassThruFields);
    //    wPassThruFields = new Button(gAccepting, SWT.CHECK);
    //    wPassThruFields.setToolTipText(
    //        BaseMessages.getString(PKG, "ParquetInputDialog.PassThruFields.Tooltip"));
    //    PropsUi.setLook(wPassThruFields);
    //    FormData fdPassThruFields = new FormData();
    //    fdPassThruFields.top = new FormAttachment(wlPassThruFields, 0, SWT.CENTER);
    //    fdPassThruFields.left = new FormAttachment(middle, 0);
    //    fdPassThruFields.right = new FormAttachment(100, 0);
    //    wPassThruFields.setLayoutData(fdPassThruFields);

    // Which transform to read from?
    wlAccTransform = new Label(gAccepting, SWT.RIGHT);
    wlAccTransform.setText(BaseMessages.getString(PKG, "ParquetInputDialog.AcceptTransform.Label"));
    PropsUi.setLook(wlAccTransform);
    FormData fdlAccTransform = new FormData();
    fdlAccTransform.top = new FormAttachment(wAccFilenames, margin);
    fdlAccTransform.left = new FormAttachment(0, 0);
    fdlAccTransform.right = new FormAttachment(middle, -margin);
    wlAccTransform.setLayoutData(fdlAccTransform);
    wAccTransform = new CCombo(gAccepting, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wAccTransform.setToolTipText(
        BaseMessages.getString(PKG, "ParquetInputDialog.AcceptTransform.Tooltip"));
    PropsUi.setLook(wAccTransform);
    FormData fdAccTransform = new FormData();
    fdAccTransform.top = new FormAttachment(wAccFilenames, margin);
    fdAccTransform.left = new FormAttachment(middle, 0);
    fdAccTransform.right = new FormAttachment(100, 0);
    wAccTransform.setLayoutData(fdAccTransform);

    // Fill in the source transforms...
    List<TransformMeta> prevTransforms =
        pipelineMeta.findPreviousTransforms(pipelineMeta.findTransform(transformName));
    for (TransformMeta prevTransform : prevTransforms) {
      wAccTransform.add(prevTransform.getName());
    }

    // Which field?
    //
    wlAccField = new Label(gAccepting, SWT.RIGHT);
    wlAccField.setText(BaseMessages.getString(PKG, "ParquetInputDialog.AcceptField.Label"));
    PropsUi.setLook(wlAccField);
    FormData fdlAccField = new FormData();
    fdlAccField.top = new FormAttachment(wAccTransform, margin);
    fdlAccField.left = new FormAttachment(0, 0);
    fdlAccField.right = new FormAttachment(middle, -margin);
    wlAccField.setLayoutData(fdlAccField);

    wAccField = new CCombo(gAccepting, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    IRowMeta previousFields;
    try {
      previousFields = pipelineMeta.getPrevTransformFields(variables, transformMeta);
    } catch (HopTransformException e) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(
              PKG, "ParquetInputDialog.ErrorDialog.UnableToGetInputFields.Title"),
          BaseMessages.getString(
              PKG, "ParquetInputDialog.ErrorDialog.UnableToGetInputFields.Message"),
          e);
      previousFields = new RowMeta();
    }
    wAccField.setItems(previousFields.getFieldNames());
    wAccField.setToolTipText(BaseMessages.getString(PKG, "ParquetInputDialog.AcceptField.Tooltip"));

    PropsUi.setLook(wAccField);
    FormData fdAccField = new FormData();
    fdAccField.top = new FormAttachment(wAccTransform, margin);
    fdAccField.left = new FormAttachment(middle, 0);
    fdAccField.right = new FormAttachment(100, 0);
    wAccField.setLayoutData(fdAccField);

    FormData fdAccepting = new FormData();
    fdAccepting.left = new FormAttachment(0, 0);
    fdAccepting.right = new FormAttachment(100, 0);
    fdAccepting.bottom = new FormAttachment(wbShowFiles, -margin * 2);
    gAccepting.setLayoutData(fdAccepting);

    ColumnInfo[] colinfo =
        new ColumnInfo[] {
          new ColumnInfo(
              BaseMessages.getString(PKG, "ParquetInputDialog.FileDirColumn.Column"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "ParquetInputDialog.WildcardColumn.Column"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "ParquetInputDialog.Files.ExcludeWildcard.Column"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "ParquetInputDialog.RequiredColumn.Column"),
              ColumnInfo.COLUMN_TYPE_CCOMBO,
              YES_NO_COMBO),
          new ColumnInfo(
              BaseMessages.getString(PKG, "ParquetInputDialog.IncludeSubDirs.Column"),
              ColumnInfo.COLUMN_TYPE_CCOMBO,
              YES_NO_COMBO)
        };

    colinfo[0].setUsingVariables(true);
    colinfo[1].setToolTip(BaseMessages.getString(PKG, "ParquetInputDialog.RegExpColumn.Column"));

    colinfo[1].setUsingVariables(true);

    colinfo[2].setUsingVariables(true);
    colinfo[2].setToolTip(
        BaseMessages.getString(PKG, "ParquetInputDialog.Files.ExcludeWildcard.Tooltip"));
    colinfo[3].setToolTip(BaseMessages.getString(PKG, "ParquetInputDialog.RequiredColumn.Tooltip"));
    colinfo[4].setToolTip(BaseMessages.getString(PKG, "ParquetInputDialog.IncludeSubDirs.Tooltip"));

    wFilenameList =
        new TableView(
            variables,
            wFileComp,
            SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER,
            colinfo,
            0,
            lsMod,
            props);
    PropsUi.setLook(wFilenameList);
    FormData fdFilenameList = new FormData();
    fdFilenameList.left = new FormAttachment(middle, 0);
    fdFilenameList.right = new FormAttachment(wbdFilename, -margin);
    fdFilenameList.top = new FormAttachment(wExcludeFilemask, margin);
    fdFilenameList.bottom = new FormAttachment(gAccepting, -margin);
    wFilenameList.setLayoutData(fdFilenameList);

    FormData fdFileComp = new FormData();
    fdFileComp.left = new FormAttachment(0, 0);
    fdFileComp.top = new FormAttachment(0, 0);
    fdFileComp.right = new FormAttachment(100, 0);
    fdFileComp.bottom = new FormAttachment(100, 0);
    wFileComp.setLayoutData(fdFileComp);

    wFileComp.pack();
    Rectangle bounds = wFileComp.getBounds();

    wFileSComp.setContent(wFileComp);
    wFileSComp.setExpandHorizontal(true);
    wFileSComp.setExpandVertical(true);
    wFileSComp.setMinWidth(bounds.width);
    wFileSComp.setMinHeight(bounds.height);

    wFileTab.setControl(wFileSComp);

    // ///////////////////////////////////////////////////////////
    // / END OF FILE TAB
    // ///////////////////////////////////////////////////////////
  }

  private void addFieldsTab(CTabFolder wTabFolder, int middle, int margin) {

    CTabItem wFieldsTab = new CTabItem(wTabFolder, SWT.NONE);
    wFieldsTab.setFont(GuiResource.getInstance().getFontDefault());
    wFieldsTab.setText(BaseMessages.getString(PKG, "ParquetInputDialog.FieldsTab.Title"));

    Composite wFieldsComp = new Composite(wTabFolder, SWT.NONE);
    PropsUi.setLook(wFieldsComp);

    FormLayout fieldsLayout = new FormLayout();
    fieldsLayout.marginWidth = 3;
    fieldsLayout.marginHeight = 3;
    wFieldsComp.setLayout(fieldsLayout);

    Group wManualSchemaDefinitionGroup = new Group(wFieldsComp, SWT.SHADOW_NONE);
    PropsUi.setLook(wManualSchemaDefinitionGroup);
    wManualSchemaDefinitionGroup.setText(
        BaseMessages.getString(PKG, "ParquetInputDialog.FieldsSchema.Label"));

    FormLayout manualSchemaDefinitionLayout = new FormLayout();
    manualSchemaDefinitionLayout.marginWidth = 10;
    manualSchemaDefinitionLayout.marginHeight = 10;
    wManualSchemaDefinitionGroup.setLayout(manualSchemaDefinitionLayout);

    ColumnInfo[] columns =
        new ColumnInfo[] {
          new ColumnInfo(
              BaseMessages.getString(PKG, "ParquetInputDialog.FieldsColumn.SourceField.Label"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              new String[0]),
          new ColumnInfo(
              BaseMessages.getString(PKG, "ParquetInputDialog.FieldsColumn.TargetField.Label"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "ParquetInputDialog.FieldsColumn.TargetType.Label"),
              ColumnInfo.COLUMN_TYPE_CCOMBO,
              ValueMetaFactory.getValueMetaNames(),
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "ParquetInputDialog.FieldsColumn.TargetFormat.Label"),
              ColumnInfo.COLUMN_TYPE_FORMAT,
              3),
          new ColumnInfo(
              BaseMessages.getString(PKG, "ParquetInputDialog.FieldsColumn.TargetLength.Label"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              true,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "ParquetInputDialog.FieldsColumn.TargetPrecision.Label"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              true,
              false),
        };

    wFields =
        new TableView(
            variables,
            wManualSchemaDefinitionGroup,
            SWT.BORDER,
            columns,
            input.getFields().size(),
            null,
            props);

    PropsUi.setLook(wFields);
    FormData fdFields = new FormData();
    fdFields.left = new FormAttachment(0, 0);
    fdFields.top = new FormAttachment(0, margin);
    fdFields.right = new FormAttachment(100, 0);
    fdFields.bottom = new FormAttachment(100, 0);
    wFields.setLayoutData(fdFields);

    FormData fdManualSchemaDefinitionComp = new FormData();
    fdManualSchemaDefinitionComp.left = new FormAttachment(0, 0);
    fdManualSchemaDefinitionComp.top = new FormAttachment(0, 0);
    fdManualSchemaDefinitionComp.right = new FormAttachment(100, 0);
    fdManualSchemaDefinitionComp.bottom = new FormAttachment(100, 0);
    wManualSchemaDefinitionGroup.setLayoutData(fdManualSchemaDefinitionComp);

    FormData fdFieldsComp = new FormData();
    fdFieldsComp.left = new FormAttachment(0, 0);
    fdFieldsComp.top = new FormAttachment(0, 0);
    fdFieldsComp.right = new FormAttachment(100, 0);
    fdFieldsComp.bottom = new FormAttachment(100, 0);
    wFieldsComp.setLayoutData(fdFieldsComp);

    wFieldsComp.layout();
    wFieldsTab.setControl(wFieldsComp);
  }

  public void setFlags() {

    boolean accept = wAccFilenames.getSelection();
    //    wlPassThruFields.setEnabled(accept);
    //    wPassThruFields.setEnabled(accept);
    //    if (!wAccFilenames.getSelection()) {
    //      wPassThruFields.setSelection(false);
    //    }
    wlAccField.setEnabled(accept);
    wAccField.setEnabled(accept);
    wlAccTransform.setEnabled(accept);
    wAccTransform.setEnabled(accept);

    wlFilename.setEnabled(!accept);
    wbbFilename.setEnabled(!accept); // Browse: add file or directory
    wbdFilename.setEnabled(!accept); // Delete
    wbeFilename.setEnabled(!accept); // Edit
    wbaFilename.setEnabled(!accept); // Add or change
    wFilename.setEnabled(!accept);
    wlFilenameList.setEnabled(!accept);
    wFilenameList.setEnabled(!accept);
    wlFilemask.setEnabled(!accept);
    wFilemask.setEnabled(!accept);
    wlExcludeFilemask.setEnabled(!accept);
    wExcludeFilemask.setEnabled(!accept);
    wFilemask.setEnabled(!accept);
    wbShowFiles.setEnabled(!accept);
  }

  private void getFields() {
    try {
      // Ask for a file to get metadata from...
      //
      String filename =
          BaseDialog.presentFileDialog(
              shell,
              new String[] {"*.parquet*", "*.*"},
              new String[] {"Parquet files", "All files"},
              true);

      if (filename != null) {
        FileObject fileObject = HopVfs.getFileObject(variables.resolve(filename));

        long size = fileObject.getContent().getSize();
        InputStream inputStream = HopVfs.getInputStream(fileObject);

        // Reads the whole file into memory...
        //
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream((int) size);
        IOUtils.copy(inputStream, outputStream);
        ParquetStream inputFile = new ParquetStream(outputStream.toByteArray(), filename);
        // Empty list of fields to retrieve: we still grab the schema
        //
        ParquetReadSupport readSupport = new ParquetReadSupport(new ArrayList<>());
        ParquetReader<RowMetaAndData> reader =
            new ParquetReaderBuilder<>(readSupport, inputFile).build();

        // Read one empty row...
        //
        reader.read();

        // Now we have the schema...
        //
        MessageType schema = readSupport.getMessageType();
        IRowMeta rowMeta = new RowMeta();
        List<ColumnDescriptor> columns = schema.getColumns();
        for (ColumnDescriptor column : columns) {
          String sourceField = "";
          String[] path = column.getPath();
          if (path.length == 1) {
            sourceField = path[0];
          } else {
            for (int i = 0; i < path.length; i++) {
              if (i > 0) {
                sourceField += ".";
              }
              sourceField += path[i];
            }
          }
          PrimitiveType primitiveType = column.getPrimitiveType();
          int hopType = IValueMeta.TYPE_STRING;
          switch (primitiveType.getPrimitiveTypeName()) {
            case INT32:
            case INT64:
              hopType = IValueMeta.TYPE_INTEGER;
              break;
            case INT96:
            case BINARY:
              hopType = IValueMeta.TYPE_BINARY;
              break;
            case FLOAT:
            case DOUBLE:
              hopType = IValueMeta.TYPE_NUMBER;
              break;
            case BOOLEAN:
              hopType = IValueMeta.TYPE_BOOLEAN;
              break;
          }
          IValueMeta valueMeta = ValueMetaFactory.createValueMeta(sourceField, hopType, -1, -1);
          rowMeta.addValueMeta(valueMeta);
        }

        BaseTransformDialog.getFieldsFromPrevious(
            rowMeta, wFields, 1, new int[] {1, 2}, new int[] {3}, -1, -1, null);
      }
    } catch (Exception e) {
      LogChannel.UI.logError("Error getting parquet file fields", e);
    }
  }

  private void displayErrorDialog(HopException e, String messageKey) {
    new ErrorDialog(
        shell,
        BaseMessages.getString(PKG, "System.Dialog.Error.Title"),
        BaseMessages.getString(PKG, messageKey),
        e);
  }

  private void get() {
    // TODO
  }

  private void showFiles() {

    ParquetInputEnhancedMeta pfm = new ParquetInputEnhancedMeta();
    getInfo(pfm);
    String[] files = pfm.getFilePaths(variables, pfm.getFileItems());
    if (files != null && files.length > 0) {
      EnterSelectionDialog esd =
          new EnterSelectionDialog(shell, files, "Files read", "Files read:");
      esd.setViewOnly();
      esd.open();
    } else {
      MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
      mb.setMessage(BaseMessages.getString(PKG, "ParquetInputDialog.NoFilesFound.DialogMessage"));
      mb.setText(BaseMessages.getString(PKG, SYSTEM_DIALOG_ERROR_TITLE));
      mb.open();
    }
  }

  private void getData(ParquetInputEnhancedMeta meta) {

    wTransformName.setText(Const.NVL(transformName, ""));
    wAccFilenames.setSelection(meta.isAcceptingFilenames());
    // wPassThruFields.setSelection(meta.isPassingThruFields());
    if (meta.getAcceptingField() != null) {
      wAccField.setText(meta.getAcceptingField());
    }
    if (meta.getAcceptingTransformName() != null) {
      wAccTransform.setText(meta.getAcceptingTransformName());
    }

    wAddFileResult.setSelection(meta.isAddResultFile());

    int nrFiles = meta.getFileItems().size();
    if (meta.getFileItems() != null && nrFiles > 0) {
      wFilenameList.clearAll();

      for (int i = 0; i < nrFiles; i++) {
        ParquetFileItem f = meta.getFileItems().get(i);
        wFilenameList.add(
            f.getFileName(),
            f.getFileMask(),
            f.getExcludeFileMask(),
            meta.getRequiredFilesDesc(f.getFileRequired()),
            meta.getRequiredFilesDesc(f.getIncludeSubFolders()));
      }

      wFilenameList.removeEmptyRows();
      wFilenameList.setRowNums();
      wFilenameList.optWidth(true);
    }

    for (int i = 0; i < input.getFields().size(); i++) {
      ParquetFileInputField field = input.getFields().get(i);
      TableItem item = wFields.table.getItem(i);
      item.setText(1, Const.NVL(field.getSourceField(), ""));
      item.setText(2, Const.NVL(field.getTargetField(), ""));
      item.setText(3, Const.NVL(field.getTargetType(), ""));
      item.setText(4, Const.NVL(field.getTargetFormat(), ""));
      item.setText(5, String.valueOf(field.getTargetLength()));
      item.setText(6, String.valueOf(field.getTargetPrecision()));
    }

    wInclFilenameField.setText(Const.NVL(meta.getFileField(), ""));
    wShortFileFieldName.setText(Const.NVL(meta.getShortFileFieldName(), ""));
    wPathFieldName.setText(Const.NVL(meta.getPathFieldName(), ""));
    wIsHiddenName.setText(Const.NVL(meta.getHiddenFieldName(), ""));
    wLastModificationTimeName.setText(Const.NVL(meta.getLastModificationTimeFieldName(), ""));
    wUriName.setText(Const.NVL(meta.getUriNameFieldName(), ""));
    wRootUriName.setText(Const.NVL(meta.getRootUriNameFieldName(), ""));
    wExtensionFieldName.setText(Const.NVL(meta.getExtensionFieldName(), ""));
    wSizeFieldName.setText(Const.NVL(meta.getSizeFieldName(), ""));

    wFields.removeEmptyRows();
    wFields.setRowNums();
    wFields.optWidth(true);

    setFlags();
  }

  private void ok() {
    returnValue = wTransformName.getText();

    getInfo(input);
    input.setChanged();
    dispose();
  }

  private void getInfo(ParquetInputEnhancedMeta meta) {
    // SR
    transformName = wTransformName.getText();

    // copy info to TextFileInputMeta class (input)
    meta.setAcceptingFilenames(wAccFilenames.getSelection());
    //    meta.setPassingThruFields(wPassThruFields.getSelection());
    meta.setAcceptingField(wAccField.getText());
    meta.setAcceptingTransformName(wAccTransform.getText());
    meta.setAcceptingTransform(pipelineMeta.findTransform(wAccTransform.getText()));

    meta.setAddResultFile(wAddFileResult.getSelection());

    int nrFiles = wFilenameList.nrNonEmpty();
    int nrFields = wFields.nrNonEmpty();

    meta.getFields().clear();
    for (int i = 0; i < nrFields; i++) {
      ParquetFileInputField field = new ParquetFileInputField();

      TableItem item = wFields.getNonEmpty(i);
      field.setSourceField(item.getText(1));
      field.setTargetField(item.getText(2));
      field.setTargetType(item.getText(3));
      field.setTargetFormat(item.getText(4));
      field.setTargetLength(Const.toInt(item.getText(5), -1));
      field.setTargetPrecision(Const.toInt(item.getText(6), -1));

      meta.getFields().add(field);
    }

    meta.getFileItems().clear();

    if (meta.getFileItems() != null && nrFiles > 0) meta.getFileItems().clear();

    for (int i = 0; i < nrFiles; i++) {
      TableItem item = wFilenameList.getNonEmpty(i);

      ParquetFileItem fileItem = new ParquetFileItem();
      fileItem.setFileName(item.getText(1));
      fileItem.setFileMask(item.getText(2));
      fileItem.setExcludeFileMask(item.getText(3));
      fileItem.setFileRequired(item.getText(4));
      fileItem.setIncludeSubFolders(item.getText(5));

      meta.getFileItems().add(fileItem);
    }

    meta.setFileField(wInclFilenameField.getText());
    meta.setShortFileFieldName(wShortFileFieldName.getText());
    meta.setPathFieldName(wPathFieldName.getText());
    meta.setHiddenFieldName(wIsHiddenName.getText());
    meta.setLastModificationTimeFieldName(wLastModificationTimeName.getText());
    meta.setUriNameFieldName(wUriName.getText());
    meta.setRootUriNameFieldName(wRootUriName.getText());
    meta.setExtensionFieldName(wExtensionFieldName.getText());
    meta.setSizeFieldName(wSizeFieldName.getText());
  }

  private void cancel() {
    returnValue = null;
    dispose();
  }

  @Override
  public void dispose() {
    props.setScreen(new WindowProperty(shell));
    shell.dispose();
  }
}
