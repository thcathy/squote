package squote.playground;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import squote.SquoteConstants;
import squote.domain.HoldingStock;
import squote.domain.StockExecutionMessage;
import squote.domain.StockExecutionMessageBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class CalculateAlgoEarning {
    private static final Logger log = LoggerFactory.getLogger(CalculateAlgoEarning.class);

    record DeletedRecord(LocalDateTime date, String id) {}
    record AddedRecord(LocalDateTime date, String id) {}

    List<DeletedRecord> deletedRecords = new ArrayList<>();
    Map<String, HoldingStock> holdings = new HashMap<>();
    List<HoldingStock> holdingFromExecMsg = new ArrayList<>();
    Map<String, AddedRecord> addedRecords = new HashMap<>();

    public static void main(String[] args) {
        var calculateAlgoEarning = new CalculateAlgoEarning();
        calculateAlgoEarning.readLogFiles(System.getenv("SQUOTE_LOG_FOLDER"));
        calculateAlgoEarning.calculateEarnings();
    }

    void calculateEarnings() {
        holdingFromExecMsg.sort(Comparator.comparing(HoldingStock::getDate));
        double buyAmount = 0, sellAmount = 0;
        int totalBuy = 0, totalSell = 0;

        for (var record : deletedRecords) {
            if (notMatchDateRange(record)) continue;

            var holding = findHolding(record);
            if (holding == null) {
                System.out.println("cannot find holding: " + record.id + "@" + record.date);
                continue;
            }

            if (holding.getSide() == SquoteConstants.Side.BUY) {
                totalBuy++;
                buyAmount += holding.getGross().doubleValue();
                System.out.println("buy holding: " + holding);
            } else {
                totalSell++;
                sellAmount += holding.getGross().doubleValue();
                System.out.println("sell holding: " + holding);
            }
        }
        System.out.println("Earning=" + (sellAmount - buyAmount));
        System.out.println("totalBuy=" + totalBuy + " totalSell=" + totalSell);
    }

    @Nullable
    private HoldingStock findHolding(DeletedRecord record) {
        var holding = holdings.get(record.id);
        if (holding != null) return holding;
        if (!addedRecords.containsKey(record.id)) return null;

        var addedHoldingTime = addedRecords.get(record.id).date;
        for (int i = holdingFromExecMsg.size() - 1; i >= 0; i--) {
            LocalDateTime dateAsLocalDateTime = holdingFromExecMsg.get(i).getDate().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            if (dateAsLocalDateTime.isBefore(addedHoldingTime)) {
                return holdingFromExecMsg.get(i);
            }
        }
        throw new RuntimeException("cannot find holding " + record.id);
    }

    private static boolean notMatchDateRange(DeletedRecord record) {
//        return record.date.getYear() != 2025;
        return record.date.getYear() != 2025 || record.date.getMonth() != Month.MARCH;
    }

    public void readLogFiles(String directoryPath) {
        Path dir = Paths.get(directoryPath);
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".log"))
                    .forEach(this::readFile);
        } catch (IOException e) {
            System.err.println("Error reading directory: " + e.getMessage());
        }
    }

    void readFile(Path filePath) {
        System.out.println("Read " + filePath);
        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            lines.forEach(this::processLog);
        } catch (IOException e) {
            System.err.println("Error reading file " + filePath + ": " + e.getMessage());
        }
    }

    void processLog(String log) {
        if (log.contains("RestStockController - delete")) {
//            deletedRecords.add(parseDeletedRecord(log));
        } else if (log.contains("RestStockController - Deleting buy/sell pair:")) {
            var records = parseBuySellPairRecord(log);
            deletedRecords.addAll(records);
        } else if (log.contains("created holding=")) {
            var holding = parseHoldingStock(log);
            holdings.put(holding.getId(), holding);
        } else if (log.contains("createHoldingStockFromExecution:") && log.contains("2800")) {
            var holding = parseExecMsg(log);
            holdingFromExecMsg.add(holding);
        } else if (log.contains("updateFundByHolding: add holding")) {
            var record = parseAddedRecord(log);
            addedRecords.put(record.id, record);
        }
    }

    List<DeletedRecord> parseBuySellPairRecord(String log) {
        Pattern pattern = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}).*?Deleting buy/sell pair: ([a-f0-9\\-]+), ([a-f0-9\\-]+)");
        Matcher matcher = pattern.matcher(log);

        if (matcher.find()) {
            String dateTimeStr = matcher.group(1);
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
            String id1 = matcher.group(2);
            String id2 = matcher.group(3);
            
            List<DeletedRecord> records = new ArrayList<>();
            records.add(new DeletedRecord(dateTime, id1));
            records.add(new DeletedRecord(dateTime, id2));
            return records;
        }
        throw new RuntimeException("Could not parse buy/sell pair log: " + log);
    }

    AddedRecord parseAddedRecord(String log) {
        Pattern pattern = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}).*?(\\b[0-9a-fA-F-]{36}\\b)");
        Matcher matcher = pattern.matcher(log);

        if (matcher.find()) {
            String dateStr = matcher.group(1);
            String uuid = matcher.group(2);
            LocalDateTime dateTime = LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
            return new AddedRecord(dateTime, uuid);
        }
        throw new RuntimeException("Could not parse log: " + log);
    }

    HoldingStock parseExecMsg(String log) {
        Pattern pattern = Pattern.compile("execution msg \\[(.*?)\\]");
        Matcher matcher = pattern.matcher(log);

        if (matcher.find()) {
            var msg = matcher.group(1);
            StockExecutionMessage executionMessage = StockExecutionMessageBuilder.build(msg).get();
            try {
                executionMessage.date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(log.substring(0, 24));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            return HoldingStock.from(executionMessage, "", null);
        }
        throw new RuntimeException("Could not parse execution msg: " + log);
    }

    HoldingStock parseHoldingStock(String logLine) {
        logLine = logLine.replaceAll("created holding=HoldingStock\\[", "");
        Pattern pattern = Pattern.compile("(\\w+)=([^,\\]]+|<null>)");
        Matcher matcher = pattern.matcher(logLine);

        Map<String, String> fields = new HashMap<>();

        while (matcher.find()) {
            fields.put(matcher.group(1), matcher.group(2));
        }

        // Parse extracted values
        String code = fields.get("code");
        Date date = null;
        try {
            date = fields.containsKey("date") && !fields.get("date").equals("<null>")
                    ? new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy").parse(fields.get("date"))
                    : null;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        BigDecimal gross = fields.containsKey("gross") ? new BigDecimal(fields.get("gross")) : BigDecimal.ZERO;
        int quantity = fields.containsKey("quantity") ? Integer.parseInt(fields.get("quantity")) : 0;
        String id = fields.getOrDefault("id", UUID.randomUUID().toString());
        String userId = fields.get("userId");
        String fundName = fields.get("fundName");
        String fillIds = fields.get("fillIds");
        BigDecimal hsce = fields.containsKey("hsce") && !fields.get("hsce").equals("<null>")
                ? new BigDecimal(fields.get("hsce"))
                : null;
        SquoteConstants.Side side = fields.containsKey("side") ? SquoteConstants.Side.valueOf(fields.get("side")) : null;

        // Construct the HoldingStock object
        HoldingStock holdingStock = new HoldingStock(id, userId, code, side, quantity, gross, date, hsce);
        holdingStock.setFillIds(fillIds);
        holdingStock.setFundName(fundName);

        return holdingStock;
    }


    DeletedRecord parseDeletedRecord(String log) {
        Pattern pattern = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}).*?id \\[([a-f0-9\\-]+)]");
        Matcher matcher = pattern.matcher(log);

        if (matcher.find()) {
            String dateTimeStr = matcher.group(1);
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
            String id = matcher.group(2);
            return new DeletedRecord(dateTime, id);
        }
        throw new RuntimeException("Could not parse log: " + log);
    }
}
