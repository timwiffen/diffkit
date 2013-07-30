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
package org.diffkit.diff.conf;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.diffkit.common.kvc.DKKeyValueCoder;
import org.diffkit.db.DKDBConnectionInfo;
import org.diffkit.db.DKDatabase;
import org.diffkit.diff.conf.DKMagicPlanRule.RuleImplementation;
import org.diffkit.diff.engine.DKDiff;
import org.diffkit.diff.engine.DKSink;
import org.diffkit.diff.engine.DKSource;
import org.diffkit.diff.engine.DKTableModel;
import org.diffkit.diff.sns.DKDBSource;
import org.diffkit.diff.sns.DKFileSink;
import org.diffkit.diff.sns.DKFileSource;
import org.diffkit.diff.sns.DKSpreadSheetFileSource;
import org.diffkit.diff.sns.DKSqlPatchSink;
import org.diffkit.diff.sns.DKWriterSink;

/**
 * @author jpanico
 */
// @SuppressWarnings("unchecked")
public class DKMagicPlanRules {

   private static final DKMagicPlanRule LHS_DB_SOURCE_FROM_LHS_DB_TABLE_RULE = new DKMagicPlanRule(
      "lhsDBSource.lhsDBtable",
      "if lhsDBTableName is specified in plan, then force lhsSource to be DBSource",
      null, "lhsSource_", "lhsDBTableName", true, new TypeRefinement<DKDBSource>(
         DKDBSource.class));
   private static final DKMagicPlanRule RHS_DB_SOURCE_FROM_RHS_DB_TABLE_RULE = new DKMagicPlanRule(
      "rhsDBSource.rhsDBtable",
      "if rhsDBTableName is specified in plan, then force rhsSource to be DBSource",
      null, "rhsSource_", "rhsDBTableName", true, new TypeRefinement<DKDBSource>(
         DKDBSource.class));
   private static final DKMagicPlanRule LHS_DB_SOURCE_FROM_DB_TABLE_RULE = new DKMagicPlanRule(
      "lhsDBSource.dbTable",
      "if dbTableName is specified in plan, then force lhsSource to be DBSource", null,
      "lhsSource_", "dbTableName", true, new TypeRefinement<DKDBSource>(DKDBSource.class));
   private static final DKMagicPlanRule RHS_DB_SOURCE_FROM_DB_TABLE_RULE = new DKMagicPlanRule(
      "rhsDBSource.dbTable",
      "if dbTableName is specified in plan, then force rhsSource to be DBSource", null,
      "rhsSource_", "dbTableName", true, new TypeRefinement<DKDBSource>(DKDBSource.class));
   private static final DKMagicPlanRule DB_TABLE_NAME_RULE = new DKMagicPlanRule(
      "dbTableName",
      "if dbTableName is specified in plan, then use it in any DBSource constructor that requires one",
      DKDBSource.class, null, "tableName_", "dbTableName", false, new PlanValue(true));
   private static final DKMagicPlanRule LHS_DB_TABLE_NAME_RULE = new DKMagicPlanRule(
      "lhsDBTableName",
      "if lhsDBTableName is specified in plan, then use it as the tableName in the lhs DBSource",
      DKDBSource.class, "lhsSource_.tableName_", "lhsDBTableName", true, new PlanValue(
         true));
   private static final DKMagicPlanRule RHS_DB_TABLE_NAME_RULE = new DKMagicPlanRule(
      "rhsDBTableName",
      "if rhsDBTableName is specified in plan, then use it as the tableName in the lhs DBSource",
      DKDBSource.class, "rhsSource_.tableName_", "rhsDBTableName", true, new PlanValue(
         true));
   private static final DKMagicPlanRule WHERE_CLAUSE_RULE = new DKMagicPlanRule(
      "whereClause",
      "if whereClause is specified in plan, then use it in any constructor that requires one",
      DKDBSource.class, null, "whereClause_", "whereClause", true, new PlanValue(true));
   private static final DKMagicPlanRule LHS_WHERE_CLAUSE_RULE = new DKMagicPlanRule(
      "lhsWhereClause",
      "assign lhsWhereClause from Plan to lhs DBSource, whether null or non-null",
      DKDBSource.class, "lhsSource_.whereClause_", "lhsWhereClause", true, new PlanValue(
         true));
   private static final DKMagicPlanRule RHS_WHERE_CLAUSE_RULE = new DKMagicPlanRule(
      "rhsWhereClause",
      "assign rhsWhereClause from Plan to rhs DBSource, whether null or non-null",
      DKDBSource.class, "rhsSource_.whereClause_", "rhsWhereClause", true, new PlanValue(
         true));
   private static final DKMagicPlanRule DEFAULT_WHERE_CLAUSE_RULE = new DKMagicPlanRule(
      "defaultWhereClause",
      "assign default null value to whereClause if value in Plan is null",
      DKDBSource.class, "whereClause_", "whereClause", false, new PlanValue(false));
   private static final DKMagicPlanRule DB_CONNECTION_INFO_RULE = new DKMagicPlanRule(
      "dbConnectionInfo",
      "if dbConnectionInfo is specified in plan, then use it in any constructor that requires one",
      null, DKDBConnectionInfo.class, "connectionInfo_", "dbConnectionInfo", false,
      new PlanValue(true));
   private static final DKMagicPlanRule LHS_DB_CONNECTION_INFO_RULE = new DKMagicPlanRule(
      "lhsDBConnectionInfo",
      "if lhsDBConnectionInfo is specified in plan, then use it as the ConnectionInfo in the lhs DBSource",
      DKDatabase.class, "lhsSource_.database_.connectionInfo_", "lhsDBConnectionInfo",
      true, new PlanValue(true));
   private static final DKMagicPlanRule RHS_DB_CONNECTION_INFO_RULE = new DKMagicPlanRule(
      "rhsDBConnectionInfo",
      "if rhsDBConnectionInfo is specified in plan, then use it as the ConnectionInfo in the rhs DBSource",
      DKDatabase.class, "rhsSource_.database_.connectionInfo_", "rhsDBConnectionInfo",
      true, new PlanValue(true));
   private static final DKMagicPlanRule MODEL_DEFAULT_RULE = new DKMagicPlanRule(
      "modelDefault",
      "if no TableModel is explicitly supplied, set it to null so that target object will default it from its ultimate source",
      null, DKTableModel.class, "model_", null, false, new Constant(null));
   private static final DKMagicPlanRule KEY_COLUMN_NAMES_RULE = new DKMagicPlanRule(
      "keyColumnNames", "map keyColumnNames from plan to param in any DKSource",
      DKSource.class, "keyColumnNames_", "keyColumnNames", true, new PlanValue(false));
   private static final DKMagicPlanRule READ_COLUMNS_RULE = new DKMagicPlanRule(
      "readColumnsDefault", "the readColumnIdxs_ in any Source will be null", null,
      int[].class, "readColumnIdxs_", null, true, new Constant(null));
   private static final DKMagicPlanRule LHS_FILE_SOURCE_RULE = new DKMagicPlanRule(
      "lhsFileSource",
      "if lhsFilePath is specified in plan, then force lhsSource to be FileSource", null,
      "lhsSource_", "lhsFilePath", true, new TypeRefinement<DKFileSource>(
         DKFileSource.class));
   private static final DKMagicPlanRule LHS_FILE_PATH_RULE = new DKMagicPlanRule(
      "lhsFilePath",
      "if lhsFilePath is specified in plan, then use it as the filePath_ in the lhs FileSource",
      DKFileSource.class, "lhsSource_.filePath_", "lhsFilePath", true,
      new PlanValue(true));
   private static final DKMagicPlanRule RHS_FILE_SOURCE_RULE = new DKMagicPlanRule(
      "rhsFileSource",
      "if rhsFilePath is specified in plan, then force rhsSource to be FileSource", null,
      "rhsSource_", "rhsFilePath", true, new TypeRefinement<DKFileSource>(
         DKFileSource.class));
   private static final DKMagicPlanRule RHS_FILE_PATH_RULE = new DKMagicPlanRule(
      "rhsFilePath",
      "if rhsFilePath is specified in plan, then use it as the filePath_ in the rhs FileSource",
      DKFileSource.class, "rhsSource_.filePath_", "rhsFilePath", true,
      new PlanValue(true));
   private static final DKMagicPlanRule FILE_SINK_RULE = new DKMagicPlanRule("fileSink",
      "if sinkFilePath specified, then force sink_ to be FileSink",
      DKPassthroughPlan.class, "sink_", "sinkFilePath", true,
      new TypeRefinement<DKFileSink>(DKFileSink.class));
   private static final DKMagicPlanRule FILE_SINK_PATH_RULE = new DKMagicPlanRule(
      "fileSinkPath",
      "if sinkFilePath is specified in plan, then use it as filePath_ arg to FileSink",
      DKFileSink.class, "sink_.filePath_", "sinkFilePath", true, new PlanValue(true));
   private static final DKMagicPlanRule SQL_PATCH_SINK_RULE = new DKMagicPlanRule(
      "sqlPatchSink",
      "if sqlPatchFilePath specified, then force sink_ to be SqlPatchSink",
      DKPassthroughPlan.class, "sink_", "sqlPatchFilePath", true,
      new TypeRefinement<DKSqlPatchSink>(DKSqlPatchSink.class));
   private static final DKMagicPlanRule SQL_PATCH_SINK_RHS_TABLE_NAME_RULE = new DKMagicPlanRule(
      "sqlPatchRhsTableName",
      "if rhsDBTableName is specified in plan, then use it as the rhsTableName_ in any DKSqlPatchSink",
      DKSqlPatchSink.class, "rhsTableName_", "rhsDBTableName", true, new PlanValue(true));
   private static final DKMagicPlanRule SQL_PATCH_FILE_PATH_RULE = new DKMagicPlanRule(
      "sqlPatchFilePath",
      "if sqlPatchFilePath is specified in plan, then use it as the patchFilePath_ in any DKSqlPatchSink",
      DKSqlPatchSink.class, "patchFilePath_", "sqlPatchFilePath", true, new PlanValue(
         true));
   private static final DKMagicPlanRule DEFAULT_SINK_RULE = new DKMagicPlanRule(
      "defaultSink",
      "if no other Sink specified, then use WriterSink that sends output to stderr using a default formatter",
      DKPassthroughPlan.class, DKSink.class, "sink_", null, false, new DefaultSink());
   private static final DKMagicPlanRule AUTOMATIC_TABLE_COMPARISON_RULE = new DKMagicPlanRule(
      "automaticTableComparison",
      "always use an AutomaticTableComparison for tableComparison_",
      DKPassthroughPlan.class, "tableComparison_", null, false,
      new TypeRefinement<DKAutomaticTableComparison>(DKAutomaticTableComparison.class));
   private static final DKMagicPlanRule DELIMITER_RULE = new DKMagicPlanRule("delimiter",
      "if delimiter is specified in plan, then use it FileSources", DKFileSource.class,
      "delimiter_", "delimiter", true, new PlanValue(true));
   private static final DKMagicPlanRule DEFAULT_DELIMITER_RULE = new DKMagicPlanRule(
      "defaultDelimiter", "if no delimiter is specified in plan, use ','",
      DKFileSource.class, "delimiter_", null, false, new Constant(","));
   private static final DKMagicPlanRule IS_SORTED_RULE = new DKMagicPlanRule("isSorted",
      "hardwire isSorted to true", DKFileSource.class, "isSorted_", null, true,
      new Constant(Boolean.TRUE));
   private static final DKMagicPlanRule VALIDATE_LAZILY_RULE = new DKMagicPlanRule(
      "validateLazily", "hardwire validateLazily to false", DKFileSource.class,
      "validateLazily_", null, true, new Constant(Boolean.FALSE));
   private static final DKMagicPlanRule DIFF_KIND_RULE = new DKMagicPlanRule("diffKind",
      "assign diffKind from Plan to AutomaticTableComparison",
      DKAutomaticTableComparison.class, "kind_", "diffKind", true, new PlanValue(true));
   private static final DKMagicPlanRule DEFAULT_DIFF_KIND_RULE = new DKMagicPlanRule(
      "defaultDiffKind", "if no Diff.Kind specified, default to BOTH",
      DKAutomaticTableComparison.class, "kind_", null, false, new Constant(
         DKDiff.Kind.BOTH));
   private static final DKMagicPlanRule DIFF_COLUMN_NAMES_RULE = new DKMagicPlanRule(
      "diffColumnNames",
      "assign diffColumnNames from Plan to AutomaticTableComparison, whether null or non-null",
      DKAutomaticTableComparison.class, "diffColumnNames_", "diffColumnNames", true,
      new PlanValue(false));
   private static final DKMagicPlanRule IGNORE_COLUMN_NAMES_RULE = new DKMagicPlanRule(
      "ignoreColumnNames",
      "assign ignoreColumnNames from Plan to AutomaticTableComparison, whether null or non-null",
      DKAutomaticTableComparison.class, "ignoreColumnNames_", "ignoreColumnNames", true,
      new PlanValue(false));
   private static final DKMagicPlanRule DISPLAY_COLUMN_NAMES_RULE = new DKMagicPlanRule(
      "displayColumnNames",
      "assign displayColumnNames from Plan to AutomaticTableComparison",
      DKAutomaticTableComparison.class, "displayColumnNames_", "displayColumnNames",
      true, new PlanValue(false));
   private static final DKMagicPlanRule MAX_DIFFS_RULE = new DKMagicPlanRule("maxDiffs",
      "assign maxDiffs from Plan to AutomaticTableComparison",
      DKAutomaticTableComparison.class, "maxDiffs_", "maxDiffs", true,
      new PlanValue(true));
   private static final DKMagicPlanRule DEFAULT_MAX_DIFFS_RULE = new DKMagicPlanRule(
      "defaultMaxDiffs", "assign default value to maxDiffs if value in Plan is null",
      DKAutomaticTableComparison.class, "maxDiffs_", null, false, new Constant(
         Long.MAX_VALUE));
   private static final DKMagicPlanRule NUMBER_TOLERANCE_RULE = new DKMagicPlanRule(
      "numberTolerance",
      "assign numberTolerance from Plan to AutomaticTableComparison, whether null or non-null",
      DKAutomaticTableComparison.class, "numberTolerance_", "numberTolerance", true,
      new PlanValue(false));
   private static final DKMagicPlanRule TOLERANCE_MAP_RULE = new DKMagicPlanRule(
      "toleranceMap",
      "assign toleranceMap from Plan to AutomaticTableComparison, whether null or non-null",
      DKAutomaticTableComparison.class, "toleranceMap_", "toleranceMap", true,
      new PlanValue(false));
   private static final DKMagicPlanRule WITH_SUMMARY_RULE = new DKMagicPlanRule(
      "withSummary", "assign withSummary from the Plan to the FileSink",
      DKFileSink.class, "withSummary_", "withSummary", true, new PlanValue(true));
   private static final DKMagicPlanRule DEFAULT_WITH_SUMMARY_RULE = new DKMagicPlanRule(
      "defaultWithSummary",
      "if no withSummary specified in the Plan, then use this rule", DKFileSink.class,
      "withSummary_", null, false, new Constant(Boolean.FALSE));
   private static final DKMagicPlanRule GROUP_BY_COLUMN_NAMES_RULE = new DKMagicPlanRule(
      "groupByColumnNames", "assign groupByColumnNames from Plan to DKFileSink",
      DKFileSink.class, "groupByColumnNames_", "groupByColumnNames", true, new PlanValue(
         false));
   private static final DKMagicPlanRule LHS_SPREADSHEET_FILE_SOURCE_RULE = new DKMagicPlanRule(
      "lhsSpreadSheetFileSource",
      "if lhsSpreadSheetFilePath is specified in plan, then force lhsSource to be SpreadSheetFileSource",
      null, "lhsSource_", "lhsSpreadSheetFilePath", true,
      new TypeRefinement<DKSpreadSheetFileSource>(DKSpreadSheetFileSource.class));
   private static final DKMagicPlanRule RHS_SPREADSHEET_FILE_SOURCE_RULE = new DKMagicPlanRule(
      "rhsSpreadSheetFileSource",
      "if rhsSpreadSheetFilePath is specified in plan, then force rhsSource to be SpreadSheetFileSource",
      null, "rhsSource_", "rhsSpreadSheetFilePath", true,
      new TypeRefinement<DKSpreadSheetFileSource>(DKSpreadSheetFileSource.class));
   private static final DKMagicPlanRule LHS_SPREADSHEET_FILE_PATH_RULE = new DKMagicPlanRule(
      "lhsSpreadSheetFilePath",
      "if lhsSpreadSheetFilePath is specified in plan, then use it as the filePath_ in the lhsSpreadSheetFileSource",
      DKSpreadSheetFileSource.class, "lhsSource_.filePath_", "lhsSpreadSheetFilePath",
      true, new PlanValue(true));

