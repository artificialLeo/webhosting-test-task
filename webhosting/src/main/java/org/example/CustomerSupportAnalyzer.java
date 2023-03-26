package org.example;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class CustomerSupportAnalyzer {

    public static void main(String[] args) {
        String input = "7\n" +
                "C 1.1 8.15.1 P 15.10.2012 83\n" +
                "C 1 10.1 P 01.12.2012 65\n" +
                "C 1.1 5.5.1 P 01.11.2012 117\n" +
                "D 1.1 8 P 01.01.2012-01.12.2012\n" +
                "C 3 10.2 N 02.10.2012 100\n" +
                "D 1 * P 8.10.2012-20.11.2012\n" +
                "D 3 10 P 01.12.2012";

        String[] lines = input.split("\n");
        int s = Integer.parseInt(lines[0]);

        List<WaitingTimeline> timelines = new ArrayList<>();
        for (int i = 1; i <= s; i++) {
            String line = lines[i];
            if (line.charAt(0) == 'C') {
                timelines.add(WaitingTimeline.fromInputLine(line));
            } else {
                Query query = Query.fromInputLine(line);
                int result = query.getAverageWaitingTime(timelines);
                System.out.println(result == -1 ? "-" : result);
            }
        }
    }
}

class WaitingTimeline {
    String serviceId;
    String questionTypeId;
    String responseType;
    Date date;
    int waitingTime;

    static SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

    static WaitingTimeline fromInputLine(String line) {
        String[] parts = line.split(" ");
        WaitingTimeline timeline = new WaitingTimeline();
        timeline.serviceId = parts[1];
        timeline.questionTypeId = parts[2];
        timeline.responseType = parts[3];
        try {
            timeline.date = dateFormat.parse(parts[4]);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        timeline.waitingTime = Integer.parseInt(parts[5]);
        return timeline;
    }

    boolean matchesQuery(Query query) {
        if (!query.serviceId.equals("*") && !this.serviceId.startsWith(query.serviceId)) {
            return false;
        }
        if (!query.questionTypeId.equals("*") && !this.questionTypeId.startsWith(query.questionTypeId)) {
            return false;
        }
        if (!this.responseType.equals(query.responseType)) {
            return false;
        }
        return !query.dateFrom.after(this.date) && !query.dateTo.before(this.date);
    }
}

class Query {
    String serviceId;
    String questionTypeId;
    String responseType;
    Date dateFrom;
    Date dateTo;

    static SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

    static Query fromInputLine(String line) {
        String[] parts = line.split(" ");
        Query query = new Query();
        query.serviceId = parts[1];
        query.questionTypeId = parts[2];
        query.responseType = parts[3];
        String[] dateRange = parts[4].split("-");
        try {
            query.dateFrom = dateFormat.parse(dateRange[0]);
            query.dateTo = dateRange.length > 1 ? dateFormat.parse(dateRange[1]) : query.dateFrom;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return query;
    }

    int getAverageWaitingTime(List<WaitingTimeline> timelines) {
        List<Integer> matchingWaitingTimes = timelines.stream()
                .filter(this::matchesQuery)
                .map(timeline -> timeline.waitingTime)
                .collect(Collectors.toList());

        if (matchingWaitingTimes.isEmpty()) {
            return -1;
        }

        int sum = matchingWaitingTimes.stream().mapToInt(Integer::intValue).sum();
        return sum / matchingWaitingTimes.size();
    }

    private boolean matchesQuery(WaitingTimeline timeline) {
        return timeline.matchesQuery(this);
    }
}