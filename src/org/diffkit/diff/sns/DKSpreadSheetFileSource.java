/**
 * Copyright 2010-2011 Kiran Ratnapu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.diffkit.diff.sns;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.diffkit.common.DKValidate;
import org.diffkit.common.annot.NotThreadSafe;
import org.diffkit.diff.engine.DKColumnModel;
import org.diffkit.diff.engine.DKContext;
import org.diffkit.diff.engine.DKSource;
import org.diffkit.diff.engine.DKTableModel;
import org.diffkit.util.DKFileUtil;

/**
 * @author kratnapu
 */
@NotThreadSafe
public class DKSpreadSheetFileSource implements DKSource {

   @SuppressWarnings("rawtypes")
   public static final Class[] HANDLER_CLASSES = { DKPoiSheet.class };
   private static final Logger LOG = LoggerFactory.getLogger(DKSpreadSheetFileSource.class);

   private final DKSheet _sheet;
   private final DKTableModel _requestedModel;
   private final String[] _requestedKeyColumnNames;
   private DKTableModel _model;
   private Iterator<Object[]> _rowIterator;
   private int _lastIndex = -1;
   private boolean _isOpen;
   private final Logger _log = LoggerFactory.getLogger(this.getClass());
   private final boolean _isDebugEnabled = _log.isDebugEnabled();

   public DKSpreadSheetFileSource(String filePath_, String sheetName_,
                                  DKTableModel requestedModel_, String[] keyColumnNames_,
                                  int[] readColumnIdxs_, boolean isSorted_,
                                  boolean hasHeader_, boolean validateLazily_) {
      if (_isDebugEnabled) {
         _log.debug("filePath_->{}", filePath_);
         _log.debug("sheetName_->{}", sheetName_);
         _log.debug("requestedModel_->{}", requestedModel_);
         _log.debug("keyColumnNames_->{}", ArrayUtils.toString(keyColumnNames_));
         _log.debug("readColumnIdxs_->{}", ArrayUtils.toString(readColumnIdxs_));
         _log.debug("isSorted_->{}", isSorted_);
         _log.debug("hasHeader_->{}", hasHeader_);
         _log.debug("validateLazily_->{}", validateLazily_);
      }
      DKValidate.notNull(filePath_);
      _sheet = createSheet(filePath_, sheetName_, isSorted_, hasHeader_, validateLazily_);
      DKValidate.notNull(_sheet);
      _requestedModel = requestedModel_ == null ? null : requestedModel_.copy();
      if (!ArrayUtils.isEmpty(readColumnIdxs_))
         throw new NotImplementedException("readColumnIdxs_ not yet supported!");
      _requestedKeyColumnNames = keyColumnNames_;
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * org.diffkit.diff.engine.DKSourceSink#open(org.diffkit.diff.engine.DKContext
    * )
    */
   public void open(DKContext context_) throws IOException {
      _log.debug("context_->{}", context_);
      this.ensureNotOpen();
      _rowIterator = _sheet.getRowIterator(this.getModel());
      _isOpen = true;
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * org.diffkit.diff.engine.DKSourceSink#close(org.diffkit.diff.engine.DKContext
    * )
    */
   public void close(DKContext context_) throws IOException {
      _log.debug("context_->{}", context_);
      this.ensureOpen();
      _sheet.close();
      _rowIterator = null;
      _isOpen = false;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.diffkit.diff.engine.DKSourceSink#getKind()
    */
   public Kind getKind() {
      return Kind.FILE;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.diffkit.diff.engine.DKSource#getModel()
    */
   public DKTableModel getModel() {
      if (_model != null)
         return _model;
      try {
         _model = determineModel(_requestedModel, _sheet, _requestedKeyColumnNames);
         return _model;
      }
      catch (IOException e_) {
         _log.error(null, e_);
         throw new RuntimeException(e_);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.diffkit.diff.engine.DKSource#getURI()
    */
   public URI getURI() throws IOException {
      throw new NotImplementedException();
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.diffkit.diff.engine.DKSource#getNextRow()
    */
   public Object[] getNextRow() throws IOException {
      this.ensureOpen();
      if (!_rowIterator.hasNext())
         return null;
      _lastIndex++;
      return _rowIterator.next();
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.diffkit.diff.engine.DKSource#getLastIndex()
    */
   public long getLastIndex() {
      return _lastIndex;
   }

   /**
    * if requestedModel_, then returns that. Else, extracts the model from
    * sheet_. If extracting from sheet and keyColumnNames_ are specifies, then
    * will attempt to modify the sheet derived model to override the default key
    * from sheet.
    */
   private static DKTableModel determineModel(DKTableModel requestedModel_,
                                              DKSheet sheet_, String[] keyColumnNames_)
      throws IOException {
      if (LOG.isDebugEnabled()) {
         LOG.debug("requestedModel_->{}", requestedModel_);
         LOG.debug("sheet_->{}", sheet_);
         LOG.debug("keyColumnNames_->{}", Arrays.toString(keyColumnNames_));
      }
      if (requestedModel_ != null)
         return requestedModel_;
      DKTableModel extractedModel = sheet_.getModelFromSheet();
      if (ArrayUtils.isEmpty(keyColumnNames_))
         return extractedModel;
      DKColumnModel[] columns = extractedModel.getColumns();
      if (columns[0].isRowNum()) {
         DKColumnModel[] newColumns = new DKColumnModel[columns.length - 1];
         for (int i = 0; i < newColumns.length; i++) {
            DKColumnModel aColumn = columns[i + 1];
            newColumns[i] = new DKColumnModel(i, aColumn.getName(), aColumn.getType(),
               aColumn.getFormatString());
         }
         columns = newColumns;
      }
      LOG.debug("columns->{}", Arrays.toString(columns));
      return new DKTableModel(extractedModel.getName(), columns,
         DKColumnModel.getColumnIndexes(columns, keyColumnNames_));
   }

   /**
    * Factory method that uses the file extension on filePath_ to determine
    * which DKSheet implementation class to use. Once it finds a handler class,
    * it calls constructor with args [file_, sheetName_, isSorted_, hasHeader_,
    * validateLazily_]
    * 
    * @return a new instance of a DKSheet implementation
    * @throws InstantiationException
    * @throws InvocationTargetException
    * @throws IllegalAccessException
    * @throws NoSuchMethodException
    */
   @SuppressWarnings("unchecked")
   private static DKSheet createSheet(String filePath_, String sheetName_,
                                      boolean isSorted_, boolean hasHeader_,
                                      boolean validateLazily_) {
      try {
         LOG.debug("filePath_->{}", filePath_);
         LOG.debug("sheetName_->{}", sheetName_);
         LOG.debug("isSorted_->{}", isSorted_);
         LOG.debug("hasHeader_->{}", hasHeader_);
         LOG.debug("validateLazily_->{}", validateLazily_);
         File file = DKFileUtil.findFile(filePath_);
         LOG.debug("file->{}", file);
         return DKAbstractSheet.constructSheet(file, sheetName_, isSorted_, hasHeader_,
            validateLazily_, (Class<DKSheet>[]) HANDLER_CLASSES);
      }
      catch (Exception e_) {
         throw new RuntimeException(e_);
      }
   }

   private void ensureOpen() {
      if (!_isOpen)
         throw new RuntimeException("not open!");
   }

   private void ensureNotOpen() {
      if (_isOpen)
         throw new RuntimeException("already open!");
   }
}