   private static final DKMagicPlanRule RHS_SPREADSHEET_FILE_PATH_RULE = new DKMagicPlanRule(
      "rhsSpreadSheetFilePath",
      "if rhsSpreadSheetFilePath is specified in plan, then use it as the filePath_ in the rhsSpreadSheetFileSource",
      DKSpreadSheetFileSource.class, "rhsSource_.filePath_", "rhsSpreadSheetFilePath",
      true, new PlanValue(true));
   private static final DKMagicPlanRule SPREADSHEET_IS_SORTED_RULE = new DKMagicPlanRule(
      "spreadsheetIsSorted",
      "assign isSorted from the Plan to the SpreadSheetFileSource",
      DKSpreadSheetFileSource.class, "isSorted_", "isSorted", true, new PlanValue(true));
   private static final DKMagicPlanRule DEFAULT_SPREADSHEET_IS_SORTED_RULE = new DKMagicPlanRule(
      "spreadsheetIsSorted", "hardwire isSorted to true", DKSpreadSheetFileSource.class,
      "isSorted_", null, false, new Constant(Boolean.TRUE));
   private static final DKMagicPlanRule SPREADSHEET_HAS_HEADER_RULE = new DKMagicPlanRule(
      "hasHeader", "assign hasHeader from the Plan to the SpreadSheetFileSource",
      DKSpreadSheetFileSource.class, "hasHeader_", "hasHeader", true, new PlanValue(true));
   private static final DKMagicPlanRule DEFAULT_SPREADSHEET_HAS_HEADER_RULE = new DKMagicPlanRule(
      "defaultHasHeader", "if no hasHeader specified in the Plan, then use this rule",
      DKSpreadSheetFileSource.class, "hasHeader_", null, false, new Constant(
         Boolean.FALSE));
   private static final DKMagicPlanRule SPREADSHEET_VALIDATE_LAZILY_RULE = new DKMagicPlanRule(
      "validateLazily", "hardwire validateLazily to false",
      DKSpreadSheetFileSource.class, "validateLazily_", null, true, new Constant(
         Boolean.FALSE));
   private static final DKMagicPlanRule LHS_SPREADSHEET_SHEET_NAME_RULE = new DKMagicPlanRule(
      "lhsSpreadSheetName",
      "if lhsSheetName is specified in plan, then use it for lhsSpreadSheetFileSource",
      DKSpreadSheetFileSource.class, "lhsSource_.sheetName_", "lhsSpreadSheetName", true,
      new PlanValue(true));
   private static final DKMagicPlanRule RHS_SPREADSHEET_SHEET_NAME_RULE = new DKMagicPlanRule(
      "rhsSpreadSheetName",
      "if rhsSheetName is specified in plan, then use it for rhsSpreadSheetFileSource",
      DKSpreadSheetFileSource.class, "rhsSource_.sheetName_", "rhsSpreadSheetName", true,
      new PlanValue(true));
   private static final DKMagicPlanRule DEFAULT_SPREADSHEET_SHEET_NAME_RULE = new DKMagicPlanRule(
      "defaultSheetName",
      "if no sheetName is specified in plan, pass in null (name extracted from doc)",
      DKSpreadSheetFileSource.class, "sheetName_", null, false, new Constant(null));
   private static final DKMagicPlanRule DEFAULT_SPREADSHEET_MODEL_RULE = new DKMagicPlanRule(
      "spreadsheetRequestModel",
      "MagicPlan does not allow specification of Model-- it's always derived directly from the Sheet",
      DKSpreadSheetFileSource.class, "requestedModel_", null, false, new Constant(null));

