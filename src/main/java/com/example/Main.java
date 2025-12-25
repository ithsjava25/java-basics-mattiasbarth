package com.example;

import com.example.api.ElpriserAPI;

import java.util.Scanner;

import static com.example.api.ElpriserAPI.Prisklass.SE1;
import static com.example.api.ElpriserAPI.Prisklass.SE3;

public class Main {
    public static void main(String[] args) {
        ElpriserAPI elpriserAPI = new ElpriserAPI();
        Scanner scanner = new Scanner(System.in);

        //Ask user for Prisklass/zone
        System.out.println("What zone would you like to use? --zone SE1|SE2|SE3|SE4 (required)");
        String zone = scanner.next();

        switch(zone.toUpperCase().trim())  {
            case "SE1":
                System.out.println("Zone SE1");
                System.out.println(elpriserAPI.getPriser("2025-09-04", SE1));
                break;
            case "SE2":
                System.out.println("Zone SE2");
                break;
            case "SE3":
                System.out.println("Zone SE3");
                break;
            case "SE4":
                System.out.println("Zone SE4");
                break;
            default:
                System.out.println("Invalid input");
        }
    }
}