package edu.illinois.i3.emop.apps.statsbuilder;


import com.google.common.collect.Lists;
import edu.illinois.i3.emop.apps.statsbuilder.stats.Bin;
import edu.illinois.i3.spellcheck.engine.SpellDictionary;

import java.util.List;
import java.util.Map;

public class ProcessingOptions {
    private SpellDictionary[] _dictionaries;
    private Map<String, String> _replacementRules;
    private Bin[] _wordLengthBins;

    public SpellDictionary[] getDictionaries() {
        return _dictionaries;
    }

    public void setDictionaries(SpellDictionary[] dictionaries) {
        _dictionaries = dictionaries;
    }

    public Map<String, String> getReplacementRules() {
        return _replacementRules;
    }

    public void setReplacementRules(Map<String, String> replacementRules) {
        _replacementRules = replacementRules;
    }

    public Bin[] getWordLengthBins() {
        return _wordLengthBins;
    }

    public void setWordLengthBins(Bin[] wordLengthBins) {
        _wordLengthBins = wordLengthBins;
    }

    private List<String> getDefaultColumns() {
        List<String> columns = Lists.newArrayList("page", "quality", "score", "tokens", "ignored", "numberObjects", "punct", "singleLetter",
                "correct", "correctP", "misspelled", "misspelledP", "cleanOneNonAlphaNoRep", "cleanTwoNonAlphaNoRep", "cleanAllAlphaNoRep",
                "lenGt1NonAlpha", "cleanThreeOrMoreNonAlpha", "cleanShortWord", "ge3RepChars", "ge4RepChars",
                "unique", "uniqueCorrect", "uniqueCorrectP", "uniqueMisspelled", "uniqueMisspelledP",
                "oneNonAlpha", "twoNonAlpha", "threeOrMoreNonAlpha", "allNonAlpha",
                "allAlpha", "1nAlpha", "2nAlpha", "3nAlpha", "ltHalfNAlpha", "applicableReplacements");

        for (Bin bin : _wordLengthBins)
            columns.add(bin.getName());

        for (Character c : OCRPage.CHARS)
            columns.add(c.toString());

        for (Bin bin : _wordLengthBins)
            columns.add("C_" + bin.getName());

        for (Character c : OCRPage.CHARS)
            columns.add("C_" + c.toString());

        for (SpellDictionary dictionary : _dictionaries)
            columns.add(dictionary.getName());

        return columns;
    }

    public List<String> getCsvColumns(Main.DocumentFormat format) {
        List<String> columns = getDefaultColumns();

        switch (format) {
            case HOCR:
                columns.addAll(1, Lists.newArrayList("paragraphs", "lines"));
                break;

            case TXT:
                // No extra columns
                break;

            default:
                throw new RuntimeException("Don't know how to handle format: " + format);
        }

        return columns;
    }
}
