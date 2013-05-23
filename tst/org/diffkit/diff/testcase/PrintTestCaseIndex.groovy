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
package org.diffkit.diff.testcase

import org.apache.commons.lang.ClassUtils 
import org.apache.commons.lang.StringUtils;

import org.diffkit.util.DKStringUtil 


/**
 * @author jpanico
 */
public class PrintTestCaseIndex {
   
   public static void main(String[] args_){
      def instance = new PrintTestCaseIndex()
      
      instance.writeIndexFile('TestCases.txt')
   }
   
   private void writeIndexFile(String filename_){
      println "filename_->$filename_"
      def readmeDirPath = this.defaultDataPath
      println "readmeDirPath->$readmeDirPath"
      URL dataPathUrl = this.class.classLoader.getResource(readmeDirPath)
      println "dataPathUrl->$dataPathUrl"
      File dataDir = [dataPathUrl.toURI()]
      println "dataDir->$dataDir"
      File indexFile = [dataDir, filename_]
      println "indexFile->$indexFile"
      def readmeFileList = dataDir.listFiles( {dir, fileName-> fileName ==~ /.*?\.README\.txt/ } as FilenameFilter )
      println "readmeFileList->$readmeFileList" 
      Arrays.sort( readmeFileList, {left, right->
         DKStringUtil.StringNumberComparator.INSTANCE.compare(left.name, right.name)
      } as Comparator 
      )
      println "readmeFileList->$readmeFileList"
      
      if(indexFile.exists())
         indexFile.delete()
      
      readmeFileList.each { 
         def matcher = it.text =~ /(test\d+) README\s+=+\s+Description\s+-+\s+(?s:(.+(?=\s+Assumptions)))/
         indexFile <<  matcher[0][1].trim() << '\n'
         indexFile << StringUtils.repeat('-', matcher[0][1].trim().size())  << '\n'
         indexFile <<  matcher[0][2].trim() << '\n\n'
      }
   }
   
   private  String getDefaultDataPath(){
      return DKStringUtil.packageNameToResourcePath(ClassUtils.getPackageName(this.getClass()) )
   }
}
