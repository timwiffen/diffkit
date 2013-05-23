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
package org.diffkit.util.tst



import org.diffkit.diff.sns.DKListSink;
import org.diffkit.diff.sns.DKPoiSheet;
import org.diffkit.util.DKClassUtil;

import groovy.util.GroovyTestCase;


/**
 * @author jpanico
 */
public class TestClassUtil extends GroovyTestCase {
   
   public void testFindStaticField(){
      assert !DKClassUtil.findStaticField( 'DOES_NOT_EXIST', DKPoiSheet.class)
      assert DKClassUtil.findStaticField( 'HANDLED_FILE_EXTENSIONS', DKPoiSheet.class)
      // doesn't find instance members
      assert !DKClassUtil.findStaticField( '_workbook', DKPoiSheet.class)
      // finds static field in the super of the target, not target
      assert DKClassUtil.findStaticField( 'COLUMN_CLUSTER_KEY_SEPARATOR', DKListSink.class)
   }
   
   public void testCreateInstance(){
      
      def created = DKClassUtil.createInstance(String.class, "hello")
      assert created
      assert created == 'hello'
      
      shouldFail(NoSuchMethodException) { 
         DKClassUtil.createInstance(DKListSink.class, "hello")
      }
   }
}
