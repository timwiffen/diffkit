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

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.ClassUtils;
import org.diffkit.common.DKValidate;
import org.diffkit.diff.engine.DKContext;
import org.diffkit.diff.engine.DKSource;
import org.diffkit.diff.engine.DKTableModel;
import org.diffkit.util.DKObjectUtil;
import org.diffkit.util.DKStringUtil;

/**
 * @author jpanico
 */
public class DKListSource implements DKSource {

   private final DKTableModel _model;
   private final List<Object[]> _rows;
   private final Iterator<Object[]> _iterator;
   private long _lastIndex = -1;

   public DKListSource(DKTableModel model_, List<Object[]> rows_) {
      _model = model_;
      _rows = rows_;
      _iterator = (_rows == null ? null : _rows.iterator());

      DKValidate.notNull(_model);
   }

   public Kind getKind() {
      return Kind.MEMORY;
   }

   public DKTableModel getModel() {
      return _model;
   }

   public Object[] getNextRow() {
      if (_iterator == null)
         return null;
      if (!_iterator.hasNext())
         return null;
      _lastIndex++;
      return _iterator.next();
   }

   public URI getURI() {
      return DKStringUtil.createURI(String.format("heap://%s",
         DKObjectUtil.getAddressHexString(this)));
   }

   public String toString() {
      return String.format("%s[%s]", ClassUtils.getShortClassName(this.getClass()),
         this.getURI());
   }

   public long getLastIndex() {
      return _lastIndex;
   }

//   @Override
   public void close(DKContext context_) throws IOException {
   }

//   @Override
   public void open(DKContext context_) throws IOException {
   }
}
