/*
 * Copyright (C) 2017 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http:
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.io;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.api.VmOptions;
import com.google.common.base.Optional;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Random;

/**
 * Benchmarks for various potential implementations of {@code ByteSource.asCharSource(...).read()}.
 */
@VmOptions({"-Xms12g", "-Xmx12g", "-d64"})
public class ByteSourceAsCharSourceReadBenchmark {
  enum ReadStrategy {
    TO_BYTE_ARRAY_NEW_STRING {
      @Override
      String read(ByteSource byteSource, Charset cs) throws IOException {
        return new String(byteSource.read(), cs);
      }
    },
    USING_CHARSTREAMS_COPY {
      @Override
      String read(ByteSource byteSource, Charset cs) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (InputStreamReader reader = new InputStreamReader(byteSource.openStream(), cs)) {
          CharStreams.copy(reader, sb);
        }
        return sb.toString();
      }
    },
    USING_DECODER_WITH_SIZE_HINT {
      @Override
      String read(ByteSource byteSource, Charset cs) throws IOException {
        Optional<Long> size = byteSource.sizeIfKnown();
        if (size.isPresent() && size.get().longValue() == size.get().intValue()) {
          int maxChars = (int) (size.get().intValue() * cs.newDecoder().maxCharsPerByte());
          char[] buffer = new char[maxChars];
          int bufIndex = 0;
          int remaining = buffer.length;
          try (InputStreamReader reader = new InputStreamReader(byteSource.openStream(), cs)) {
            int nRead = 0;
            while (remaining > 0 && (nRead = reader.read(buffer, bufIndex, remaining)) != -1) {
              bufIndex += nRead;
              remaining -= nRead;
            }
            if (nRead == -1) {
              return new String(buffer, 0, bufIndex);
            }
            StringBuilder builder = new StringBuilder(bufIndex + 32);
            builder.append(buffer, 0, bufIndex);
            buffer = null; 
            CharStreams.copy(reader, builder);
            return builder.toString();
          }

        } else {
          return TO_BYTE_ARRAY_NEW_STRING.read(byteSource, cs);
        }
      }
    };

    abstract String read(ByteSource byteSource, Charset cs) throws IOException;
  }

  @Param({"UTF-8"})
  String charsetName;

  @Param ReadStrategy strategy;

  @Param({"10", "1024", "1048576"})
  int size;

  Charset charset;
  ByteSource data;

  @BeforeExperiment
  public void setUp() {
    charset = Charset.forName(charsetName);
    StringBuilder sb = new StringBuilder();
    Random random = new Random(0xdeadbeef); 
    sb.ensureCapacity(size);
    for (int k = 0; k < size; k++) {
      sb.append((char) (random.nextInt(127 - 9) + 9));
    }
    String string = sb.toString();
    sb.setLength(0);
    data = ByteSource.wrap(string.getBytes(charset));
  }

  @Benchmark
  public int timeCopy(int reps) throws IOException {
    int r = 0;
    final Charset localCharset = charset;
    final ByteSource localData = data;
    final ReadStrategy localStrategy = strategy;
    for (int i = 0; i < reps; i++) {
      r += localStrategy.read(localData, localCharset).hashCode();
    }
    return r;
  }
}
