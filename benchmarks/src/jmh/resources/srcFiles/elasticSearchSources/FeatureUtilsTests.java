/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.core.ml.inference.preprocessing.customwordembedding;

import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.CoreMatchers.equalTo;

public class FeatureUtilsTests extends ESTestCase {

    public void testValidUTF8Length() {
        {
            String strAZ = " a az qalıb breyn rinq intellektual oyunu üzrə yarışın zona mərhələləri "
                + "keçirilib miq un qalıqlarının dənizdən çıxarılması davam edir məhəmməd "
                + "peyğəmbərin karikaturalarını";

            String truncated = FeatureUtils.truncateToNumValidBytes(strAZ, 200);
            assertThat(truncated, equalTo(strAZ));
        }
        {
            String strBE = " а друкаваць іх не было тэхнічна магчыма бліжэй за вільню тым самым часам "
                + "нямецкае кіраўніцтва прапаноўвала апроч ўвядзення лацінкі яе";

            String truncated = FeatureUtils.truncateToNumValidBytes(strBE, 200);
            assertThat(
                truncated,
                equalTo(" а друкаваць іх не было тэхнічна магчыма бліжэй за вільню тым самым часам " + "нямецкае кіраўніцтва прапаноўвала ")
            );
        }
        {
            String strAR = "احتيالية بيع أي حساب";

            String truncated = FeatureUtils.truncateToNumValidBytes(strAR, 200);
            assertThat(truncated, equalTo(strAR));
        }
        {
            String strZH = "产品的简报和公告 提交该申请后无法进行更改 请确认您的选择是正确的 " + "对于要提交的图书 我确认 我是版权所有者或已得到版权所有者的授权 " + "要更改您的国家 地区 请在此表的最上端更改您的";

            String truncated = FeatureUtils.truncateToNumValidBytes(strZH, 200);
            assertThat(truncated, equalTo("产品的简报和公告 提交该申请后无法进行更改 请确认您的选择是正确的 " + "对于要提交的图书 我确认 我是版权所有者或已得到版权所有者的授权 " + "要更改"));
        }
    }

    public void testCleanText() {
        assertThat(
            FeatureUtils.cleanAndLowerText("This has a tag in <br> it <ssss>&..
            equalTo(" this has a tag in br it ssss winter ")
        );

        assertThat(
            FeatureUtils.cleanAndLowerText(" This has a tag in <br> it <ssss>&..
            equalTo(" this has a tag in br it ssss winter ")
        );

        assertThat(
            FeatureUtils.cleanAndLowerText(" This has a tag in <p> it </p><ssss>&..
            equalTo(" this has a tag in p it p ssss winter ")
        );

        assertThat(
            FeatureUtils.cleanAndLowerText("  This has a tag in \n<p> it \r\n</p><ssss>&..
            equalTo(" this has a tag in p it p ssss winter ")
        );

        assertThat(
            FeatureUtils.cleanAndLowerText(" !This has    a tag.in\n+|iW£#   <p> hello\nit </p><ssss>&..
            equalTo(" this has a tag in iw p hello it p ssss winter ")
        );

        assertThat(FeatureUtils.cleanAndLowerText("北京——。"), equalTo(" 北京 "));
        assertThat(FeatureUtils.cleanAndLowerText("北京——中国共产党已为国家主席习近平或许无限期地继续执政扫清了道路。"), equalTo(" 北京 中国共产党已为国家主席习近平或许无限期地继续执政扫清了道路 "));
    }
}
