package edu.illinois.i3.emop.apps.statsbuilder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.illinois.i3.emop.apps.statsbuilder.stats.Bin;

import java.util.List;
import java.util.Map;

public class OCRPageStats {

    ///////////////////////////////////////////
    // Page statistics
    ///////////////////////////////////////////

    // Note: correctable profile = tokens which, after cleaning, contain at most 2 non-alpha characters and at least 1 alpha character,
    //       have a length of at least 3, and do not contain 4 or more repeated characters in a run

    // Note: cleaning = removal of max MAX_LEADING_PUNCT_TO_REMOVE + MAX_TRAILING_PUNCT_TO_REMOVE total punctuation characters from the beginning and end of a token
    //       a token can be cleaned only if, after cleaning, the remaining substring has a length >= CLEAN_TOKEN_LEN_THRESHOLD

    // Note: spell checking: do not spellcheck (i.e. count as correct or incorrect) tokens made up of exactly 1 alpha char

    // TODO: use PROPORTIONS not COUNTS as features; also deal with NaN situations

    private int _pageNumber;                        // page number

                                                    // number of...
    private int _tokenCount;                        // tokens on page
    private int _correctTokenCount;                 // correct (found in dictionary) tokens (after cleaning)
    private int _incorrectTokenCount;               // incorrect (not found in dictionary but have at least one alpha character) tokens (after cleaning)
    private int _uniqueTokenCount;                  // unique tokens
    private int _uniqueCorrectTokenCount;           // unique correct tokens (after cleaning)
    private int _uniqueIncorrectTokenCount;         // unique incorrect tokens (after cleaning)
    private int _oneNonAlphaTokenCount;             // tokens containing exactly 1 non-alpha character and at least 1 alpha
    private int _twoNonAlphaTokenCount;             // tokens containing exactly 2 non-alpha characters and at least 1 alpha
    private int _threeOrMoreNonAlphaTokenCount;     // tokens containing > 2 non-alpha characters and at least 1 alpha
    private int _allNonAlphaTokenCount;             // tokens that do not contain alpha characters
    private int _allAlphaTokenCount;                // tokens that contain exclusively alpha characters
    private int _1numAlphaTokenCount;               // tokens that contain exactly 1 number and at least 1 alpha
    private int _2numAlphaTokenCount;               // tokens that contain exactly 2 numbers and at least 1 alpha
    private int _3numAlphaTokenCount;               // tokens that contain exactly 3 numbers and at least 1 alpha
    private int _ltHalfNumAlphaTokenCount;          // tokens for which less than half of the characters are numbers (but contain at least 1 number)
    private int _ge3RepeatedCharsTokenCount;        // tokens containing 3 or more repeated characters (not numbers) in a run
    private int _ge4RepeatedCharsTokenCount;        // tokens containing 4 or more repeated characters (not numbers) in a run
    private int _applicableReplacementRulesCount;   // replacement rules that match tokens on the page

    private int _numberObjectsTokenCount;           // tokens that could represent numbers, dates, amounts of money, identifiers..etc. (are number based)
    private int _punctTokenCount;                   // tokens that are made up of exactly 1 punctuation character (non-alphanum)
    private int _lenGt1NonAlphaTokenCount;          // tokens of length > 1 that contain exclusively non-alpha characters (but are not made up entirely of numbers) (can be thought of as "garbage" tokens)
    private int _cleanOneNonAlphaNoRepTokenCount;   // tokens which, after cleaning, are of length at least 3, contain exactly 1 non-alpha character and at least 1 alpha, and no 4 or more repeated characters in a run
    private int _cleanTwoNonAlphaNoRepTokenCount;   // tokens which, after cleaning, are of length at least 3, contain exactly 2 non-alpha character and at least 1 alpha, and no 4 or more repeated characters in a run
    private int _cleanThreeOrMoreNonAlphaTokenCount;// tokens which, after cleaning, are of length at least 3, contain > 2 non-alpha character and at least 1 alpha (for correction purposes they can also be thought of as "garbage")
    private int _cleanAllAlphaNoRepTokenCount;      // tokens which, after cleaning, contain exclusively alpha characters and no 4 or more repeated characters in a run
    private int _cleanShortWordCount;               // tokens which, after cleaning, have length < 3 and are supposed to be words (i.e. no numbers, no single punctuation, no single letters)
    private int _singleLetterCount;                 // tokens made up of exactly 1 alpha character

