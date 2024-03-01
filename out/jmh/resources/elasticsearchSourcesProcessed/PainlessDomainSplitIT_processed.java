/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.ml.transforms;

import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.rest.ESRestTestCase;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.elasticsearch.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class PainlessDomainSplitIT extends ESRestTestCase {

    private static final String BASE_PATH = "/_ml/";

    static class TestConfiguration {
        public String subDomainExpected;
        public String domainExpected;
        public String hostName;

        TestConfiguration(String subDomainExpected, String domainExpected, String hostName) {
            this.subDomainExpected = subDomainExpected;
            this.domainExpected = domainExpected;
            this.hostName = hostName;
        }
    }

    public static final ArrayList<TestConfiguration> tests;

    static {
        tests = new ArrayList<>();

        tests.add(new TestConfiguration("", "", ""));
        tests.add(new TestConfiguration("", "", "."));


        tests.add(new TestConfiguration("www", "google.com", "www.google.com"));
        tests.add(new TestConfiguration("www.maps", "google.co.uk", "www.maps.google.co.uk"));
        tests.add(new TestConfiguration("www", "theregister.co.uk", "www.theregister.co.uk"));
        tests.add(new TestConfiguration("", "gmail.com", "gmail.com"));
        tests.add(new TestConfiguration("media.forums", "theregister.co.uk", "media.forums.theregister.co.uk"));
        tests.add(new TestConfiguration("www", "www.com", "www.www.com"));
        tests.add(new TestConfiguration("", "www.com", "www.com"));
        tests.add(new TestConfiguration("", "internalunlikelyhostname", "internalunlikelyhostname"));
        tests.add(new TestConfiguration("internalunlikelyhostname", "bizarre", "internalunlikelyhostname.bizarre"));
        tests.add(new TestConfiguration("", "internalunlikelyhostname.info", "internalunlikelyhostname.info"));  
        tests.add(new TestConfiguration("internalunlikelyhostname", "information", "internalunlikelyhostname.information"));
        tests.add(new TestConfiguration("", "216.22.0.192", "216.22.0.192"));
        tests.add(new TestConfiguration("", "::1", "::1"));
        tests.add(new TestConfiguration("", "FE80:0000:0000:0000:0202:B3FF:FE1E:8329", "FE80:0000:0000:0000:0202:B3FF:FE1E:8329"));
        tests.add(new TestConfiguration("216.22", "project.coop", "216.22.project.coop"));
        tests.add(new TestConfiguration("www", "xn--h1alffa9f.xn--p1ai", "www.xn--h1alffa9f.xn--p1ai"));
        tests.add(new TestConfiguration("", "", ""));
        tests.add(new TestConfiguration("www", "parliament.uk", "www.parliament.uk"));
        tests.add(new TestConfiguration("www", "parliament.co.uk", "www.parliament.co.uk"));
        tests.add(new TestConfiguration("www.a", "cgs.act.edu.au", "www.a.cgs.act.edu.au"));
        tests.add(new TestConfiguration("www", "google.com.au", "www.google.com.au"));
        tests.add(new TestConfiguration("www", "metp.net.cn", "www.metp.net.cn"));
        tests.add(new TestConfiguration("www", "waiterrant.blogspot.com", "www.waiterrant.blogspot.com"));
        tests.add(new TestConfiguration("", "kittens.blogspot.co.uk", "kittens.blogspot.co.uk"));
        tests.add(new TestConfiguration("", "prelert.s3.amazonaws.com", "prelert.s3.amazonaws.com"));
        tests.add(new TestConfiguration("daves_bucket", "prelert.s3.amazonaws.com", "daves_bucket.prelert.s3.amazonaws.com"));
        tests.add(new TestConfiguration("example", "example", "example.example"));
        tests.add(new TestConfiguration("b.example", "example", "b.example.example"));
        tests.add(new TestConfiguration("a.b.example", "example", "a.b.example.example"));
        tests.add(new TestConfiguration("example", "local", "example.local"));
        tests.add(new TestConfiguration("b.example", "local", "b.example.local"));
        tests.add(new TestConfiguration("a.b.example", "local", "a.b.example.local"));
        tests.add(
            new TestConfiguration(
                "r192494180984795-1-1041782-channel-live.ums",
                "ustream.tv",
                "r192494180984795-1-1041782-cha" + "nnel-live.ums.ustream.tv"
            )
        );
        tests.add(new TestConfiguration("192.168.62.9", "prelert.com", "192.168.62.9.prelert.com"));

        tests.add(new TestConfiguration("kerberos.http.192.168", "62.222", "kerberos.http.192.168.62.222"));

        /*
        String dnsLongerThan254Chars = "davesbucketdavesbucketdavesbucketdavesbucketdavesbucketdaves.bucketdavesbucketdavesbuc" +
                "ketdavesbucketdavesbucketdaves.bucketdavesbucketdavesbucketdavesbucketdavesbucket.davesbucketdavesbucketdaves" +
                "bucketdavesbucket.davesbucketdavesbucket.prelert.s3.amazonaws.com";
        String hrd = "prelert.s3.amazonaws.com";
        tests.add(new TestConfiguration(dnsLongerThan254Chars.substring(0, dnsLongerThan254Chars.length() - (hrd.length() + 1)),
                hrd, dnsLongerThan254Chars));
        */


        tests.add(new TestConfiguration("_example", "local", "_example.local"));
        tests.add(new TestConfiguration("www._maps", "google.co.uk", "www._maps.google.co.uk"));
        tests.add(new TestConfiguration("-forum", "theregister.co.uk", "-forum.theregister.co.uk"));
        tests.add(new TestConfiguration("www._yourmp", "parliament.uk", "www._yourmp.parliament.uk"));
        tests.add(new TestConfiguration("www.-a", "cgs.act.edu.au", "www.-a.cgs.act.edu.au"));
        tests.add(new TestConfiguration("", "-foundation.org", "-foundation.org"));
        tests.add(new TestConfiguration("www", "-foundation.org", "www.-foundation.org"));
        tests.add(new TestConfiguration("", "_nfsv4idmapdomain", "_nfsv4idmapdomain"));
        tests.add(new TestConfiguration("_nfsv4idmapdomain", "prelert.com", "_nfsv4idmapdomain.prelert.com"));

        tests.add(new TestConfiguration(null, "example.com", "example.COM"));
        tests.add(new TestConfiguration(null, "example.com", "WwW.example.COM"));

        tests.add(new TestConfiguration(null, "domain.biz", "domain.biz"));
        tests.add(new TestConfiguration(null, "domain.biz", "b.domain.biz"));
        tests.add(new TestConfiguration(null, "domain.biz", "a.b.domain.biz"));

        tests.add(new TestConfiguration(null, "example.com", "example.com"));
        tests.add(new TestConfiguration(null, "example.com", "b.example.com"));
        tests.add(new TestConfiguration(null, "example.com", "a.b.example.com"));
        tests.add(new TestConfiguration(null, "example.uk.com", "example.uk.com"));
        tests.add(new TestConfiguration(null, "example.uk.com", "b.example.uk.com"));
        tests.add(new TestConfiguration(null, "example.uk.com", "a.b.example.uk.com"));
        tests.add(new TestConfiguration(null, "test.ac", "test.ac"));
        tests.add(new TestConfiguration(null, "c.gov.cy", "c.gov.cy"));
        tests.add(new TestConfiguration(null, "c.gov.cy", "b.c.gov.cy"));
        tests.add(new TestConfiguration(null, "c.gov.cy", "a.b.c.gov.cy"));

        tests.add(new TestConfiguration(null, "test.jp", "test.jp"));
        tests.add(new TestConfiguration(null, "test.jp", "www.test.jp"));
        tests.add(new TestConfiguration(null, "test.ac.jp", "test.ac.jp"));
        tests.add(new TestConfiguration(null, "test.ac.jp", "www.test.ac.jp"));
        tests.add(new TestConfiguration(null, "test.kyoto.jp", "test.kyoto.jp"));
        tests.add(new TestConfiguration(null, "b.ide.kyoto.jp", "b.ide.kyoto.jp"));
        tests.add(new TestConfiguration(null, "b.ide.kyoto.jp", "a.b.ide.kyoto.jp"));
        tests.add(new TestConfiguration(null, "city.kobe.jp", "city.kobe.jp"));
        tests.add(new TestConfiguration(null, "city.kobe.jp", "www.city.kobe.jp"));
        tests.add(new TestConfiguration(null, "test.us", "test.us"));
        tests.add(new TestConfiguration(null, "test.us", "www.test.us"));
        tests.add(new TestConfiguration(null, "test.ak.us", "test.ak.us"));
        tests.add(new TestConfiguration(null, "test.ak.us", "www.test.ak.us"));
        tests.add(new TestConfiguration(null, "test.k12.ak.us", "test.k12.ak.us"));
        tests.add(new TestConfiguration(null, "test.k12.ak.us", "www.test.k12.ak.us"));
�司.cn", "食狮.公司.cn"));
�司.cn", "www.食狮.公司.cn"));
�司.cn", "shishi.公司.cn"));

        tests.add(new TestConfiguration(null, "xn--85x722f.com.cn", "xn--85x722f.com.cn"));
        tests.add(new TestConfiguration(null, "xn--85x722f.xn--55qx5d.cn", "xn--85x722f.xn--55qx5d.cn"));
        tests.add(new TestConfiguration(null, "xn--85x722f.xn--55qx5d.cn", "www.xn--85x722f.xn--55qx5d.cn"));
        tests.add(new TestConfiguration(null, "shishi.xn--55qx5d.cn", "shishi.xn--55qx5d.cn"));
        tests.add(new TestConfiguration(null, "xn--85x722f.xn--fiqs8s", "xn--85x722f.xn--fiqs8s"));
        tests.add(new TestConfiguration(null, "xn--85x722f.xn--fiqs8s", "www.xn--85x722f.xn--fiqs8s"));
        tests.add(new TestConfiguration(null, "shishi.xn--fiqs8s", "shishi.xn--fiqs8s"));
    }

    public void testIsolated() throws Exception {
        Settings.Builder settings = Settings.builder()
            .put(IndexMetadata.INDEX_NUMBER_OF_SHARDS_SETTING.getKey(), 1)
            .put(IndexMetadata.INDEX_NUMBER_OF_REPLICAS_SETTING.getKey(), 0);

        createIndex("painless", settings.build());
        Request createDoc = new Request("PUT", "/painless/_doc/1");
        createDoc.setJsonEntity("{\"test\": \"test\"}");
        createDoc.addParameter("refresh", "true");
        client().performRequest(createDoc);

        Pattern pattern = Pattern.compile("domain_split\":\\[(.*?),(.*?)\\]");

        Map<String, Object> params = new HashMap<>();
        for (TestConfiguration testConfig : tests) {
            params.put("host", testConfig.hostName);
            String mapAsJson = Strings.toString(jsonBuilder().map(params));
            logger.info("params={}", mapAsJson);

            Request searchRequest = new Request("GET", "/painless/_search");
            searchRequest.setJsonEntity(Strings.format("""
                {
                    "query" : {
                        "match_all": {}
                    },
                    "script_fields" : {
                        "domain_split" : {
                            "script" : {
                                "lang": "painless",
                                "source": " return domainSplit(params['host']); ",
                                "params": %s
                            }
                        }
                    }
                }""", mapAsJson));
            String responseBody = EntityUtils.toString(client().performRequest(searchRequest).getEntity());
            Matcher m = pattern.matcher(responseBody);

            String actualSubDomain = "";
            String actualDomain = "";
            if (m.find()) {
                actualSubDomain = m.group(1).replace("\"", "");
                actualDomain = m.group(2).replace("\"", "");
            }

            String expectedTotal = "[" + testConfig.subDomainExpected + "," + testConfig.domainExpected + "]";
            String actualTotal = "[" + actualSubDomain + "," + actualDomain + "]";

            if (testConfig.subDomainExpected != null) {
                assertThat(
                    "Expected subdomain ["
                        + testConfig.subDomainExpected
                        + "] but found ["
                        + actualSubDomain
                        + "]. Actual "
                        + actualTotal
                        + " vs Expected "
                        + expectedTotal,
                    actualSubDomain,
                    equalTo(testConfig.subDomainExpected)
                );
            }

            assertThat(
                "Expected domain ["
                    + testConfig.domainExpected
                    + "] but found ["
                    + actualDomain
                    + "].  Actual "
                    + actualTotal
                    + " vs Expected "
                    + expectedTotal,
                actualDomain,
                equalTo(testConfig.domainExpected)
            );
        }
    }

    public void testHRDSplit() throws Exception {
        Request createJobRequest = new Request("PUT", BASE_PATH + "anomaly_detectors/hrd-split-job");
        createJobRequest.setJsonEntity("""
            {
                "description":"Domain splitting",
                "analysis_config" : {
                    "bucket_span":"3600s",
                    "detectors" :[{"function":"count", "by_field_name" : "domain_split"}]
                },
                "data_description" : {
                    "time_field":"time"
                }
            }""");
        client().performRequest(createJobRequest);
        client().performRequest(new Request("POST", BASE_PATH + "anomaly_detectors/hrd-split-job/_open"));

        Settings.Builder settings = Settings.builder()
            .put(IndexMetadata.INDEX_NUMBER_OF_SHARDS_SETTING.getKey(), 1)
            .put(IndexMetadata.INDEX_NUMBER_OF_REPLICAS_SETTING.getKey(), 0);

        createIndex("painless", settings.build(), """
            "properties": { "domain": { "type": "keyword" },"time": { "type": "date" } }""");

        ZonedDateTime baseTime = ZonedDateTime.now(ZoneOffset.UTC).minusYears(1);
        TestConfiguration test = tests.get(randomInt(tests.size() - 1));

        String expectedSub = test.subDomainExpected == null ? ".*" : test.subDomainExpected.replace(".", "\\.");
        String expectedHRD = test.domainExpected.replace(".", "\\.");
        Pattern pattern = Pattern.compile("domain_split\":\\[\"(" + expectedSub + "),(" + expectedHRD + ")\"[,\\]]");

        for (int i = 1; i <= 100; i++) {
            ZonedDateTime time = baseTime.plusHours(i);
            String formattedTime = time.format(DateTimeFormatter.ISO_DATE_TIME);
            if (i % 50 == 0) {
                for (int j = 0; j < 100; j++) {
                    Request createDocRequest = new Request("POST", "/painless/_doc");
                    createDocRequest.setJsonEntity(Strings.format("""
                        {"domain": "bar.bar.com", "time": "%s"}
                        """, formattedTime));
                    client().performRequest(createDocRequest);
                }
            } else {
                Request createDocRequest = new Request("PUT", "/painless/_doc/" + formattedTime);
                createDocRequest.setJsonEntity(Strings.format("""
                    {"domain": "%s", "time": "%s"}
                    """, test.hostName, formattedTime));
                client().performRequest(createDocRequest);
            }
        }

        client().performRequest(new Request("POST", "/painless/_refresh"));

        Request createFeedRequest = new Request("PUT", BASE_PATH + "datafeeds/hrd-split-datafeed");
        createFeedRequest.setJsonEntity("""
            {
               "job_id":"hrd-split-job",
               "indexes":["painless"],
               "script_fields": {
                  "domain_split": {
                     "script": "return domainSplit(doc['domain'].value, params);"
                  }
               }
            }""");

        client().performRequest(createFeedRequest);
        Request startDatafeedRequest = new Request("POST", BASE_PATH + "datafeeds/hrd-split-datafeed/_start");
        startDatafeedRequest.addParameter("start", baseTime.format(DateTimeFormatter.ISO_DATE_TIME));
        startDatafeedRequest.addParameter("end", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME));
        client().performRequest(startDatafeedRequest);

        waitUntilDatafeedIsStopped("hrd-split-datafeed");
        waitUntilJobIsClosed("hrd-split-job");

        client().performRequest(new Request("POST", "/.ml-anomalies-*/_refresh"));

        Response records = client().performRequest(new Request("GET", BASE_PATH + "anomaly_detectors/hrd-split-job/results/records"));
        String responseBody = EntityUtils.toString(records.getEntity());
        assertThat("response body [" + responseBody + "] did not contain [\"count\":2]", responseBody, containsString("\"count\":2"));

        Matcher m = pattern.matcher(responseBody);
        String actualSubDomain = "";
        String actualDomain = "";
        if (m.find()) {
            actualSubDomain = m.group(1).replace("\"", "");
            actualDomain = m.group(2).replace("\"", "");
        }

        String expectedTotal = "[" + test.subDomainExpected + "," + test.domainExpected + "]";
        String actualTotal = "[" + actualSubDomain + "," + actualDomain + "]";

        if (test.subDomainExpected != null) {
            assertThat(
                "Expected subdomain ["
                    + test.subDomainExpected
                    + "] but found ["
                    + actualSubDomain
                    + "]. Actual "
                    + actualTotal
                    + " vs Expected "
                    + expectedTotal,
                actualSubDomain,
                equalTo(test.subDomainExpected)
            );
        }

        assertThat(
            "Expected domain ["
                + test.domainExpected
                + "] but found ["
                + actualDomain
                + "].  Actual "
                + actualTotal
                + " vs Expected "
                + expectedTotal,
            actualDomain,
            equalTo(test.domainExpected)
        );
    }

    private void waitUntilJobIsClosed(String jobId) throws Exception {
        assertBusy(() -> {
            try {
                Response jobStatsResponse = client().performRequest(
                    new Request("GET", BASE_PATH + "anomaly_detectors/" + jobId + "/_stats")
                );
                assertThat(EntityUtils.toString(jobStatsResponse.getEntity()), containsString("\"state\":\"closed\""));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void waitUntilDatafeedIsStopped(String dfId) throws Exception {
        assertBusy(() -> {
            try {
                Response datafeedStatsResponse = client().performRequest(new Request("GET", BASE_PATH + "datafeeds/" + dfId + "/_stats"));
                assertThat(EntityUtils.toString(datafeedStatsResponse.getEntity()), containsString("\"state\":\"stopped\""));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, 60, TimeUnit.SECONDS);
    }
}
