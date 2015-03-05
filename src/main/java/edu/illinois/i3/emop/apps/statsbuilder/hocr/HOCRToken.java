package edu.illinois.i3.emop.apps.statsbuilder.hocr;

import edu.illinois.i3.emop.apps.statsbuilder.OCRToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.util.Properties;

/**
 * @author capitanu
 */
public class HOCRToken implements OCRToken {

    private static final Logger log = LoggerFactory.getLogger(HOCRToken.class);

    private final Element _tokenXml;
    private final boolean _isLastTokenOnLine;
    private final String _tokenId;
    private final Properties _tokenProperties;

    public HOCRToken(Element tokenXml, boolean isLastTokenOnLine) {
        _tokenXml = tokenXml;
        _isLastTokenOnLine = isLastTokenOnLine;
        _tokenId = _tokenXml.hasAttribute("id") ? _tokenXml.getAttribute("id") : null;

        _tokenProperties = new Properties();
        String title = _tokenXml.getAttribute("title");
        String[] props = title.split(";");
        for (String prop : props) {
            prop = prop.trim();
            int idx = prop.indexOf(" ");
            String propName = prop.substring(0, idx);
            String propValue = prop.substring(idx + 1);
            _tokenProperties.put(propName, propValue);
        }
    }

    public String getTokenId() {
        return _tokenId;
    }

    public Properties getTokenProperties() {
        return _tokenProperties;
    }

    public String getText() {
        return _tokenXml.getTextContent();
    }

    public boolean isLastTokenOnLine() {
        return _isLastTokenOnLine;
    }

    @Override
    public String toString() {
        return getText();
    }
}