    protected Map<Character, Integer> _charCountsCorrectable;       // character counts for tokens matching the "correctable profile" (for alpha and punct characters, case insensitive)
    protected Map<Character, Integer> _charCounts;                  // character counts for entire page (for alpha and punct characters, case insensitive)
    protected Map<String, Integer> _misspellingCounts;              // count of occurrences for each misspelled word
    protected Map<Integer, Integer> _tokenLengths;                  // distribution of token lengths for the raw tokens
    protected Map<String, Integer> _dictionaryMatches;              // number of dictionary matches for each dictionary
    protected Map<Bin<Integer>, Integer> _binTokenLengths;          // binned token lengths for the raw tokens
    protected Map<Bin<Integer>, Integer> _correctableTokenLengths;  // binned token lengths for the cleaned tokens that match the "correctable profile"


    public Map<String, Integer> getMisspellingCounts() {
        return _misspellingCounts;
    }

    public Map<Integer, Integer> getTokenLengths() {
        return _tokenLengths;
    }

    public Map<String, Integer> getDictionaryMatches() {
        return _dictionaryMatches;
    }

    public Map<Bin<Integer>, Integer> getBinTokenLengths() {
        return _binTokenLengths;
    }

    public void setMisspellingCounts(Map<String, Integer> misspellingCounts) {
        _misspellingCounts = misspellingCounts;
    }

    public void setTokenLengths(Map<Integer, Integer> tokenLengths) {
        _tokenLengths = tokenLengths;
    }

    public void setDictionaryMatches(Map<String, Integer> dictionaryMatches) {
        _dictionaryMatches = dictionaryMatches;
    }

    public void setBinTokenLengths(Map<Bin<Integer>, Integer> binTokenLengths) {
        _binTokenLengths = binTokenLengths;
    }

    public int getPageNumber() {
        return _pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        _pageNumber = pageNumber;
    }

    public int getTokenCount() {
        return _tokenCount;
    }

    public void setTokenCount(int tokenCount) {
        _tokenCount = tokenCount;
    }

    public int getCorrectTokenCount() {
        return _correctTokenCount;
    }

    public void setCorrectTokenCount(int correctTokenCount) {
        _correctTokenCount = correctTokenCount;
    }

    public int getIncorrectTokenCount() {
        return _incorrectTokenCount;
    }

    public void setIncorrectTokenCount(int incorrectTokenCount) {
        _incorrectTokenCount = incorrectTokenCount;
    }

    public int getIgnoredTokenCount() {
        return _numberObjectsTokenCount + _punctTokenCount + _singleLetterCount;
    }

    public int getUniqueTokenCount() {
        return _uniqueTokenCount;
    }

    public void setUniqueTokenCount(int uniqueTokenCount) {
        _uniqueTokenCount = uniqueTokenCount;
    }

    public int getUniqueCorrectTokenCount() {
        return _uniqueCorrectTokenCount;
    }

    public void setUniqueCorrectTokenCount(int uniqueCorrectTokenCount) {
        _uniqueCorrectTokenCount = uniqueCorrectTokenCount;
    }

    public int getUniqueIncorrectTokenCount() {
        return _uniqueIncorrectTokenCount;
    }

    public void setUniqueIncorrectTokenCount(int uniqueIncorrectTokenCount) {
        _uniqueIncorrectTokenCount = uniqueIncorrectTokenCount;
    }

    public int getOneNonAlphaTokenCount() {
        return _oneNonAlphaTokenCount;
    }

