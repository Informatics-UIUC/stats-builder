package edu.illinois.i3.emop.apps.statsbuilder.ocrxml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class OCRXMLTokenIterator implements Iterator<String> {

	private static final Logger log = LoggerFactory.getLogger(OCRXMLTokenIterator.class);

	private final XPathExpression _xpathPage;
	private final XPathExpression _xpathToken;

	private final NodeList _pages;
	private final int _pageCount;
	private int _currentPageIndex;
	private NodeList _currentPageTokens;
	private int _currentPageTokenCount;
	private int _currentTokenIndex;


	public OCRXMLTokenIterator(InputStream ocrXmlDocumentStream) throws XPathExpressionException, SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

		Document document = documentBuilder.parse(ocrXmlDocumentStream);

        XPath xpath = XPathFactory.newInstance().newXPath();

		_xpathPage = xpath.compile("/book/text/page");
		_xpathToken = xpath.compile("pageContent//wd");

		_pages = (NodeList) _xpathPage.evaluate(document, XPathConstants.NODESET);
		_pageCount = _pages.getLength();

		_currentPageIndex = _currentTokenIndex = _currentPageTokenCount = -1;

		advance();
	}

	public boolean hasNext() {
		return _currentPageTokens != null && _currentTokenIndex < _currentPageTokenCount;
	}

	public String next() {
		if (!hasNext())
			throw new NoSuchElementException();

		String token = _currentPageTokens.item(_currentTokenIndex).getTextContent();

		// Advance to next token
		advance();

		return token;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	protected void advance() {
		_currentTokenIndex++;

		if (_currentTokenIndex >= _currentPageTokenCount) {
			// No more tokens on current page - try to advance to next page
			_currentPageIndex++;
			_currentTokenIndex = 0;

			if (_currentPageIndex < _pageCount) {
				Node currentPage = _pages.item(_currentPageIndex);
				try {
					_currentPageTokens = (NodeList) _xpathToken.evaluate(currentPage, XPathConstants.NODESET);
					_currentPageTokenCount = _currentPageTokens.getLength();
				}
				catch (XPathExpressionException e) {
					log.error("SHOULD NOT HAPPEN: Invalid XPath Expression", e);
					throw new RuntimeException(e);
				}
			} else
				// No more pages
				_currentPageTokens = null;
		}
	}
}
