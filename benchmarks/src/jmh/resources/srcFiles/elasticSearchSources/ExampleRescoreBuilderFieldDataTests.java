/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.example.rescore;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHits;
import org.apache.lucene.store.Directory;
import org.apache.lucene.tests.index.RandomIndexWriter;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.compress.CompressedXContent;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.search.rescore.RescoreContext;
import org.elasticsearch.test.AbstractBuilderTestCase;

import java.io.IOException;

public class ExampleRescoreBuilderFieldDataTests extends AbstractBuilderTestCase {


    private String fieldFactorFieldName = "literalNameOfFieldUsedAsFactor";
    private float fieldFactorValue = 2.0f;

    private IndexSearcher getSearcher(IndexReader r) {
        IndexSearcher searcher = newSearcher(r);
        return searcher;
    }

    private IndexReader publishDocs(int numDocs, String fieldName, Directory dir) throws Exception {
        RandomIndexWriter w = new RandomIndexWriter(random(), dir, newIndexWriterConfig());
        for (int i = 0; i < numDocs; i++) {
            Document d = new Document();
            d.add(newStringField("id", Integer.toString(i), Field.Store.YES));
            d.add(new FloatField(fieldName, fieldFactorValue, Field.Store.YES ));
            w.addDocument(d);
        }
        IndexReader reader = w.getReader();
        w.close();
        return reader;
    }
    @Override
    protected void initializeAdditionalMappings(MapperService mapperService) throws IOException {

        mapperService.merge(
            "_doc",
            new CompressedXContent(Strings.toString(PutMappingRequest.simpleMapping(fieldFactorFieldName, "type=float"))),
            MapperService.MergeReason.MAPPING_UPDATE
        );
    }


    public void testRescoreUsingFieldData() throws Exception {
        float originalScoreOfTopDocs = 1.0f;

        float factor = (float) randomDoubleBetween(1.0d, Float.MAX_VALUE/(fieldFactorValue * originalScoreOfTopDocs)-1, false);


        Directory dir = newDirectory();
        int numDocs = 3;
        IndexReader reader = publishDocs(numDocs, fieldFactorFieldName, dir);
        IndexSearcher searcher = getSearcher(reader);

        ExampleRescoreBuilder builder = new ExampleRescoreBuilder(factor, fieldFactorFieldName).windowSize(2);

        RescoreContext context = builder.buildContext(createSearchExecutionContext(searcher));

        TopDocs docs = new TopDocs(new TotalHits(10, TotalHits.Relation.EQUAL_TO), new ScoreDoc[3]);
        docs.scoreDocs[0] = new ScoreDoc(0, originalScoreOfTopDocs);
        docs.scoreDocs[1] = new ScoreDoc(1, originalScoreOfTopDocs);
        docs.scoreDocs[2] = new ScoreDoc(2, originalScoreOfTopDocs);
        context.rescorer().rescore(docs, searcher, context);

        assertEquals(originalScoreOfTopDocs*factor*fieldFactorValue, docs.scoreDocs[0].score, 0.0f);
        assertEquals(originalScoreOfTopDocs*factor*fieldFactorValue, docs.scoreDocs[1].score, 0.0f);
        assertEquals(originalScoreOfTopDocs, docs.scoreDocs[2].score, 0.0f);

        reader.close();
        dir.close();
    }
}