    public void setOneNonAlphaTokenCount(int oneNonAlphaTokenCount) {
        _oneNonAlphaTokenCount = oneNonAlphaTokenCount;
    }

    public int getTwoNonAlphaTokenCount() {
        return _twoNonAlphaTokenCount;
    }

    public void setTwoNonAlphaTokenCount(int twoNonAlphaTokenCount) {
        _twoNonAlphaTokenCount = twoNonAlphaTokenCount;
    }

    public int getThreeOrMoreNonAlphaTokenCount() {
        return _threeOrMoreNonAlphaTokenCount;
    }

    public void setThreeOrMoreNonAlphaTokenCount(int threeOrMoreNonAlphaTokenCount) {
        _threeOrMoreNonAlphaTokenCount = threeOrMoreNonAlphaTokenCount;
    }

    public int getAllNonAlphaTokenCount() {
        return _allNonAlphaTokenCount;
    }

    public void setAllNonAlphaTokenCount(int allNonAlphaTokenCount) {
        _allNonAlphaTokenCount = allNonAlphaTokenCount;
    }

    public int getAllAlphaTokenCount() {
        return _allAlphaTokenCount;
    }

    public void setAllAlphaTokenCount(int allAlphaTokenCount) {
        _allAlphaTokenCount = allAlphaTokenCount;
    }

    public int get1numAlphaTokenCount() {
        return _1numAlphaTokenCount;
    }

    public void set1numAlphaTokenCount(int a1numAlphaTokenCount) {
        _1numAlphaTokenCount = a1numAlphaTokenCount;
    }

    public int get2numAlphaTokenCount() {
        return _2numAlphaTokenCount;
    }

    public void set2numAlphaTokenCount(int a2numAlphaTokenCount) {
        _2numAlphaTokenCount = a2numAlphaTokenCount;
    }

    public int get3numAlphaTokenCount() {
        return _3numAlphaTokenCount;
    }

    public void set3numAlphaTokenCount(int a3numAlphaTokenCount) {
        _3numAlphaTokenCount = a3numAlphaTokenCount;
    }

    public int getLtHalfNumAlphaTokenCount() {
        return _ltHalfNumAlphaTokenCount;
    }

    public void setLtHalfNumAlphaTokenCount(int ltHalfNumAlphaTokenCount) {
        _ltHalfNumAlphaTokenCount = ltHalfNumAlphaTokenCount;
    }

    public int getGe3RepeatedCharsTokenCount() {
        return _ge3RepeatedCharsTokenCount;
    }

    public void setGe3RepeatedCharsTokenCount(int ge3RepeatedCharsTokenCount) {
        _ge3RepeatedCharsTokenCount = ge3RepeatedCharsTokenCount;
    }

    public int getGe4RepeatedCharsTokenCount() {
        return _ge4RepeatedCharsTokenCount;
    }

    public void setGe4RepeatedCharsTokenCount(int ge4RepeatedCharsTokenCount) {
        _ge4RepeatedCharsTokenCount = ge4RepeatedCharsTokenCount;
    }

    public int getApplicableReplacementRulesCount() {
        return _applicableReplacementRulesCount;
    }

    public void setApplicableReplacementRulesCount(int applicableReplacementRulesCount) {
        _applicableReplacementRulesCount = applicableReplacementRulesCount;
    }

    public int getNumberObjectsTokenCount() {
        return _numberObjectsTokenCount;
    }

    public void setNumberObjectsTokenCount(int numbersTokenCount) {
        _numberObjectsTokenCount = numbersTokenCount;
    }

    public int getPunctTokenCount() {
        return _punctTokenCount;
    }

    public void setPunctTokenCount(int punctTokenCount) {
        _punctTokenCount = punctTokenCount;
    }

    public int getLenGt1NonAlphaTokenCount() {
        return _lenGt1NonAlphaTokenCount;
    }

    public void setLenGt1NonAlphaTokenCount(int lenGt1NonAlphaTokenCount) {
        _lenGt1NonAlphaTokenCount = lenGt1NonAlphaTokenCount;
    }

