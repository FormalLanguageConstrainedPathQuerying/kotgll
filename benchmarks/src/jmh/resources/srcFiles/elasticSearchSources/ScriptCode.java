/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 * This Java port of CLD3 was derived from Google's CLD3 project at https:
 */

package org.elasticsearch.xpack.core.ml.inference.preprocessing.customwordembedding;

/**
 * These are the custom script codes that match up to the appropriate id row for the CLD3 weights and quantiles
 *
 * See https:
 */
public enum ScriptCode {
    Common(0),
    Latin(1),
    Greek(2),
    Cyrillic(3),
    Armenian(4),
    Hebrew(5),
    Arabic(6),
    Syriac(7),
    Thaana(8),
    Devanagari(9),
    Bengali(10),
    Gurmukhi(11),
    Gujarati(12),
    Oriya(13),
    Tamil(14),
    Telugu(15),
    Kannada(16),
    Malayalam(17),
    Sinhala(18),
    Thai(19),
    Lao(20),
    Tibetan(21),
    Myanmar(22),
    Georgian(23),
    Hani(24),
    Ethiopic(25),
    Cherokee(26),
    Canadian_Aboriginal(27),
    Ogham(28),
    Runic(29),
    Khmer(30),
    Mongolian(31),
    Undefined_32(32),
    Undefined_33(33),
    Bopomofo(34),
    Undefined_35(35),
    Yi(36),
    Old_Italic(37),
    Gothic(38),
    Deseret(39),
    Inherited(40),
    Tagalog(41),
    Hanunoo(42),
    Buhid(43),
    Tagbanwa(44),
    Limbu(45),
    Tai_Le(46),
    Linear_B(47),
    Ugaritic(48),
    Shavian(49),
    Osmanya(50),
    Cypriot(51),
    Braille(52),
    Buginese(53),
    Coptic(54),
    New_Tai_Lue(55),
    Glagolitic(56),
    Tifinagh(57),
    Syloti_Nagri(58),
    Old_Persian(59),
    Kharoshthi(60),
    Balinese(61),
    Cuneiform(62),
    Phoenician(63),
    Phags_Pa(64),
    Nko(65),
    Sundanese(66),
    Lepcha(67),
    Ol_Chiki(68),
    Vai(69),
    Saurashtra(70),
    Kayah_Li(71),
    Rejang(72),
    Lycian(73),
    Carian(74),
    Lydian(75),
    Cham(76),
    Tai_Tham(77),
    Tai_Viet(78),
    Avestan(79),
    Egyptian_Hieroglyphs(80),
    Samaritan(81),
    Lisu(82),
    Bamum(83),
    Javanese(84),
    Meetei_Mayek(85),
    Imperial_Aramaic(86),
    Old_South_Arabian(87),
    Inscriptional_Parthian(88),
    Inscriptional_Pahlavi(89),
    Old_Turkic(90),
    Kaithi(91),
    Batak(92),
    Brahmi(93),
    Mandaic(94),
    Chakma(95),
    Meroitic_Cursive(96),
    Meroitic_Hieroglyphs(97),
    Miao(98),
    Sharada(99),
    Sora_Sompeng(100),
    Takri(101),
    MAX_SCRIPT_CODE(102);

    private final int code;

    ScriptCode(int code) {
        this.code = code;
    }

