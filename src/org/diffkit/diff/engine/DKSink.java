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
package org.diffkit.diff.engine;

import java.io.IOException;

/**
 * Sinks are one-shot-- a new one must be created for each invocation of
 * DKDiffEnging.diff(...) and then thrown away.
 * 
 * @author jpanico
 */
public interface DKSink extends DKSourceSink {

   public void record(DKDiff diff_, DKContext context_) throws IOException;

   /**
    * includes DKDiffs of any Kind
    */
   public long getDiffCount();

   public long getRowDiffCount();

   public long getColumnDiffCount();

   public String generateSummary(DKContext context_);

}
