package edu.illinois.i3.emop.apps.statsbuilder.hocr;

import edu.illinois.i3.emop.apps.statsbuilder.OCRPageStats;

import java.util.Map;

public class HOCRPageStats extends OCRPageStats {

    private int _linesCount;
    private int _paragraphCount;

    public int getLinesCount() {
        return _linesCount;
    }

    public void setLinesCount(int linesCount) {
        _linesCount = linesCount;
    }

    public int getParagraphCount() {
        return _paragraphCount;
    }

    public void setParagraphCount(int paragraphCount) {
        _paragraphCount = paragraphCount;
    }

    @Override
    public Map<String, Object> toCsvEntry() {
        Map<String, Object> csvEntry = super.toCsvEntry();
        csvEntry.put("paragraphs", getParagraphCount());
        csvEntry.put("lines", getLinesCount());

        return csvEntry;
    }
}
