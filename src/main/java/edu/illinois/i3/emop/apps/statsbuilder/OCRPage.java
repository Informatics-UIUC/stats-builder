package edu.illinois.i3.emop.apps.statsbuilder;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.illinois.i3.emop.apps.statsbuilder.exceptions.PageParserException;
import edu.illinois.i3.emop.apps.statsbuilder.stats.Bin;
import edu.illinois.i3.spellcheck.engine.SpellDictionary;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class OCRPage<T extends OCRPageStats> {

    public static final char[] CHARS = new char[] {
      'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
      '`','~','!','@','#','$','%','^','&','*','(',')','-','_','=','+','[',']','{','}','\\','|',';',':','\'','"',',','<','.','>','/','?'
    };

    protected static final int MAX_LEADING_PUNCT_TO_REMOVE = 1;
    protected static final int MAX_TRAILING_PUNCT_TO_REMOVE = 3;
    protected static final int CLEAN_TOKEN_LEN_THRESHOLD = 3;

    protected final Pattern NonAlphaPattern = Pattern.compile("\\P{L}", Pattern.CANON_EQ);
    protected final Pattern PunctPattern = Pattern.compile("^\\p{Punct}$");
    protected final Pattern NumberBasedObjectPattern = Pattern.compile("^\\p{Sc}?[\\.,/\\-]?(\\p{N}+[\\.,/%\\-]?)+\\p{Sc}?$");
    protected final Pattern OneAlphaPattern = Pattern.compile("^\\p{L}$", Pattern.CANON_EQ);
    protected final Pattern Repeated3orMoreCharsPattern = Pattern.compile("(\\P{N})\\1{2,}", Pattern.CANON_EQ);
    protected final Pattern Repeated4orMoreCharsPattern = Pattern.compile("(\\P{N})\\1{3,}", Pattern.CANON_EQ);

    protected Bin<Integer>[] _wordLengthBins;

    public abstract Iterator<? extends OCRToken> getTokenIterator();

    public abstract Integer getPageNumber();

    public void setWordLengthBins(Bin<Integer>[] bins) {
        _wordLengthBins = bins;
    }

    protected abstract T buildOCRPageStatsBean(SpellDictionary[] dictionaries, Map<String, String> replacementRules);

    public T calculateStatistics(SpellDictionary[] dictionaries, Map<String,String> replacementRules) throws PageParserException {
        int tokenCount = 0;
        int correctTokenCount = 0;
        int incorrectTokenCount = 0;
        int uniqueTokenCount = 0;
        int uniqueCorrectTokenCount = 0;
        int uniqueIncorrectTokenCount = 0;
        int oneNonAlphaTokenCount = 0;
        int twoNonAlphaTokenCount = 0;
        int threeOrMoreNonAlphaTokenCount = 0;
        int allNonAlphaTokenCount = 0;
        int allAlphaTokenCount = 0;
        int oneNumAlphaTokenCount = 0;
        int twoNumAlphaTokenCount = 0;
        int threeNumAlphaTokenCount = 0;
        int ltHalfNumAlphaTokenCount = 0;
        int ge3RepeatedCharsTokenCount = 0;
        int ge4RepeatedCharsTokenCount = 0;
        int applicableReplacementRulesCount = 0;

        int numberObjectsTokenCount = 0;
        int punctTokenCount = 0;
        int lenGt1NonAlphaTokenCount = 0;
        int cleanOneNonAlphaNoRepTokenCount = 0;
        int cleanTwoNonAlphaNoRepTokenCount = 0;
        int cleanThreeOrMoreNonAlphaTokenCount = 0;
        int cleanAllAlphaNoRepTokenCount = 0;
        int cleanShortWordCount = 0;
        int singleLetterCount = 0;

        Map<Character, Integer> charCountsCorrectable = Maps.newLinkedHashMap();
        Map<Character, Integer> charCounts = Maps.newLinkedHashMap();

        for (int i = 0, iMax = CHARS.length; i < iMax; i++) {
            charCountsCorrectable.put(CHARS[i], 0);
            charCounts.put(CHARS[i], 0);
        }

        Map<Bin<Integer>, Integer> correctableTokenLengths = Maps.newLinkedHashMap();
        Map<Bin<Integer>, Integer> binTokenLengths = Maps.newLinkedHashMap();

        for (Bin<Integer> bin : _wordLengthBins) {
            correctableTokenLengths.put(bin, 0);
            binTokenLengths.put(bin, 0);
        }

        Map<Integer, Integer> tokenLengths = Maps.newHashMap();
        Map<String, Integer> dictionaryMatches = Maps.newHashMap();
        Map<String, Integer> misspellingCounts = Maps.newHashMap();

        Set<String> uniqueTokens = Sets.newHashSet();
        Set<String> uniqueCorrectTokens = Sets.newHashSet();

        for (SpellDictionary dictionary : dictionaries)
            dictionaryMatches.put(dictionary.getName(), 0);

        Iterator<? extends OCRToken> tokenIterator = getTokenIterator();
        while (tokenIterator.hasNext()) {
            OCRToken token = tokenIterator.next();
            String tokenText = token.getText().trim();

            // join end of line hyphenated words
            if (token.isLastTokenOnLine() && tokenText.endsWith("-") && tokenIterator.hasNext()) {
                String nextTokenText = tokenIterator.next().getText().trim();
                tokenText = tokenText.substring(0, tokenText.length() - 1) + nextTokenText;
            }

            if (tokenText.isEmpty())
                continue;

            String normTokenText = tokenText.toLowerCase();
            String cleanTokenText = cleanToken(normTokenText);

            // tokenText      = the default, not-normalized, token (trimmed)
            // normTokenText  = the normalized (lowercased) tokenText
            // cleanTokenText = the normTokenText with MAX_LEADING_PUNCT_REMOVE punctuation removed, and MAX_TRAILING_PUNCT_REMOVE punctuation removed
            //                  (can be 'null' if, after cleaning, the remaining substring has a length < CLEAN_TOKEN_LEN_THRESHOLD)

            Integer tokenLength = tokenText.length();
            Integer cleanTokenLength =  (cleanTokenText != null) ? cleanTokenText.length() : null;

            tokenCount++;
            uniqueTokens.add(normTokenText);

            if (replacementRules.containsKey(tokenText) || (cleanTokenText != null && replacementRules.containsKey(cleanTokenText)))
                applicableReplacementRulesCount++;

            // update token length distribution for raw tokens
            Integer tokenLengthCount = tokenLengths.get(tokenLength);
            if (tokenLengthCount == null)
                tokenLengthCount = 0;
            tokenLengths.put(tokenLength, tokenLengthCount + 1);

            // update token length bins for raw tokens
            for (Bin<Integer> bin : _wordLengthBins) {
                Integer min = bin.getMin();
                Integer max = bin.getMax();

                if ((min == null || tokenLength > min) && (max == null || tokenLength <= max)) {
                    binTokenLengths.put(bin, binTokenLengths.get(bin) + 1);
                    break;
                }
            }

            Matcher punctMatcher = PunctPattern.matcher(tokenText);
            Matcher numberMatcher = NumberBasedObjectPattern.matcher(tokenText);
            Matcher singleAlphaMatcher = OneAlphaPattern.matcher(tokenText);

            // compute the number of non-alpha characters in the raw token
            {
                Matcher nonAlphaMatcher = NonAlphaPattern.matcher(tokenText);
                int nonAlphaCount = 0;
                while (nonAlphaMatcher.find())
                    nonAlphaCount++;

                if (nonAlphaCount == 0)
                    allAlphaTokenCount++;

                else

                if (nonAlphaCount == 1 && tokenLength > nonAlphaCount)
                    oneNonAlphaTokenCount++;

                else

                if (nonAlphaCount == 2 && tokenLength > nonAlphaCount)
                    twoNonAlphaTokenCount++;

                else

                if (nonAlphaCount > 2 && tokenLength > nonAlphaCount)
                    threeOrMoreNonAlphaTokenCount++;

                else

                if (nonAlphaCount == tokenLength) {
                    allNonAlphaTokenCount++;

                    if (tokenLength > 1 && !numberMatcher.matches())
                        lenGt1NonAlphaTokenCount++;
                }

                // update character counts for token (for alpha and punct characters, case insensitive)
                for (char c : normTokenText.toCharArray())
                    if (charCounts.containsKey(c))
                        charCounts.put(c, charCounts.get(c) + 1);
            }

            if (punctMatcher.matches()) {
                punctTokenCount++;
                continue;
            }

            if (numberMatcher.matches()) {
                numberObjectsTokenCount++;
                continue;
            }

            if (singleAlphaMatcher.matches()) {
                singleLetterCount++;
                continue;
            }


            // check whether the token contains more than 2 repeated characters in a run
            Matcher ge3RepeatedCharsMatcher = Repeated3orMoreCharsPattern.matcher(normTokenText);
            if (ge3RepeatedCharsMatcher.find())
                ge3RepeatedCharsTokenCount++;

            boolean rep = false;
            Matcher ge4RepeatedCharsMatcher = Repeated4orMoreCharsPattern.matcher(normTokenText);
            if (ge4RepeatedCharsMatcher.find()) {
                ge4RepeatedCharsTokenCount++;
                rep = true;
            }

            // compute the number of non-alpha characters in the cleaned token (if it contains no more than 3 repeated characters in a run)
            if (cleanTokenText != null) {
                if (!rep) {
                    boolean matchesCorrectableProfile = false;
                    Matcher nonAlphaMatcher = NonAlphaPattern.matcher(cleanTokenText);
                    int nonAlphaCount = 0;
                    while (nonAlphaMatcher.find())
                        nonAlphaCount++;

                    if (nonAlphaCount == 0) {
                        cleanAllAlphaNoRepTokenCount++;
                        matchesCorrectableProfile = true;
                    }

                    else

                    if (nonAlphaCount == 1 && cleanTokenLength > nonAlphaCount) {
                        cleanOneNonAlphaNoRepTokenCount++;
                        matchesCorrectableProfile = true;
                    }

                    else

                    if (nonAlphaCount == 2 && cleanTokenLength > nonAlphaCount) {
                        cleanTwoNonAlphaNoRepTokenCount++;
                        matchesCorrectableProfile = true;
                    }

                    else

                    if (nonAlphaCount > 2 && cleanTokenLength > nonAlphaCount)
                        cleanThreeOrMoreNonAlphaTokenCount++;

                    if (matchesCorrectableProfile) {
                        // update character counts for cleaned token (for alpha and punct characters, case insensitive)
                        for (char c : cleanTokenText.toCharArray())
                            if (charCountsCorrectable.containsKey(c))
                                charCountsCorrectable.put(c, charCountsCorrectable.get(c) + 1);

                        // update token length bins for clean tokens matching the correctable profile
                        for (Bin<Integer> bin : _wordLengthBins) {
                            Integer min = bin.getMin();
                            Integer max = bin.getMax();

                            if ((min == null || cleanTokenLength > min) && (max == null || cleanTokenLength <= max)) {
                                correctableTokenLengths.put(bin, correctableTokenLengths.get(bin) + 1);
                                break;
                            }
                        }

                    }
                }
            } else
                cleanShortWordCount++;

            // if normTokenText contains at least 1 alpha, then figure out how many digits are also included
            if (normTokenText.replaceFirst("\\p{L}", "").length() < tokenLength) {
                int numDigitsInToken = String.format("x%sx", normTokenText).split("\\p{N}").length - 1;
                switch (numDigitsInToken) {
                    case 1:
                        oneNumAlphaTokenCount++;
                        break;

                    case 2:
                        twoNumAlphaTokenCount++;
                        break;

                    case 3:
                        threeNumAlphaTokenCount++;
                        break;
                }

                if (numDigitsInToken > 0 && numDigitsInToken < normTokenText.length() / 2)
                    ltHalfNumAlphaTokenCount++;
            }

            boolean isCorrectWord = false;

            if (cleanTokenText == null)
                // use the original normalized token
                cleanTokenText = normTokenText;

            // check if token in dictionary
            for (SpellDictionary dictionary : dictionaries) {
                boolean isCorrect = dictionary.isCorrect(cleanTokenText);
                isCorrectWord |= isCorrect;

                if (isCorrect) {
                    // update the dictionary match count for this dictionary
                    Integer dictMatchCount = dictionaryMatches.get(dictionary.getName());
                    dictionaryMatches.put(dictionary.getName(), dictMatchCount + 1);
                }
            }

            if (isCorrectWord) {
                correctTokenCount++;
                uniqueCorrectTokens.add(cleanTokenText);
            } else {
                incorrectTokenCount++;

                // update the misspelling count for this misspelling
                Integer misspellingCount = misspellingCounts.get(cleanTokenText);
                if (misspellingCount == null)
                    misspellingCount = 0;
                misspellingCounts.put(cleanTokenText, misspellingCount + 1);
            }
        }

        uniqueTokenCount = uniqueTokens.size();
        uniqueCorrectTokenCount = uniqueCorrectTokens.size();
        uniqueIncorrectTokenCount = misspellingCounts.size();

        T pageStats = buildOCRPageStatsBean(dictionaries, replacementRules);
        pageStats.set1numAlphaTokenCount(oneNumAlphaTokenCount);
        pageStats.set2numAlphaTokenCount(twoNumAlphaTokenCount);
        pageStats.set3numAlphaTokenCount(threeNumAlphaTokenCount);
        pageStats.setAllAlphaTokenCount(allAlphaTokenCount);
        pageStats.setAllNonAlphaTokenCount(allNonAlphaTokenCount);
        pageStats.setApplicableReplacementRulesCount(applicableReplacementRulesCount);
        pageStats.setCleanAllAlphaNoRepTokenCount(cleanAllAlphaNoRepTokenCount);
        pageStats.setCleanOneNonAlphaNoRepTokenCount(cleanOneNonAlphaNoRepTokenCount);
        pageStats.setCleanShortWordCount(cleanShortWordCount);
        pageStats.setCleanThreeOrMoreNonAlphaTokenCount(cleanThreeOrMoreNonAlphaTokenCount);
        pageStats.setCleanTwoNonAlphaNoRepTokenCount(cleanTwoNonAlphaNoRepTokenCount);
        pageStats.setCorrectTokenCount(correctTokenCount);
        pageStats.setGe3RepeatedCharsTokenCount(ge3RepeatedCharsTokenCount);
        pageStats.setGe4RepeatedCharsTokenCount(ge4RepeatedCharsTokenCount);
        pageStats.setIncorrectTokenCount(incorrectTokenCount);
        pageStats.setLenGt1NonAlphaTokenCount(lenGt1NonAlphaTokenCount);
        pageStats.setLtHalfNumAlphaTokenCount(ltHalfNumAlphaTokenCount);
        pageStats.setNumberObjectsTokenCount(numberObjectsTokenCount);
        pageStats.setOneNonAlphaTokenCount(oneNonAlphaTokenCount);
        pageStats.setPageNumber(getPageNumber());
        pageStats.setPunctTokenCount(punctTokenCount);
        pageStats.setSingleLetterCount(singleLetterCount);
        pageStats.setThreeOrMoreNonAlphaTokenCount(threeOrMoreNonAlphaTokenCount);
        pageStats.setTokenCount(tokenCount);
        pageStats.setTwoNonAlphaTokenCount(twoNonAlphaTokenCount);
        pageStats.setUniqueCorrectTokenCount(uniqueCorrectTokenCount);
        pageStats.setUniqueIncorrectTokenCount(uniqueIncorrectTokenCount);
        pageStats.setUniqueTokenCount(uniqueTokenCount);

        pageStats.setBinTokenLengths(binTokenLengths);
        pageStats.setCharCounts(charCounts);
        pageStats.setCharCountsCorrectable(charCountsCorrectable);
        pageStats.setCorrectableTokenLengths(correctableTokenLengths);
        pageStats.setDictionaryMatches(dictionaryMatches);
        pageStats.setMisspellingCounts(misspellingCounts);
        pageStats.setTokenLengths(tokenLengths);

        return pageStats;
    }

    protected String cleanToken(String token) {
        String cleanToken = token.replaceFirst("^\\p{Punct}{0," + MAX_LEADING_PUNCT_TO_REMOVE + "}", "")
                .replaceFirst("\\p{Punct}{0," + MAX_TRAILING_PUNCT_TO_REMOVE + "}$", "");

        // a token can be cleaned only if, after cleaning, the remaining substring has a length >= 3
        if (cleanToken.length() < CLEAN_TOKEN_LEN_THRESHOLD)
            cleanToken = null;

        return cleanToken;
    }

    @Override
    public String toString() {
        return "Page " + getPageNumber();
    }
}
