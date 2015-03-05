package edu.illinois.i3.emop.apps.statsbuilder.hocr;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.illinois.i3.emop.apps.statsbuilder.OCRPage;
import edu.illinois.i3.emop.apps.statsbuilder.exceptions.PageParserException;
import edu.illinois.i3.spellcheck.engine.SpellDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author capitanu
 */
public class HOCRPage extends OCRPage<HOCRPageStats> {

    private static final Logger log = LoggerFactory.getLogger(HOCRPage.class);

    // Page metadata
    private final String _pageId;
    private final Integer _pageNumber;
    private final String _ocrEngine;
    private final Set<String> _ocrCapabilities;
    private final Element _pageXml;
    private final XPathExpression _xpathOCRPara;
    private final XPathExpression _xpathOCRLine;
    private final XPathExpression _xpathOCRXWord;

    // Additional page stats
    private List<Integer> _linesPerParagraph;
    private List<Integer> _tokensPerLine;


    private HOCRPage(String pageId, Integer pageNumber, Element pageXml, String ocrEngine, Set<String> ocrCapabilities) {
        _pageId = pageId;
        _pageNumber = pageNumber;
        _pageXml = pageXml;
        _ocrEngine = ocrEngine;
        _ocrCapabilities = ocrCapabilities;

        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();

        try {
            _xpathOCRPara = xpath.compile("descendant::*[@class='ocr_par']");
            _xpathOCRLine = xpath.compile("descendant::*[@class='ocr_line']");
            _xpathOCRXWord = xpath.compile("descendant::*[@class='ocrx_word']");
        }
        catch (XPathExpressionException e) {
            throw new RuntimeException(e);
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

    public static HOCRPage parse(InputStream pageStream, Integer pageNumber) throws PageParserException {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(false);
            documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            Document document = documentBuilder.parse(pageStream);

            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();

            String ocrEngine = (String) xpath.evaluate("/html/head/meta[@name='ocr-system']/@content", document, XPathConstants.STRING);
            Set<String> ocrCapabilities = Sets.newHashSet();
            String capabilities = (String) xpath.evaluate("/html/head/meta[@name='ocr-capabilities']/@content", document, XPathConstants.STRING);
            ocrCapabilities.addAll(Arrays.asList(capabilities.split(" ")));

            Element pageXml = (Element) xpath.evaluate("//*[@class='ocr_page']", document, XPathConstants.NODE);
            String pageId = pageXml.getAttribute("id");

            return new HOCRPage(pageId, pageNumber, pageXml, ocrEngine, ocrCapabilities);
        }
        catch (Exception e) {
            log.error("HOCR parser error", e);
            throw new PageParserException(e);
        }
    }

    public static HOCRPage parse(File pageFile) throws PageParserException {
        try {
            return parse(new FileInputStream(pageFile), getPageNumber(pageFile));
        }
        catch (FileNotFoundException e) {
            throw new PageParserException(e);
        }
    }

    public String getPageId() {
        return _pageId;
    }

    public String getOcrEngine() {
        return _ocrEngine;
    }

    public Set<String> getOcrCapabilities() {
        return _ocrCapabilities;
    }

    public List<Integer> getLinesPerParagraph() {
        return _linesPerParagraph;
    }

    public List<Integer> getTokensPerLine() {
        return _tokensPerLine;
    }

    @Override
    public Iterator<HOCRToken> getTokenIterator() {
        return new HOCRTokenIterator(_pageXml);
    }

    @Override
    public Integer getPageNumber() {
        return _pageNumber;
    }

    @Override
    protected HOCRPageStats buildOCRPageStatsBean(SpellDictionary[] dictionaries, Map<String, String> replacementRules) {
        return new HOCRPageStats();
    }

    @Override
    public HOCRPageStats calculateStatistics(SpellDictionary[] dictionaries, Map<String,String> replacementRules) throws PageParserException {
        int paragraphCount;
        int linesCount;

        try {
            NodeList paragraphs = (NodeList) _xpathOCRPara.evaluate(_pageXml, XPathConstants.NODESET);
            paragraphCount = paragraphs.getLength();
            _linesPerParagraph = Lists.newArrayListWithExpectedSize(paragraphCount);
            _tokensPerLine = Lists.newArrayList();

            for (int i = 0; i < paragraphCount; i++) {
                Element xmlParagraph = (Element) paragraphs.item(i);
                NodeList lines = (NodeList) _xpathOCRLine.evaluate(xmlParagraph, XPathConstants.NODESET);
                int lineCount = lines.getLength();
                _linesPerParagraph.add(lineCount);

                for (int j = 0; j < lineCount; j++) {
                    Element xmlLine = (Element) lines.item(j);
                    NodeList tokens = (NodeList) _xpathOCRXWord.evaluate(xmlLine, XPathConstants.NODESET);
                    int tokenCount = tokens.getLength();
                    _tokensPerLine.add(tokenCount);
                }
            }

            linesCount = _tokensPerLine.size();

            HOCRPageStats pageStats = super.calculateStatistics(dictionaries, replacementRules);
            pageStats.setLinesCount(linesCount);
            pageStats.setParagraphCount(paragraphCount);

            return pageStats;
        }
        catch (XPathExpressionException e) {
            throw new PageParserException(e);
        }
    }
}
