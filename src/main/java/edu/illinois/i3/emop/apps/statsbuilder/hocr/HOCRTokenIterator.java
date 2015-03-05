package edu.illinois.i3.emop.apps.statsbuilder.hocr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author capitanu
 */
public class HOCRTokenIterator implements Iterator<HOCRToken> {

    private static final Logger log = LoggerFactory.getLogger(HOCRTokenIterator.class);

    private final NodeList _lines;
    private final int _lineCount;
    private int _currentLineIndex;
    private final XPathExpression _xpathToken;
    private NodeList _currentLineTokens;
    private int _currentLineTokenCount;
    private int _currentTokenIndex;

    public HOCRTokenIterator(Element pageXml) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            _xpathToken = xpath.compile("descendant::*[@class='ocrx_word']");
            _lines = (NodeList) xpath.evaluate("descendant::*[@class='ocr_line']", pageXml, XPathConstants.NODESET);
        }
        catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }

        _lineCount = _lines.getLength();
        _currentLineIndex = _currentTokenIndex = _currentLineTokenCount = -1;

        advance();
    }

    @Override
    public boolean hasNext() {
        return _currentLineTokens != null && _currentTokenIndex < _currentLineTokenCount;
    }

    @Override
    public HOCRToken next() {
        if (!hasNext())
            throw new NoSuchElementException();

        Element wordXml = (Element) _currentLineTokens.item(_currentTokenIndex);
        boolean isLastTokenOnLine = _currentTokenIndex == _currentLineTokenCount - 1;

        HOCRToken word = new HOCRToken(wordXml, isLastTokenOnLine);

        // Advance to next token
        advance();

        return word;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    protected void advance() {
        _currentTokenIndex++;

        if (_currentTokenIndex >= _currentLineTokenCount) {
            // No more tokens on current line - try to advance to next line
            _currentLineIndex++;
            _currentTokenIndex = 0;

            if (_currentLineIndex < _lineCount) {
                Node currentLineXml = _lines.item(_currentLineIndex);
                try {
                    _currentLineTokens = (NodeList) _xpathToken.evaluate(currentLineXml, XPathConstants.NODESET);
                    _currentLineTokenCount = _currentLineTokens.getLength();
                }
                catch (XPathExpressionException e) {
                    throw new RuntimeException(e);
                }
            } else
                // No more lines
                _currentLineTokens = null;
        }
    }
}
