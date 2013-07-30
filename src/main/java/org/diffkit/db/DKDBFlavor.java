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
package org.diffkit.db;

import org.diffkit.common.DKValidate;

/**
 * @author jpanico
 */
public enum DKDBFlavor {
   H2("org.h2.Driver"), MYSQL("com.mysql.jdbc.Driver"), ORACLE(
      "oracle.jdbc.driver.OracleDriver"), DB2("com.ibm.db2.jcc.DB2Driver"), SQLSERVER(
      "net.sourceforge.jtds.jdbc.Driver"), POSTGRES("org.postgresql.Driver",
      true, true), HYPERSQL("org.hsqldb.jdbc.JDBCDriver"), SYBASE("");

   public final String _driverName;
   public final boolean _ignoreUnrecognizedTypes;
   public final boolean _caseSensitive;

   private DKDBFlavor(String driverName_) {
      this(driverName_, false, false);
   }

   private DKDBFlavor(String driverName_, boolean ignoreUnrecognizedTypes_,
                      boolean caseSensitive_) {
      _driverName = driverName_;
      _ignoreUnrecognizedTypes = ignoreUnrecognizedTypes_;
      _caseSensitive = caseSensitive_;
      DKValidate.notNull(_driverName);
   }

   /**
    * will simply return null if argument is not recognized, instead of throwing
    */
   public static DKDBFlavor forName(String name_) {
      if (name_ == null)
         return null;

      try {
         return Enum.valueOf(DKDBFlavor.class, name_);
      }
      catch (Exception e_) {
         return null;
      }
   }
}
