package edu.illinois.i3.emop.apps.statsbuilder.txt;


import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import edu.illinois.i3.emop.apps.statsbuilder.OCRPage;
import edu.illinois.i3.emop.apps.statsbuilder.OCRPageStats;
import edu.illinois.i3.emop.apps.statsbuilder.OCRToken;
import edu.illinois.i3.emop.apps.statsbuilder.exceptions.PageParserException;
import edu.illinois.i3.spellcheck.engine.SpellDictionary;
import opennlp.tools.tokenize.Tokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TxtPage extends OCRPage<OCRPageStats> {

    private static final Logger log = LoggerFactory.getLogger(TxtPage.class);

    private final Integer _pageNumber;
    private final List<TxtToken> _tokens;

    private TxtPage(Integer pageNumber, String[] tokens) {
        _pageNumber = pageNumber;

        _tokens = Lists.newArrayListWithExpectedSize(tokens.length);
        for (String token : tokens)
            _tokens.add(new TxtToken(token));
    }

    public static TxtPage parse(InputStream pageStream, Integer pageNumber, Tokenizer tokenizer) throws PageParserException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(pageStream, Charsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                sb.append(line).append('\n');

            String[] tokens = tokenizer.tokenize(sb.toString());
            return new TxtPage(pageNumber, tokens);
        }
        catch (IOException e) {
            log.error("Txt parser error", e);
            throw new PageParserException(e);
        }
    }

    public static TxtPage parse(File pageFile, Tokenizer tokenizer) throws PageParserException {
        try {
            return parse(new FileInputStream(pageFile), getPageNumber(pageFile), tokenizer);
        }
        catch (FileNotFoundException e) {
            throw new PageParserException(e);
        }
    }

    private static Integer getPageNumber(File pageFile) {
        Integer pageNumber = null;

        String fileName = pageFile.getName();
        Pattern pattern = Pattern.compile("^\\p{N}+");
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.find())
            pageNumber = Integer.parseInt(matcher.group());

        return pageNumber;
    }

    @Override
    public Iterator<? extends OCRToken> getTokenIterator() {
        return _tokens.iterator();
    }

    @Override
    public Integer getPageNumber() {
        return _pageNumber;
    }

    @Override
    protected OCRPageStats buildOCRPageStatsBean(SpellDictionary[] dictionaries, Map<String, String> replacementRules) {
        return new OCRPageStats();
    }
}
