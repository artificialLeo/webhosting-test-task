package org.example;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;


public class App {

    private static String webData = "" +
            "7\n" +
            "C 1.1 8.15.1 P 15.10.2012 83\n" +
            "C 1 10.1 P 01.12.2012 65\n" +
            "C 1.1 5.5.1 P 01.11.2012 117\n" +
            "D 1.1 8 P 01.01.2012-01.12.2012\n" +
            "C 3 10.2 N 02.10.2012 100\n" +
            "D 1 * P 8.10.2012-20.11.2012\n" +
            "D 3 10 P 01.12.2012\n";

    public static void main( String[] args ) {
        System.out.println( calculateReplyWaitingTime() );
    }

    private static String calculateReplyWaitingTime() {
        String[] webHostingQueryParameters = webData.split("\n");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < webHostingQueryParameters.length; i++) {
            if (webHostingQueryParameters[i].charAt(0) == 'D') {
                result.append(getReplyWaitingTimeFromQuery(Arrays.copyOfRange(webHostingQueryParameters, 0, i + 1))).append("\n");
            }
        }

        return result.toString();
    }

    private static String getReplyWaitingTimeFromQuery(String[] dataForQuery) {
        String result = "";
        int dataForQueryLength = dataForQuery.length;
        QueryLine queryLine = new QueryLine(dataForQuery[dataForQueryLength - 1]);

        WaitingTimeline[] waitingTimelines = new WaitingTimeline[dataForQueryLength - 1];

        for (int i = 0; i < dataForQuery.length - 1; i++) {
            if (dataForQuery[i].charAt(0) == 'C') {
                WaitingTimeline parsedTimeline = new WaitingTimeline(dataForQuery[i]);
                waitingTimelines[i] = parsedTimeline;
            }
        }

        return matchTimelinesToQueryAndReturnStringResult(queryLine, waitingTimelines);
    }

    private static String matchTimelinesToQueryAndReturnStringResult(QueryLine queryLine, WaitingTimeline[] waitingTimelines) {
        int waitingTimelineMatchesCounter = 0;
        int waitingTimeSum = 0;
        int waitingTimelinesWhichMatchesTheQuery = 0;

        for (int i = 0; i < waitingTimelines.length; i++) {
            if (waitingTimelines[i] == null) {
                continue;
            }

            if (queryLine.serviceIdNotParsedLength == 1) {
                if (queryLine.serviceId == 0 || queryLine.serviceId == waitingTimelines[i].serviceId) {
                    waitingTimelineMatchesCounter++;
                } else {
                    waitingTimelineMatchesCounter = 0;
                    continue;
                }
            } if (queryLine.serviceIdNotParsedLength == 2) {
                if (queryLine.serviceId == waitingTimelines[i].serviceId
                        && queryLine.variationId == waitingTimelines[i].variationId) {
                    waitingTimelineMatchesCounter++;
                } else {
                    waitingTimelineMatchesCounter = 0;
                    continue;
                }
            }

            if (queryLine.questionTypeIdNotParsedLength == 1) {
                if (queryLine.typeId == 0 || queryLine.typeId == waitingTimelines[i].typeId) {
                    waitingTimelineMatchesCounter++;
                } else {
                    waitingTimelineMatchesCounter = 0;
                    continue;
                }
            } if (queryLine.questionTypeIdNotParsedLength == 2) {
                if (queryLine.typeId == waitingTimelines[i].typeId
                        && queryLine.categoryId == waitingTimelines[i].categoryId) {
                    waitingTimelineMatchesCounter++;
                } else {
                    waitingTimelineMatchesCounter = 0;
                    continue;
                }
            } if (queryLine.questionTypeIdNotParsedLength == 3) {
                if (queryLine.typeId == waitingTimelines[i].typeId
                        && queryLine.categoryId == waitingTimelines[i].categoryId
                        && queryLine.subcategoryId == waitingTimelines[i].subcategoryId) {
                    waitingTimelineMatchesCounter++;
                } else {
                    waitingTimelineMatchesCounter = 0;
                    continue;
                }
            }

            if (queryLine.responseType == waitingTimelines[i].responseType) {
                waitingTimelineMatchesCounter++;
            } else {
                waitingTimelineMatchesCounter = 0;
                continue;
            }

            if (queryLine.dateRange) {
                if (waitingTimelines[i].responseDate > queryLine.fromDate && waitingTimelines[i].responseDate < queryLine.toDate) {
                    waitingTimelineMatchesCounter++;
                } else {
                    waitingTimelineMatchesCounter = 0;
                    continue;
                }
            } else {
                if (queryLine.fromDate == waitingTimelines[i].responseDate) {
                    waitingTimelineMatchesCounter++;
                } else {
                    waitingTimelineMatchesCounter = 0;
                    continue;
                }
            }

            if (waitingTimelineMatchesCounter == 4) {
                waitingTimelinesWhichMatchesTheQuery++;
                waitingTimeSum += waitingTimelines[i].waitingTime;
            }

        }

        int result = 0;
        if (waitingTimelinesWhichMatchesTheQuery == 0) {
            waitingTimelinesWhichMatchesTheQuery = 1;
        }
        result = waitingTimeSum / waitingTimelinesWhichMatchesTheQuery;

        return Integer.toString(result).equals("0") ? "-" : Integer.toString(result);
    }

    private static long convert_DD_MM_YYYY_toLong(String dateFromString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        String day = dateFromString.split("\\.")[0];
        String month = dateFromString.split("\\.")[1];
        String year = dateFromString.split("\\.")[2];

        if (day.length() == 1) {
            day = "0" + day;
        }
        if (month.length() == 1) {
            month = "0" + month;
        }

        dateFromString = day + "." + month + "." + year;

        LocalDate date = LocalDate.parse(dateFromString, formatter);



        LocalDateTime dateTime = date.atStartOfDay();
        Instant instant = dateTime.toInstant(ZoneOffset.UTC);

        return instant.toEpochMilli();

    }

    private static class WaitingTimeline {
        private final char type = 'C';

        private final int serviceId;
        private final int variationId;

        private final int typeId;
        private final int categoryId;
        private final int subcategoryId;

        private final char responseType;
        private final long responseDate;

        private final int waitingTime;

        public WaitingTimeline(String waitingTimeline) {
            String[] rawData = waitingTimeline.split(" ");

            String[] serviceIdNotParsed = rawData[1].split("\\.");
            int serviceId = Integer.parseInt(serviceIdNotParsed[0]);
            int variationId = 0;

            if (serviceIdNotParsed.length == 2) {
                variationId = Integer.parseInt(serviceIdNotParsed[1]);
            }

            this.serviceId = serviceId;
            this.variationId = variationId;

            String[] questionTypeIdNotParsed = rawData[2].split("\\.");
            int typeId = Integer.parseInt(questionTypeIdNotParsed[0]);
            int categoryId = 0;
            int subcategoryId = 0;

            if (questionTypeIdNotParsed.length == 2) {
                categoryId = Integer.parseInt(questionTypeIdNotParsed[1]);
            } else if (questionTypeIdNotParsed.length == 3) {
                categoryId = Integer.parseInt(questionTypeIdNotParsed[1]);
                subcategoryId = Integer.parseInt(questionTypeIdNotParsed[2]);
            }


            this.typeId = typeId;
            this.categoryId = categoryId;
            this.subcategoryId = subcategoryId;

            this.responseType = rawData[3].charAt(0);

            this.responseDate = convert_DD_MM_YYYY_toLong(rawData[4]);

            this.waitingTime = Integer.parseInt(rawData[5]);
        }

        public char getType() {
            return type;
        }

        public int getServiceId() {
            return serviceId;
        }

        public int getVariationId() {
            return variationId;
        }

        public int getTypeId() {
            return typeId;
        }

        public int getCategoryId() {
            return categoryId;
        }

        public int getSubcategoryId() {
            return subcategoryId;
        }

        public char getResponseType() {
            return responseType;
        }

        public long getResponseDate() {
            return responseDate;
        }

        public int getWaitingTime() {
            return waitingTime;
        }

        @Override
        public String toString() {
            return "WaitingTimeline{" +
                    "type=" + type +
                    ", serviceId=" + serviceId +
                    ", variationId=" + variationId +
                    ", typeId=" + typeId +
                    ", categoryId=" + categoryId +
                    ", subcategoryId=" + subcategoryId +
                    ", responseType=" + responseType +
                    ", responseDate=" + responseDate +
                    ", waitingTime=" + waitingTime +
                    '}';
        }
    }

    private static class QueryLine {
        private final char type = 'D';

        private final int serviceIdNotParsedLength;
        private final int serviceId;
        private final int variationId;

        private final int questionTypeIdNotParsedLength;
        private final int typeId;
        private final int categoryId;
        private final int subcategoryId;

        private final char responseType;

        private boolean dateRange = false;
        private final long fromDate;
        private final long toDate;

        public QueryLine(String queryLine) {
            String[] rawData = queryLine.split(" ");

            String[] serviceIdNotParsed = rawData[1].split("\\.");
            this.serviceIdNotParsedLength = serviceIdNotParsed.length;
            int serviceId = Objects.equals(serviceIdNotParsed[0], "*") ? 0 : Integer.parseInt(serviceIdNotParsed[0]);
            int variationId = 0;
            
            if (serviceIdNotParsed.length == 2) {
                variationId = Integer.parseInt(serviceIdNotParsed[1]);
            }

            this.serviceId = serviceId;
            this.variationId = variationId;

            String[] questionTypeIdNotParsed = rawData[2].split("\\.");
            this.questionTypeIdNotParsedLength = questionTypeIdNotParsed.length;
            int typeId = Objects.equals(questionTypeIdNotParsed[0], "*") ? 0 : Integer.parseInt(questionTypeIdNotParsed[0]);
            int categoryId = 0;
            int subcategoryId = 0;

            if (questionTypeIdNotParsed.length == 2) {
                categoryId = Integer.parseInt(questionTypeIdNotParsed[1]);
            } else if (questionTypeIdNotParsed.length == 3) {
                categoryId = Integer.parseInt(questionTypeIdNotParsed[1]);
                subcategoryId = Integer.parseInt(questionTypeIdNotParsed[2]);
            }

            this.typeId = typeId;
            this.categoryId = categoryId;
            this.subcategoryId = subcategoryId;

            this.responseType = rawData[3].charAt(0);

            long fromDate = convert_DD_MM_YYYY_toLong(rawData[4].split("-")[0]);
            long toDate = 0;

            if (rawData[4].split("-").length == 2) {
                toDate = convert_DD_MM_YYYY_toLong(rawData[4].split("-")[1]);
                this.dateRange = true;
            }

            this.fromDate = fromDate;
            this.toDate = toDate;
        }

        public char getType() {
            return type;
        }

        public int getServiceIdNotParsedLength() {
            return serviceIdNotParsedLength;
        }

        public int getServiceId() {
            return serviceId;
        }

        public int getVariationId() {
            return variationId;
        }

        public int getQuestionTypeIdNotParsedLength() {
            return questionTypeIdNotParsedLength;
        }

        public int getTypeId() {
            return typeId;
        }

        public int getCategoryId() {
            return categoryId;
        }

        public int getSubcategoryId() {
            return subcategoryId;
        }

        public char getResponseType() {
            return responseType;
        }

        public boolean dateRange() {
            return dateRange;
        }

        public long getFromDate() {
            return fromDate;
        }

        public long getToDate() {
            return toDate;
        }

        @Override
        public String toString() {
            return "QueryLine{" +
                    "type=" + type +
                    ", serviceIdNotParsedLength=" + serviceIdNotParsedLength +
                    ", serviceId=" + serviceId +
                    ", variationId=" + variationId +
                    ", questionTypeIdNotParsedLength=" + questionTypeIdNotParsedLength +
                    ", typeId=" + typeId +
                    ", categoryId=" + categoryId +
                    ", subcategoryId=" + subcategoryId +
                    ", responseType=" + responseType +
                    ", dateRange=" + dateRange +
                    ", fromDate=" + fromDate +
                    ", toDate=" + toDate +
                    '}';
        }
    }
}
