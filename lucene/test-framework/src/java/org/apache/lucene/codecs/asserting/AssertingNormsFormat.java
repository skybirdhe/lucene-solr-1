package org.apache.lucene.codecs.asserting;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import org.apache.lucene.codecs.NormsConsumer;
import org.apache.lucene.codecs.NormsFormat;
import org.apache.lucene.codecs.NormsProducer;
import org.apache.lucene.codecs.lucene49.Lucene49NormsFormat;
import org.apache.lucene.index.AssertingAtomicReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.SegmentWriteState;

/**
 * Just like {@link Lucene49NormsFormat} but with additional asserts.
 */
public class AssertingNormsFormat extends NormsFormat {
  private final NormsFormat in = new Lucene49NormsFormat();
  
  @Override
  public NormsConsumer normsConsumer(SegmentWriteState state) throws IOException {
    NormsConsumer consumer = in.normsConsumer(state);
    assert consumer != null;
    return new AssertingNormsConsumer(consumer, state.segmentInfo.getDocCount());
  }

  @Override
  public NormsProducer normsProducer(SegmentReadState state) throws IOException {
    assert state.fieldInfos.hasNorms();
    NormsProducer producer = in.normsProducer(state);
    assert producer != null;
    return new AssertingNormsProducer(producer, state.segmentInfo.getDocCount());
  }
  
  static class AssertingNormsConsumer extends NormsConsumer {
    private final NormsConsumer in;
    private final int maxDoc;
    
    AssertingNormsConsumer(NormsConsumer in, int maxDoc) {
      this.in = in;
      this.maxDoc = maxDoc;
    }

    @Override
    public void addNormsField(FieldInfo field, Iterable<Number> values) throws IOException {
      int count = 0;
      for (Number v : values) {
        assert v != null;
        count++;
      }
      assert count == maxDoc;
      AssertingDocValuesFormat.checkIterator(values.iterator(), maxDoc, false);
      in.addNormsField(field, values);
    }
    
    @Override
    public void close() throws IOException {
      in.close();
    }
  }
  
  static class AssertingNormsProducer extends NormsProducer {
    private final NormsProducer in;
    private final int maxDoc;
    
    AssertingNormsProducer(NormsProducer in, int maxDoc) {
      this.in = in;
      this.maxDoc = maxDoc;
    }

    @Override
    public NumericDocValues getNorms(FieldInfo field) throws IOException {
      assert field.getNormType() == FieldInfo.DocValuesType.NUMERIC;
      NumericDocValues values = in.getNorms(field);
      assert values != null;
      return new AssertingAtomicReader.AssertingNumericDocValues(values, maxDoc);
    }

    @Override
    public void close() throws IOException {
      in.close();
    }

    @Override
    public long ramBytesUsed() {
      return in.ramBytesUsed();
    }

    @Override
    public void checkIntegrity() throws IOException {
      in.checkIntegrity();
    }
  }
}