    public int getCleanOneNonAlphaNoRepTokenCount() {
        return _cleanOneNonAlphaNoRepTokenCount;
    }

    public void setCleanOneNonAlphaNoRepTokenCount(int cleanOneNonAlphaNoRepTokenCount) {
        _cleanOneNonAlphaNoRepTokenCount = cleanOneNonAlphaNoRepTokenCount;
    }

    public int getCleanTwoNonAlphaNoRepTokenCount() {
        return _cleanTwoNonAlphaNoRepTokenCount;
    }

    public void setCleanTwoNonAlphaNoRepTokenCount(int cleanTwoNonAlphaNoRepTokenCount) {
        _cleanTwoNonAlphaNoRepTokenCount = cleanTwoNonAlphaNoRepTokenCount;
    }

    public int getCleanThreeOrMoreNonAlphaTokenCount() {
        return _cleanThreeOrMoreNonAlphaTokenCount;
    }

    public void setCleanThreeOrMoreNonAlphaTokenCount(int cleanThreeOrMoreNonAlphaTokenCount) {
        _cleanThreeOrMoreNonAlphaTokenCount = cleanThreeOrMoreNonAlphaTokenCount;
    }

    public int getCleanAllAlphaNoRepTokenCount() {
        return _cleanAllAlphaNoRepTokenCount;
    }

    public void setCleanAllAlphaNoRepTokenCount(int cleanAllAlphaNoRepTokenCount) {
        _cleanAllAlphaNoRepTokenCount = cleanAllAlphaNoRepTokenCount;
    }

    public int getCleanShortWordCount() {
        return _cleanShortWordCount;
    }

    public void setCleanShortWordCount(int cleanShortWordCount) {
        _cleanShortWordCount = cleanShortWordCount;
    }

    public int getSingleLetterCount() {
        return _singleLetterCount;
    }

    public void setSingleLetterCount(int singleLetterCount) {
        _singleLetterCount = singleLetterCount;
    }

    public Map<Character, Integer> getCharCountsCorrectable() {
        return _charCountsCorrectable;
    }

    public void setCharCountsCorrectable(Map<Character, Integer> charCountsCorrectable) {
        _charCountsCorrectable = charCountsCorrectable;
    }

    public Map<Character, Integer> getCharCounts() {
        return _charCounts;
    }

    public void setCharCounts(Map<Character, Integer> charCounts) {
        _charCounts = charCounts;
    }

    public Map<Bin<Integer>, Integer> getCorrectableTokenLengths() {
        return _correctableTokenLengths;
    }

    public void setCorrectableTokenLengths(Map<Bin<Integer>, Integer> correctableTokenLengths) {
        _correctableTokenLengths = correctableTokenLengths;
    }

    public double getPercentCorrect() {
        return (double) _correctTokenCount / (_tokenCount - getIgnoredTokenCount());
    }

    public double getPercentIncorrect() {
        return (double) _incorrectTokenCount / (_tokenCount - getIgnoredTokenCount());
    }

    public double getPercentUniqueCorrect() {
        return (double) _uniqueCorrectTokenCount / _uniqueTokenCount;
    }

    public double getPercentUniqueIncorrect() {
        return (double) _uniqueIncorrectTokenCount / _uniqueTokenCount;
    }

    public double getPageQualityScore() {
        return 1.0d - ((double) (_lenGt1NonAlphaTokenCount + _cleanThreeOrMoreNonAlphaTokenCount) / (_tokenCount - getIgnoredTokenCount()));
    }

    public double getScore() {
        return (double)
                // number of tokens matching the "correctable profile"
                (_cleanOneNonAlphaNoRepTokenCount + _cleanTwoNonAlphaNoRepTokenCount + _cleanAllAlphaNoRepTokenCount)
                // divided by
                /
                // max number of potentially correctable tokens
                (_tokenCount - getIgnoredTokenCount() - _cleanShortWordCount);
    }

