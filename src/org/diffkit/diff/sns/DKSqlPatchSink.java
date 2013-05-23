/**
 * Copyright 2010-2011 Joseph Panico
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.diffkit.common.DKUserException;
import org.diffkit.common.DKValidate;
import org.diffkit.common.annot.NotThreadSafe;
import org.diffkit.db.DKDBConnectionInfo;
import org.diffkit.db.DKDBTable;
import org.diffkit.db.DKDatabase;
import org.diffkit.diff.engine.DKColumnDiff;
import org.diffkit.diff.engine.DKColumnDiffRow;
import org.diffkit.diff.engine.DKContext;
import org.diffkit.diff.engine.DKDiff;
import org.diffkit.diff.engine.DKRowDiff;
import org.diffkit.diff.engine.DKSide;
import org.diffkit.util.DKListUtil;
import org.diffkit.util.DKObjectUtil;
import org.diffkit.util.DKStringUtil;

/**
 * @author jpanico
 */
@NotThreadSafe
public class DKSqlPatchSink extends DKAbstractSink {

   private long _rowDiffCount;
   private long _columnDiffCount;
   private DKColumnDiffRow _runnningColumnDiffRow;
   private final List<Integer> _runningRhsColumnDiffIndices = new ArrayList<Integer>();
   private Writer _writer;
   private DKDatabase _database;
   private DKDBTable _rhsTable;
   private File _file;
   private final Logger _log = LoggerFactory.getLogger(this.getClass());
   private final boolean _isDebugEnabled = _log.isDebugEnabled();

   public DKSqlPatchSink(DKDBConnectionInfo connectionInfo_, String rhsTableName_,
                         String patchFilePath_) throws SQLException, IOException {
      super(null);
      DKValidate.notNull(patchFilePath_);
      _file = new File(patchFilePath_);
      if (_file.exists())
         throw new DKUserException(String.format(
            "sink file [%s] already exists! please remove it and try again.", _file));
      this.init(connectionInfo_, rhsTableName_, null);
   }

   public DKSqlPatchSink(String newFilePath_, DKSqlPatchSink toClone_)
      throws IOException, SQLException {
      this(toClone_.getConnectionInfo(), toClone_.getRhsTableName(), newFilePath_);
   }

   private DKSqlPatchSink(DKDBConnectionInfo connectionInfo_, String rhsTableName_,
                          Writer writer_) throws SQLException {
      super(null);
      this.init(connectionInfo_, rhsTableName_, writer_);
   }

   private void init(DKDBConnectionInfo connectionInfo_, String rhsTableName_,
                     Writer writer_) throws SQLException {

      DKValidate.notNull(connectionInfo_, rhsTableName_);
      _writer = writer_;
      _database = new DKDatabase(connectionInfo_);
      _rhsTable = _database.getTable(null, null, rhsTableName_);
      DKValidate.notNull(_database, _rhsTable);
   }

   private DKDBConnectionInfo getConnectionInfo() {
      return _database.getConnectionInfo();
   }

   private String getRhsTableName() {
      return _rhsTable.getTableName();
   }

   public File getFile() {
      return _file;
   }

   public void record(DKDiff diff_, DKContext context_) {
      this.ensureStarted();
      this.ensureNotEnded();
      if (diff_ == null)
         return;
      DKDiff.Kind diffKind = diff_.getKind();
      if (diffKind == DKDiff.Kind.ROW_DIFF)
         _rowDiffCount++;
      else if (diffKind == DKDiff.Kind.COLUMN_DIFF)
         _columnDiffCount++;

      if (diff_ instanceof DKRowDiff) {
         DKRowDiff diff = (DKRowDiff) diff_;
         // it's on the RHS, which is not the reference side, so it's an *extra*
         // row
         if (diff.getSide() == DKSide.RIGHT)
            this.writeExtraRow(diff);
         // it's on the LHS, which is the reference side, so it's *missing* from
         // the RHS
         else if (diff.getSide() == DKSide.LEFT)
            this.writeMissingRow(diff);
      }
      else if (diff_ instanceof DKColumnDiff)
         this.writeColumnDiff((DKColumnDiff) diff_, context_);

   }

