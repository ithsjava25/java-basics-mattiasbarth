package com.example;

import com.example.api.ElpriserAPI;
import com.example.api.ElpriserAPI.Elpris;
import com.example.api.ElpriserAPI.Prisklass;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) {

        //Läser argument
        Map<String, String> argsMap = parseArgs(args);

        if (argsMap.containsKey("--help")) {
            printHelp();
            return;
        }

        //Kollar att en zone valts
        if (!argsMap.containsKey("--zone")) {
            System.out.println("Du måste ange en zon. Vänligen försök igen.");
            printHelp();
            return;
        }

        //Kollar vilken zon som valts
        Prisklass zone;
        try {
            zone = Prisklass.valueOf(argsMap.get("--zone"));
        } catch (Exception e) {
            System.out.println("Felaktig zon. Använd SE1, SE2, SE3 eller SE4.");
            return;
        }

        LocalDate date = argsMap.containsKey("--date") ?
                LocalDate.parse(argsMap.get("--date")) : LocalDate.now();

        ElpriserAPI api = new ElpriserAPI();
        List<Elpris> prices = new ArrayList<>();

        //Hämta dagens och morgondagens priser
        prices.addAll(api.getPriser(date, zone));
        prices.addAll(api.getPriser(date.plusDays(1), zone));

        //Ta bort timmar som redan har passerat
        prices.removeIf(p -> p.timeEnd().isBefore(java.time.ZonedDateTime.now()));

        //Sorterar
        if (argsMap.containsKey("--sorted")) {
            prices.sort((p1, p2) -> Double.compare(p2.sekPerKWh(),  p1.sekPerKWh()));
        }

        if (prices.isEmpty()) {
            System.out.println("Ingen prisdata tillgänglig. Priserna för nästa dag uppdateras kl 13:00.");
            return;
        }

        //Skriv ut priser om inte "--charging" används
        if (!argsMap.containsKey("--charging")) {
            for (Elpris p : prices) {
                System.out.printf("%s %02d:00 -> %.2f SEK/kWh%n",
                        p.timeStart().toLocalDate(),
                        p.timeStart().getHour(),
                        p.sekPerKWh());
            }
        }

        // Statistik: medel, billigast och dyrast
        double sum = 0;
        Elpris cheapest = prices.getFirst();
        Elpris mostExpensive = prices.getFirst();

        for (Elpris p : prices) {
            sum += p.sekPerKWh();
            if (p.sekPerKWh() < cheapest.sekPerKWh()) cheapest = p;
            if (p.sekPerKWh() > mostExpensive.sekPerKWh()) mostExpensive = p;
        }

        double average = sum / prices.size();
        System.out.printf("%nMedelpris: %.2f SEK/kWh%n", average);
        System.out.printf("Billigast timme: %02d:00 (%.2f)%n", cheapest.timeStart().getHour(), cheapest.sekPerKWh());
        System.out.printf("Dyrast timme: %02d:00 (%.2f)%n", mostExpensive.timeStart().getHour(), mostExpensive.sekPerKWh());

        //EV-laddning
        if (argsMap.containsKey("--charging")) {
            int hours = Integer.parseInt(argsMap.get("--charging").replace("h", ""));
            findBestChargingWindow(prices, hours);
        }
    }

    private static void findBestChargingWindow(List<Elpris> prices, int hours) {
        double minSum = Double.MAX_VALUE;
        int startIndex = 0;

        for (int i = 0; i <= prices.size() - hours; i++) {
            double sum = 0;
            for (int j = 0; j < hours; j++) {
                sum += prices.get(i + j).sekPerKWh();
            }
            if (sum < minSum) {
                minSum = sum;
                startIndex = i;
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd 'kl' HH:mm");
        Elpris start = prices.get(startIndex);
        Elpris end = prices.get(startIndex + hours - 1);

        System.out.println("\nBästa fönster för laddning:");
        System.out.println("Start: " + start.timeStart().format(formatter));
        System.out.println("Slut : " + end.timeEnd().format(formatter));
        System.out.printf("Totalkostnad: %.2f SEK", minSum);
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    map.put(args[i], args[i + 1]);
                    i++;
                } else {
                    map.put(args[i], "");
                }
            }
        }
        return map;
    }

    private static void printHelp() {
        System.out.println("""
                Följande Command-Line Arguments kan användas:
                  --zone SE1|SE2|SE3|SE4 (välj zon, obligatoriskt)
                  --date YYYY-MM-DD (valfri, välj datum, dagens datum om inget väljs)
                  --sorted (valfri, ger högsta pris överst)
                  --charging 2h|4h|8h (valfri, hittar bästa fönster för laddning)
                  --help (ger den här menyn)
                """);
    }
}