    public Map<String, Object> toCsvEntry() {
        Map<String, Object> csvEntry = Maps.newLinkedHashMap();

        csvEntry.put("page", getPageNumber());
        csvEntry.put("quality", getPageQualityScore());
        csvEntry.put("score", getScore());
        csvEntry.put("tokens", getTokenCount());
        csvEntry.put("ignored", getIgnoredTokenCount());
        csvEntry.put("numberObjects", getNumberObjectsTokenCount());
        csvEntry.put("punct", getPunctTokenCount());
        csvEntry.put("singleLetter", getSingleLetterCount());
        csvEntry.put("correct", getCorrectTokenCount());
        csvEntry.put("correctP", getPercentCorrect());
        csvEntry.put("misspelled", getIncorrectTokenCount());
        csvEntry.put("misspelledP", getPercentIncorrect());
        csvEntry.put("cleanOneNonAlphaNoRep", getCleanOneNonAlphaNoRepTokenCount());
        csvEntry.put("cleanTwoNonAlphaNoRep", getCleanTwoNonAlphaNoRepTokenCount());
        csvEntry.put("cleanAllAlphaNoRep", getCleanAllAlphaNoRepTokenCount());
        csvEntry.put("lenGt1NonAlpha", getLenGt1NonAlphaTokenCount());
        csvEntry.put("cleanThreeOrMoreNonAlpha", getCleanThreeOrMoreNonAlphaTokenCount());
        csvEntry.put("cleanShortWord", getCleanShortWordCount());
        csvEntry.put("ge3RepChars", getGe3RepeatedCharsTokenCount());
        csvEntry.put("ge4RepChars", getGe4RepeatedCharsTokenCount());
        csvEntry.put("unique", getUniqueTokenCount());
        csvEntry.put("uniqueCorrect", getUniqueCorrectTokenCount());
        csvEntry.put("uniqueCorrectP", getPercentUniqueCorrect());
        csvEntry.put("uniqueMisspelled", getUniqueIncorrectTokenCount());
        csvEntry.put("uniqueMisspelledP", getPercentUniqueIncorrect());
        csvEntry.put("oneNonAlpha", getOneNonAlphaTokenCount());
        csvEntry.put("twoNonAlpha", getTwoNonAlphaTokenCount());
        csvEntry.put("threeOrMoreNonAlpha", getThreeOrMoreNonAlphaTokenCount());
        csvEntry.put("allNonAlpha", getAllNonAlphaTokenCount());
        csvEntry.put("allAlpha", getAllAlphaTokenCount());
        csvEntry.put("1nAlpha", get1numAlphaTokenCount());
        csvEntry.put("2nAlpha", get2numAlphaTokenCount());
        csvEntry.put("3nAlpha", get3numAlphaTokenCount());
        csvEntry.put("ltHalfNAlpha", getLtHalfNumAlphaTokenCount());
        csvEntry.put("applicableReplacements", getApplicableReplacementRulesCount());

        for (Map.Entry<Bin<Integer>, Integer> entry : getBinTokenLengths().entrySet())
            csvEntry.put(entry.getKey().getName(), entry.getValue());

        for (Map.Entry<Character,Integer> entry : getCharCounts().entrySet())
            csvEntry.put(entry.getKey().toString(), entry.getValue());

        for (Map.Entry<Bin<Integer>, Integer> entry : getCorrectableTokenLengths().entrySet())
            csvEntry.put("C_"+entry.getKey().getName(), entry.getValue());

        for (Map.Entry<Character,Integer> entry : getCharCountsCorrectable().entrySet())
            csvEntry.put("C_"+entry.getKey().toString(), entry.getValue());

        for (Map.Entry<String,Integer> entry : getDictionaryMatches().entrySet())
            csvEntry.put(entry.getKey(), entry.getValue());

        return csvEntry;
    }

    public List<String> getAvailableColumns() {
        return Lists.newArrayList(toCsvEntry().keySet());
    }

}
