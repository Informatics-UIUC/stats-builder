package edu.illinois.i3.emop.apps.statsbuilder;


import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.martiansoftware.jsap.*;
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser;
import com.martiansoftware.jsap.stringparsers.FileStringParser;
import edu.illinois.i3.emop.apps.statsbuilder.exceptions.PageParserException;
import edu.illinois.i3.emop.apps.statsbuilder.hocr.HOCRPage;
import edu.illinois.i3.emop.apps.statsbuilder.hocr.HOCRPageStats;
import edu.illinois.i3.emop.apps.statsbuilder.stats.Bin;
import edu.illinois.i3.emop.apps.statsbuilder.stats.BinFactory;
import edu.illinois.i3.emop.apps.statsbuilder.txt.TxtPage;
import edu.illinois.i3.spellcheck.engine.SpellDictionary;
import edu.illinois.i3.spellcheck.engine.SpellDictionaryHashMap;
import opennlp.tools.tokenize.SimpleTokenizer;
import org.apache.commons.math3.stat.descriptive.AggregateSummaryStatistics;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.encoder.CsvEncoder;
import org.supercsv.encoder.DefaultCsvEncoder;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Application entry point
 *
 * @author capitanu
 *
 */

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public enum DocumentFormat {
        TXT, HOCR, GALEXML
    }

    public static void main(String[] args) throws Exception {
        JSAPResult cmdLine = parseArguments(args);

        File directory = cmdLine.getFile("directory");
        log.info("Using data directory: {}", directory);

        File[] dictFiles = cmdLine.getFileArray("dictionary");
        SpellDictionary[] dictionaries = getDictionaries(dictFiles);

        File[] replacementRuleFiles = cmdLine.getFileArray("replacements");
        Map<String, String> replacementRules = getReplacementRules(replacementRuleFiles);

        String output = cmdLine.getString("output");
        log.info("Output file: {}", output);
        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), Charsets.UTF_8));

        final DocumentFormat format = DocumentFormat.valueOf(cmdLine.getString("format").toUpperCase());
        String filter = cmdLine.getString("filter");

        // compute word length bins based on dictionary statistics
        Collection<SummaryStatistics> dictStats = new ArrayList<>();
        for (File dictFile : dictFiles) {
            try (Reader reader = new InputStreamReader(new FileInputStream(dictFile), Charsets.UTF_8)) {
                dictStats.add(createSummaryDictStats(reader));
            }
        }

        StatisticalSummary aggDictStats = AggregateSummaryStatistics.aggregate(dictStats);
        Bin[] wordLengthBins = getStatisticalBins(aggDictStats, new BinFactory<Integer>() {
            @Override
            public Bin<Integer> createBin(Double min, Double max) {
                return new Bin<>(
                        min != null ? (int) Math.ceil(min) : null,
                        max != null ? (int) Math.ceil(max) : null
                );
            }
        });

        if (log.isDebugEnabled()) {
            log.debug("n: {}", aggDictStats.getN());
            log.debug("min_length: {}", aggDictStats.getMin());
            log.debug("max_length: {}", aggDictStats.getMax());
            log.debug("mean_length: {}", aggDictStats.getMean());
            log.debug("stdev: {}", aggDictStats.getStandardDeviation());
            log.debug("bins: [{}]", Joiner.on(",").join(wordLengthBins));
        }

        final ProcessingOptions processingOptions = new ProcessingOptions();
        processingOptions.setDictionaries(dictionaries);
        processingOptions.setReplacementRules(replacementRules);
        processingOptions.setWordLengthBins(wordLengthBins);

        final CsvEncoder csvEncoder = new DefaultCsvEncoder();
        final CsvPreference csvPreference =
                new CsvPreference.Builder(CsvPreference.EXCEL_PREFERENCE)
                        .useEncoder(csvEncoder).build();

        try (ICsvMapWriter csvWriter = new CsvMapWriter(writer, csvPreference)) {
            List<String> columns = processingOptions.getCsvColumns(format);
            columns.add(0, "docId");
            final String[] header = columns.toArray(new String[columns.size()]);

            csvWriter.writeHeader(header);

            final Pattern filterPattern = Pattern.compile(filter);
            Path path = FileSystems.getDefault().getPath(directory.getAbsolutePath());
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                int count = 1;

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Matcher matcher = filterPattern.matcher(file.toString());
                    if (matcher.find()) {
                        if (matcher.groupCount() == 0) {
                            log.error("No groups matched the specified file filter - cannot compute document id!");
                            return FileVisitResult.TERMINATE;
                        }

                        log.info("{}: {}", count, file);

                        StringBuilder sb = new StringBuilder();
                        for (int i = 1, iMax = matcher.groupCount(); i <= iMax; i++)
                            sb.append("-").append(matcher.group(i));

                        String docId = sb.substring(1);

                        Map<String,Object> csvEntry = null;
                        switch (format) {
                            case HOCR:
                                HOCRPageStats hocrPageStats = processHOCR(docId, file.toFile(), processingOptions);
                                if (hocrPageStats != null)
                                    csvEntry = hocrPageStats.toCsvEntry();
                                break;

                            case GALEXML:
                                //processGALEXML(docId, file.toFile(), processingOptions);
                                break;

                            case TXT:
                                OCRPageStats txtPageStats = processTXT(docId, file.toFile(), processingOptions);
                                if (txtPageStats != null)
                                    csvEntry = txtPageStats.toCsvEntry();
                                break;

                            default:
                                log.error("Don't know how to process document format: {}", format);
                                System.exit(1);
                        }

                        if (csvEntry != null) {
                            csvEntry.put("docId", docId);
                            csvWriter.write(csvEntry, header);
                        }

                        count++;
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        }

        log.info("Finished");
    }

    private static OCRPageStats processTXT(String docId, File file, ProcessingOptions options) {
        try {
            TxtPage page = TxtPage.parse(file, SimpleTokenizer.INSTANCE);
            page.setWordLengthBins(options.getWordLengthBins());
            return page.calculateStatistics(options.getDictionaries(), options.getReplacementRules());
        }
        catch (PageParserException e) {
            log.error("Error processing page: " + file, e);
            return null;
        }
    }

    private static HOCRPageStats processHOCR(String docId, File file, ProcessingOptions options) {
        try {
            HOCRPage page = HOCRPage.parse(file);
            page.setWordLengthBins(options.getWordLengthBins());
            return page.calculateStatistics(options.getDictionaries(), options.getReplacementRules());
        }
        catch (PageParserException e) {
            log.error("Error processing page: " + file, e);
            return null;
        }
    }

    private static Bin[] getStatisticalBins(StatisticalSummary stat, BinFactory binFactory) {
        Bin[] bins = new Bin[8];

        double mean = stat.getMean();
        double stdev = stat.getStandardDeviation();

        Double low = null;
        for (int i = -3; i <= 3; i++) {
            double high = mean + i * stdev;
            bins[i+3] = binFactory.createBin(low, high);
            low = high;
        }

        bins[7] = binFactory.createBin(low, null);

        return bins;
    }

    private static void createDictWordLengthsCSV(Map<String, Map<Integer, Integer>> dictWordLengths, Writer writer) throws IOException {
        final CsvEncoder csvEncoder = new DefaultCsvEncoder();
        final CsvPreference csvPreference =
                new CsvPreference.Builder(CsvPreference.EXCEL_PREFERENCE)
                        .useEncoder(csvEncoder).build();

        try (ICsvMapWriter csvWriter = new CsvMapWriter(writer, csvPreference)) {
            int maxWordLength = 0;

            String[] header = new String[dictWordLengths.size() + 1];
            header[0] = "wordLength";
            int i = 1;

            for (Map.Entry<String, Map<Integer, Integer>> entry : dictWordLengths.entrySet()) {
                String dictName = entry.getKey();
                header[i] = dictName;
                maxWordLength = Math.max(maxWordLength, Ordering.natural().max(entry.getValue().keySet()));
                i++;
            }

            csvWriter.writeHeader(header);

            for (i = 1; i < maxWordLength; i++) {
                Map<String, Object> csvEntry = Maps.newHashMap();
                csvEntry.put(header[0], Integer.toString(i));

                for (Map.Entry<String, Map<Integer, Integer>> entry : dictWordLengths.entrySet()) {
                    Integer wordLengthCount = entry.getValue().get(i);
                    if (wordLengthCount == null)
                        wordLengthCount = 0;
                    csvEntry.put(entry.getKey(), wordLengthCount);
                }

                csvWriter.write(csvEntry, header);
            }
        }
    }

    private static SummaryStatistics createSummaryDictStats(Reader reader) throws IOException {
        SummaryStatistics stats = new SummaryStatistics();
        BufferedReader br = (reader instanceof BufferedReader) ? (BufferedReader) reader : new BufferedReader(reader);

        String word;
        while ((word = br.readLine()) != null) {
            int wordLength = word.length();
            stats.addValue(wordLength);
        }

        return stats;
    }

    private static Map<Integer, Integer> computeWordLengthDistForDict(File dictFile) throws IOException {
        Map<Integer, Integer> wordLengths = Maps.newHashMap();

        try (BufferedReader reader = new BufferedReader(new FileReader(dictFile))) {
            String word;
            while ((word = reader.readLine()) != null) {
                Integer wordLength = word.length();
                Integer wordLengthCount = wordLengths.get(wordLength);
                if (wordLengthCount == null)
                    wordLengthCount = 0;
                wordLengths.put(wordLength, wordLengthCount + 1);
            }
        }

        return wordLengths;
    }

    private static Map<String, String> getReplacementRules(File[] replacementRuleFiles) throws IOException {
        Map<String, String> replacementRules = Maps.newHashMap();

        if (replacementRuleFiles != null) {
            for (File replacementRuleFile : replacementRuleFiles) {
                log.info("Loading replacement rules: {}", replacementRuleFile);

                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(replacementRuleFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }

                String[] rules = sb.toString().split(";");
                for (String rule : rules) {
                    rule = rule.trim();
                    if (rule.isEmpty())
                        continue;

                    String[] parts = rule.split("=");
                    if (parts.length != 2)
                        throw new RuntimeException("Invalid replacement rule: " + rule);
                    replacementRules.put(parts[1].trim(), parts[0].trim());
                }
            }
        }

        return replacementRules;
    }

    private static SpellDictionary[] getDictionaries(File[] dictFiles) throws IOException {
        int dictCount = dictFiles.length;
        SpellDictionary[] dictionaries  = new SpellDictionary[dictCount];

        for (int i = 0; i < dictCount; i++) {
            File dictFile = dictFiles[i];
            String dictFileName = dictFile.getName();
            log.info("Loading dictionary: {}", dictFile);
            SpellDictionary dict = new SpellDictionaryHashMap(dictFile);
            dict.setName(dictFileName.substring(0, dictFileName.lastIndexOf(".")));
            dictionaries[i] = dict;
        }

        return dictionaries;
    }

    private static Parameter[] getApplicationParameters() {
        Parameter dictionary = new FlaggedOption("dictionary")
                .setStringParser(
                        FileStringParser.getParser()
                                .setMustBeFile(true)
                                .setMustExist(true))
                .setRequired(true)
                .setShortFlag('d')
                .setAllowMultipleDeclarations(true)
                .setHelp("Specifies one or more dictionaries to use");

        Parameter replacements = new FlaggedOption("replacements")
                .setStringParser(
                        FileStringParser.getParser()
                                .setMustBeFile(true)
                                .setMustExist(true))
                .setRequired(false)
                .setShortFlag('r')
                .setAllowMultipleDeclarations(true)
                .setHelp("Specifies one or more files containing the replacement rules to apply");

        Parameter format = new FlaggedOption("format")
                .setStringParser(EnumeratedStringParser.getParser("txt;hocr;galexml"))
                .setRequired(true)
                .setShortFlag('f')
                .setHelp("Specifies the format of the input files");

        Parameter filter = new FlaggedOption("filter")
                .setRequired(true)
                .setShortFlag('x')
                .setHelp("A regex for matching the files to process in the folder; grouping should be used for specifying the document id");

        Parameter output = new FlaggedOption("output")
                .setRequired(true)
                .setShortFlag('o')
                .setHelp("Specifies the output CSV file to be created");

        Parameter directory = new UnflaggedOption("directory")
                .setStringParser(
                        FileStringParser.getParser()
                                .setMustBeDirectory(true)
                                .setMustExist(true))
                .setRequired(true)
                .setHelp("Directory containing the files to process");

        return new Parameter[] { dictionary, replacements, format, filter, output, directory };
    }

    private static String getApplicationHelp() {
        return "Tokenizes and spellchecks a set of input files from a given directory and returns statistics about the correctness of each document";
    }

    private static JSAPResult parseArguments(String[] args) throws JSAPException {
        SimpleJSAP jsap = new SimpleJSAP("StatsBuilder", getApplicationHelp(), getApplicationParameters());
        JSAPResult result = jsap.parse(args);

        if (jsap.messagePrinted())
            System.exit(1);

        return result;
    }
}