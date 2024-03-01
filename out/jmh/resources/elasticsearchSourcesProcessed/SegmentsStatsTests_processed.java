/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.index.engine;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.NoMergePolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.elasticsearch.index.store.LuceneFilesExtensions;
import org.elasticsearch.test.ESTestCase;

public class SegmentsStatsTests extends ESTestCase {

    public void testFileExtensionDescriptions() throws Exception {
        try (Directory dir = newDirectory()) {
            try (
                IndexWriter w = new IndexWriter(
                    dir,
                    new IndexWriterConfig().setUseCompoundFile(false).setMergePolicy(NoMergePolicy.INSTANCE)
                )
            ) {
                Document doc = new Document();
                StringField id = new StringField("id", "1", Store.YES);
                doc.add(id);
                doc.add(new LongPoint("lp", 42L));
                doc.add(new NumericDocValuesField("ndv", 42L));
                FieldType ft = new FieldType(TextField.TYPE_STORED);
                ft.setStoreTermVectors(true);
                doc.add(new Field("if", "elasticsearch", ft));
                w.addDocument(doc);
                id.setStringValue("2");
                w.addDocument(doc);
                w.deleteDocuments(new Term("id", "2"));
            }

            for (String file : dir.listAll()) {
                final String extension = IndexFileNames.getExtension(file);
                if ("lock".equals(extension)) {
                    continue;
                }
                if (extension != null) {
                    assertNotNull(
                        "extension [" + extension + "] was not contained in the known segment stats files",
                        LuceneFilesExtensions.fromExtension(extension)
                    );
                }
            }
        }
    }

}