   public static DKMagicPlanRule[] RULES = { LHS_DB_SOURCE_FROM_LHS_DB_TABLE_RULE,
      RHS_DB_SOURCE_FROM_RHS_DB_TABLE_RULE, LHS_DB_SOURCE_FROM_DB_TABLE_RULE,
      RHS_DB_SOURCE_FROM_DB_TABLE_RULE, DB_TABLE_NAME_RULE, LHS_DB_TABLE_NAME_RULE,
      RHS_DB_TABLE_NAME_RULE, WHERE_CLAUSE_RULE, LHS_WHERE_CLAUSE_RULE,
      RHS_WHERE_CLAUSE_RULE, DEFAULT_WHERE_CLAUSE_RULE, DB_CONNECTION_INFO_RULE,
      LHS_DB_CONNECTION_INFO_RULE, RHS_DB_CONNECTION_INFO_RULE, MODEL_DEFAULT_RULE,
      KEY_COLUMN_NAMES_RULE, READ_COLUMNS_RULE, LHS_FILE_SOURCE_RULE, LHS_FILE_PATH_RULE,
      RHS_FILE_SOURCE_RULE, RHS_FILE_PATH_RULE, LHS_SPREADSHEET_FILE_SOURCE_RULE,
      RHS_SPREADSHEET_FILE_SOURCE_RULE, LHS_SPREADSHEET_FILE_PATH_RULE,
      RHS_SPREADSHEET_FILE_PATH_RULE, FILE_SINK_RULE, FILE_SINK_PATH_RULE,
      SQL_PATCH_SINK_RULE, SQL_PATCH_SINK_RHS_TABLE_NAME_RULE, SQL_PATCH_FILE_PATH_RULE,
      DEFAULT_SINK_RULE, AUTOMATIC_TABLE_COMPARISON_RULE, DELIMITER_RULE,
      DEFAULT_DELIMITER_RULE, IS_SORTED_RULE, VALIDATE_LAZILY_RULE, DIFF_KIND_RULE,
      DEFAULT_DIFF_KIND_RULE, DIFF_COLUMN_NAMES_RULE, IGNORE_COLUMN_NAMES_RULE,
      DISPLAY_COLUMN_NAMES_RULE, MAX_DIFFS_RULE, DEFAULT_MAX_DIFFS_RULE,
      NUMBER_TOLERANCE_RULE, TOLERANCE_MAP_RULE, WITH_SUMMARY_RULE,
      DEFAULT_WITH_SUMMARY_RULE, GROUP_BY_COLUMN_NAMES_RULE, SPREADSHEET_IS_SORTED_RULE,
      DEFAULT_SPREADSHEET_IS_SORTED_RULE, SPREADSHEET_HAS_HEADER_RULE,
      DEFAULT_SPREADSHEET_HAS_HEADER_RULE, LHS_SPREADSHEET_SHEET_NAME_RULE,
      RHS_SPREADSHEET_SHEET_NAME_RULE, DEFAULT_SPREADSHEET_SHEET_NAME_RULE,
      SPREADSHEET_VALIDATE_LAZILY_RULE, DEFAULT_SPREADSHEET_MODEL_RULE };

