/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.analysis.common;

import org.apache.lucene.analysis.CharacterUtils;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.util.CharTokenizer;

import java.io.IOException;

@Deprecated
class XLowerCaseTokenizer extends Tokenizer {

    private int offset = 0, bufferIndex = 0, dataLen = 0, finalOffset = 0;

    private static final int IO_BUFFER_SIZE = 4096;

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

    private final CharacterUtils.CharacterBuffer ioBuffer = CharacterUtils.newCharacterBuffer(IO_BUFFER_SIZE);

    @Override
    public final boolean incrementToken() throws IOException {
        clearAttributes();
        int length = 0;
        int start = -1; 
        int end = -1;
        char[] buffer = termAtt.buffer();
        while (true) {
            if (bufferIndex >= dataLen) {
                offset += dataLen;
                CharacterUtils.fill(ioBuffer, input); 
                if (ioBuffer.getLength() == 0) {
                    dataLen = 0; 
                    if (length > 0) {
                        break;
                    } else {
                        finalOffset = correctOffset(offset);
                        return false;
                    }
                }
                dataLen = ioBuffer.getLength();
                bufferIndex = 0;
            }
            final int c = Character.codePointAt(ioBuffer.getBuffer(), bufferIndex, ioBuffer.getLength());
            final int charCount = Character.charCount(c);
            bufferIndex += charCount;

            if (Character.isLetter(c)) {               
                if (length == 0) {                
                    assert start == -1;
                    start = offset + bufferIndex - charCount;
                    end = start;
                } else if (length >= buffer.length - 1) { 
                    buffer = termAtt.resizeBuffer(2 + length); 
                }
                end += charCount;
                length += Character.toChars(Character.toLowerCase(c), buffer, length); 
                int maxTokenLen = CharTokenizer.DEFAULT_MAX_WORD_LEN;
                if (length >= maxTokenLen) { 
                    break;
                }
            } else if (length > 0) {           
                break;                           
            }
        }

        termAtt.setLength(length);
        assert start != -1;
        offsetAtt.setOffset(correctOffset(start), finalOffset = correctOffset(end));
        return true;

    }

    @Override
    public final void end() throws IOException {
        super.end();
        offsetAtt.setOffset(finalOffset, finalOffset);
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        bufferIndex = 0;
        offset = 0;
        dataLen = 0;
        finalOffset = 0;
        ioBuffer.reset(); 
    }

}