    public static ScriptCode unicodeScriptToULScript(Character.UnicodeScript scriptId) {
        switch (scriptId) {
            case COMMON:
                return ScriptCode.Common;
            case LATIN:
                return ScriptCode.Latin;
            case GREEK:
                return ScriptCode.Greek;
            case CYRILLIC:
                return ScriptCode.Cyrillic;
            case ARMENIAN:
                return ScriptCode.Armenian;
            case HEBREW:
                return ScriptCode.Hebrew;
            case ARABIC:
                return ScriptCode.Arabic;
            case SYRIAC:
                return ScriptCode.Syriac;
            case THAANA:
                return ScriptCode.Thaana;
            case DEVANAGARI:
                return ScriptCode.Devanagari;
            case BENGALI:
                return ScriptCode.Bengali;
            case GURMUKHI:
                return ScriptCode.Gurmukhi;
            case GUJARATI:
                return ScriptCode.Gujarati;
            case ORIYA:
                return ScriptCode.Oriya;
            case TAMIL:
                return ScriptCode.Tamil;
            case TELUGU:
                return ScriptCode.Telugu;
            case KANNADA:
                return ScriptCode.Kannada;
            case MALAYALAM:
                return ScriptCode.Malayalam;
            case SINHALA:
                return ScriptCode.Sinhala;
            case THAI:
                return ScriptCode.Thai;
            case LAO:
                return ScriptCode.Lao;
            case TIBETAN:
                return ScriptCode.Tibetan;
            case MYANMAR:
                return ScriptCode.Myanmar;
            case GEORGIAN:
                return ScriptCode.Georgian;
            case HANGUL:
            case HAN:       
            case HIRAGANA:  
            case KATAKANA:  
                return ScriptCode.Hani;
            case ETHIOPIC:
                return ScriptCode.Ethiopic;
            case CHEROKEE:
                return ScriptCode.Cherokee;
            case CANADIAN_ABORIGINAL:
                return ScriptCode.Canadian_Aboriginal;
            case OGHAM:
                return ScriptCode.Ogham;
            case RUNIC:
                return ScriptCode.Runic;
            case KHMER:
                return ScriptCode.Khmer;
            case MONGOLIAN:
                return ScriptCode.Mongolian;
            case BOPOMOFO:
                return ScriptCode.Bopomofo;
            case YI:
                return ScriptCode.Yi;
            case OLD_ITALIC:
                return ScriptCode.Old_Italic;
            case GOTHIC:
                return ScriptCode.Gothic;
            case DESERET:
                return ScriptCode.Deseret;
            case INHERITED:
                return ScriptCode.Inherited;
            case TAGALOG:
                return ScriptCode.Tagalog;
            case HANUNOO:
                return ScriptCode.Hanunoo;
            case BUHID:
                return ScriptCode.Buhid;
            case TAGBANWA:
                return ScriptCode.Tagbanwa;
            case LIMBU:
                return ScriptCode.Limbu;
            case TAI_LE:
                return ScriptCode.Tai_Le;
            case LINEAR_B:
                return ScriptCode.Linear_B;
            case UGARITIC:
                return ScriptCode.Ugaritic;
            case SHAVIAN:
                return ScriptCode.Shavian;
            case OSMANYA:
                return ScriptCode.Osmanya;
            case CYPRIOT:
                return ScriptCode.Cypriot;
            case BRAILLE:
                return ScriptCode.Braille;
            case BUGINESE:
                return ScriptCode.Buginese;
            case COPTIC:
                return ScriptCode.Coptic;
            case NEW_TAI_LUE:
                return ScriptCode.New_Tai_Lue;
            case GLAGOLITIC:
                return ScriptCode.Glagolitic;
            case TIFINAGH:
                return ScriptCode.Tifinagh;
            case SYLOTI_NAGRI:
                return ScriptCode.Syloti_Nagri;
            case OLD_PERSIAN:
                return ScriptCode.Old_Persian;
            case KHAROSHTHI:
                return ScriptCode.Kharoshthi;
            case BALINESE:
                return ScriptCode.Balinese;
            case CUNEIFORM:
                return ScriptCode.Cuneiform;
            case PHOENICIAN:
                return ScriptCode.Phoenician;
            case PHAGS_PA:
                return ScriptCode.Phags_Pa;
            case NKO:
                return ScriptCode.Nko;
            case SUNDANESE:
                return ScriptCode.Sundanese;
            case LEPCHA:
                return ScriptCode.Lepcha;
            case OL_CHIKI:
                return ScriptCode.Ol_Chiki;
            case VAI:
                return ScriptCode.Vai;
            case SAURASHTRA:
                return ScriptCode.Saurashtra;
            case KAYAH_LI:
                return ScriptCode.Kayah_Li;
            case REJANG:
                return ScriptCode.Rejang;
            case LYCIAN:
                return ScriptCode.Lycian;
            case CARIAN:
                return ScriptCode.Carian;
            case LYDIAN:
                return ScriptCode.Lydian;
            case CHAM:
                return ScriptCode.Cham;
            case TAI_THAM:
                return ScriptCode.Tai_Tham;
            case TAI_VIET:
                return ScriptCode.Tai_Viet;
            case AVESTAN:
                return ScriptCode.Avestan;
            case EGYPTIAN_HIEROGLYPHS:
                return ScriptCode.Egyptian_Hieroglyphs;
            case SAMARITAN:
                return ScriptCode.Samaritan;
            case LISU:
                return ScriptCode.Lisu;
            case BAMUM:
                return ScriptCode.Bamum;
            case JAVANESE:
                return ScriptCode.Javanese;
            case MEETEI_MAYEK:
                return ScriptCode.Meetei_Mayek;
            case IMPERIAL_ARAMAIC:
                return ScriptCode.Imperial_Aramaic;
            case OLD_SOUTH_ARABIAN:
                return ScriptCode.Old_South_Arabian;
            case INSCRIPTIONAL_PARTHIAN:
                return ScriptCode.Inscriptional_Parthian;
            case INSCRIPTIONAL_PAHLAVI:
                return ScriptCode.Inscriptional_Pahlavi;
            case OLD_TURKIC:
                return ScriptCode.Old_Turkic;
            case KAITHI:
                return ScriptCode.Kaithi;
            case BATAK:
                return ScriptCode.Batak;
            case BRAHMI:
                return ScriptCode.Brahmi;
            case MANDAIC:
                return ScriptCode.Mandaic;
            case MEROITIC_CURSIVE:
                return ScriptCode.Meroitic_Cursive;
            case MEROITIC_HIEROGLYPHS:
                return ScriptCode.Meroitic_Hieroglyphs;
            case CHAKMA:
                return ScriptCode.Chakma;
            case SHARADA:
                return ScriptCode.Sharada;
            case SORA_SOMPENG:
                return ScriptCode.Sora_Sompeng;
            case MIAO:
                return ScriptCode.Miao;
            case TAKRI:
                return ScriptCode.Takri;
            case UNKNOWN:
            default:
        }
        return ScriptCode.Common;
    }

    public int toInt() {
        return code;
    }
}