   // it's "extra" on the RHS, which means it has to be DELETEd
   private void writeExtraRow(DKRowDiff rowDiff_) {
      if (_isDebugEnabled)
         _log.debug("rowDiff_->{}", rowDiff_);
      this.flushRunningColumnDiffRow();
      if (rowDiff_ == null)
         return;
      try {
         String deleteSql = _database.generateDeleteDML(rowDiff_.getRow(), _rhsTable);
         if (_isDebugEnabled)
            _log.debug("deleteSql->{}", deleteSql);
         _writer.write(String.format("%s;\n\n", deleteSql));
      }
      catch (Exception e_) {
         _log.error(null, e_);
      }
   }

   // it's "missing" from the RHS, which means it has to be INSERTed
   private void writeMissingRow(DKRowDiff rowDiff_) {
      if (_isDebugEnabled)
         _log.debug("rowDiff_->{}", rowDiff_);
      this.flushRunningColumnDiffRow();
      if (rowDiff_ == null)
         return;
      try {
         String insertSql = _database.generateInsertDML(rowDiff_.getRow(), _rhsTable);
         if (_isDebugEnabled)
            _log.debug("insertSql->{}", insertSql);
         _writer.write(String.format("%s;\n\n", insertSql));
      }
      catch (Exception e_) {
         _log.error(null, e_);
      }
   }

   // add an UPDATE to the running columnDiffRow
   private void writeColumnDiff(DKColumnDiff columnDiff_, DKContext context_) {
      if (_isDebugEnabled) {
         _log.debug("columnDiff_->{}", columnDiff_);
         _log.debug("context_->{}", (context_ == null ? null : context_.getDescription()));
      }
      if (columnDiff_ == null) {
         this.flushRunningColumnDiffRow();
         return;
      }
      DKColumnDiffRow columnDiffRow = columnDiff_.getRow();
      if (_runnningColumnDiffRow != columnDiffRow) {
         this.flushRunningColumnDiffRow();
         _runnningColumnDiffRow = columnDiffRow;
      }
      _runningRhsColumnDiffIndices.add(new Integer(context_._rhsColumnIdx));
   }

   private void flushRunningColumnDiffRow() {
      if (_runnningColumnDiffRow == null) {
         _runningRhsColumnDiffIndices.clear();
         return;
      }
      try {
         String updateSql = _database.generateUpdateDML(
            _runnningColumnDiffRow.getLhsRow(),
            DKListUtil.toPrimitiveArray(_runningRhsColumnDiffIndices), _rhsTable);
         if (_isDebugEnabled)
            _log.debug("updateSql->{}", updateSql);
         _writer.write(String.format("%s;\n\n", updateSql));
      }
      catch (Exception e_) {
         _log.error(null, e_);
      }
      _runnningColumnDiffRow = null;
      _runningRhsColumnDiffIndices.clear();
   }

   @Override
   public void open(DKContext context_) throws IOException {
      super.open(context_);
      if (_writer == null)
         _writer = new BufferedWriter(new FileWriter(_file));
   }

   @Override
   public void close(DKContext context_) throws IOException {
      super.close(context_);
      this.flushRunningColumnDiffRow();
      _writer.flush();
      _writer.close();
      _writer = null;
   }

   // @Override
   public Kind getKind() {
      return Kind.FILE;
   }

   @Override
   public long getDiffCount() {
      return (_rowDiffCount + _columnDiffCount);
   }

   @Override
   public long getRowDiffCount() {
      return _rowDiffCount;
   }

   @Override
   public long getColumnDiffCount() {
      return _columnDiffCount;
   }

   public URI getURI() {
      return DKStringUtil.createURI(String.format("patch://%s",
         DKObjectUtil.getAddressHexString(this)));
   }

   public String toString() {
      return String.format("%s[%s, diffCount=%s]",
         ClassUtils.getShortClassName(this.getClass()), this.getURI(),
         this.getDiffCount());
   }

}
