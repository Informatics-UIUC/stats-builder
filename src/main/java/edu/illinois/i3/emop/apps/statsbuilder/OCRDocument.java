package edu.illinois.i3.emop.apps.statsbuilder;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.encoder.CsvEncoder;
import org.supercsv.encoder.DefaultCsvEncoder;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.IOException;
import java.io.Writer;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OCRDocument<T extends OCRPageStats> {

    private static final Logger log = LoggerFactory.getLogger(OCRDocument.class);

    private final String _docId;
    private final Set<T> _pages;

    public OCRDocument(String docId) {
        _docId = docId;
        _pages = Sets.newTreeSet(new PageNumberComparator());
    }

    public String getDocId() {
        return _docId;
    }

    public Set<T> getPagesStats() {
        return _pages;
    }

    public void addPageStats(T pageStats) {
        _pages.add(pageStats);
    }

    public void writePageStatsCsv(Writer writer, boolean includeHeader) throws IOException {
        if (_pages.isEmpty()) {
            log.warn(String.format("Document %s contains 0 pages - no stats CSV will be created", _docId));
            return;
        }

        final CsvEncoder csvEncoder = new DefaultCsvEncoder();
        final CsvPreference csvPreference =
                new CsvPreference.Builder(CsvPreference.EXCEL_PREFERENCE)
                        .useEncoder(csvEncoder).build();

        try (ICsvMapWriter csvWriter = new CsvMapWriter(writer, csvPreference)) {
            List<String> columns = _pages.iterator().next().getAvailableColumns();
            String[] header = columns.toArray(new String[columns.size()]);

            if (includeHeader)
                csvWriter.writeHeader(header);

            for (T pageStats : _pages) {
                Map<String, Object> csvEntry = pageStats.toCsvEntry();
                csvWriter.write(csvEntry, header);
            }
        }
    }


    private final class PageNumberComparator implements Comparator<T> {
        @Override
        public int compare(T p1, T p2) {
            return Integer.compare(p1.getPageNumber(), p2.getPageNumber());
        }
    }

    @Override
    public String toString() {
        return _docId;
    }
}