   private static class TypeRefinement<T> extends RuleImplementation {
      private final Class<T> _type;

      private TypeRefinement(Class<T> type_) {
         _type = type_;
      }

      @Override
      public boolean applies(DKMagicDependency<?> dependency_, DKMagicPlan providedPlan_) {
         if (!super.applies(dependency_, providedPlan_))
            return false;
         if (dependency_.getTargetClass() == _type)
            return false;
         return true;
      }

      @Override
      public Object resolve(DKMagicDependency<?> dependency_, DKMagicPlan providedPlan_) {
         return _type;
      }
   }

   private static class PlanValue extends RuleImplementation {
      private final boolean _valueRequired;

      private PlanValue() {
         this(true);
      }

      private PlanValue(boolean valueRequired_) {
         _valueRequired = valueRequired_;
      }

      @Override
      public boolean applies(DKMagicDependency<?> dependency_, DKMagicPlan providedPlan_) {
         if (_valueRequired)
            return super.applies(dependency_, providedPlan_);

         boolean targetDependentClassMatches = this.targetDependentClassMatches(
            dependency_, providedPlan_);
         boolean targetClassMatches = this.targetClassMatches(dependency_, providedPlan_);
         boolean targetParmNameMatches = this.targetParmNameMatches(dependency_,
            providedPlan_);
         _rule.getLog().trace("_rule->{}", _rule);
         _rule.getLog().trace("targetDependentClassMatches->{}",
            targetDependentClassMatches);
         _rule.getLog().trace("targetClassMatches->{}", targetClassMatches);
         _rule.getLog().trace("targetParmNameMatches->{}", targetParmNameMatches);
         return (targetDependentClassMatches && targetClassMatches && targetParmNameMatches);
      }

      @Override
      public Object resolve(DKMagicDependency<?> dependency_, DKMagicPlan providedPlan_) {
         return DKKeyValueCoder.getInstance().getValueAtPath(
            this.getRule().getMagicPlanKey(), providedPlan_);
      }
   }

   private static class Constant extends RuleImplementation {
      private final Object _constantValue;

      private Constant(Object constantValue_) {
         _constantValue = constantValue_;
      }

      @Override
      public Object resolve(DKMagicDependency<?> dependency_, DKMagicPlan providedPlan_) {
         return _constantValue;
      }
   }

   private static class DefaultSink extends RuleImplementation {

      @Override
      public Object resolve(DKMagicDependency<?> dependency_, DKMagicPlan providedPlan_) {
         try {
            return new DKWriterSink(
               new BufferedWriter(new OutputStreamWriter(System.out)), null);
         }
         catch (IOException e_) {
            throw new RuntimeException(e_);
         }
      }
   }
}
