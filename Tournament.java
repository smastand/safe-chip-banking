import java.io.*;
import java.time.Instant;
import java.util.*;

/// A version of SimpleCashGame for tournaments.
/// The same program is used for SNG's and MTT's.
public class Tournament extends CashGame implements CompleteShorts {
    // database variables and lists
    private static double bank;
    private static int entries;
    private static ArrayList<String> players;
    private static ArrayList<Double> amounts;
    private static ArrayList<Integer> bullets;
    private static ArrayList<String> sideNames;
    private static ArrayList<ArrayList<String>> sidePlayers;

    public static void main(String[] args) {
        if (!(new File(TOURNAMENT_RULES_FILE_NAME)).exists()) {
            System.out.printf("There is no tournament rules file along path:\n%s\n", TOURNAMENT_FILE_NAME);
            return;
        }
        Scanner s = new Scanner(System.in);
        String inputString;
        char inputChar;

        for (;;) {
            readTournament();
            readSideGames();

            System.out.println("""
                    ----TOURNAMENT----
                    1: Player Entry
                    2: Add-Ons Phase
                    3: Cashout
                    4: Print Names List
                    5: Side Games Menu
                    6: Print Rules
                    7: Elimination Wave
                    9: Print Log
                    0: Quit""");

            inputString = s.nextLine();
            inputChar = inputString.isEmpty() ? '0' : inputString.charAt(0);

            if (inputChar == '1') {
                // 1 player entry
                System.out.print("---- PLAYER ENTRY ----\nEnter name: ");
                String name = s.nextLine().toLowerCase();
                if (nameCheck(name)) {
                    // player already exists
                    int index = players.indexOf(name);
                    System.out.printf("%s is already in the system for $%.2f.\nHow much is %s buying in for? $",
                            cap(name), amounts.get(index), cap(name));
                    double rebuy;
                    try {
                        rebuy = roundMoney(Double.parseDouble(s.nextLine()));
                    } catch (NumberFormatException e) {
                        System.out.println(NFI);
                        continue;
                    }

                    ArrayList<Integer> toAdd = new ArrayList<>();
                    for (int i = 0; i < sideNames.size(); i++) {
                        if (sidePlayers.get(i).contains(name)) {
                            System.out.printf("Type 1 to be part of side game %s : ", sideNames.get(i));
                            if (s.nextLine().contains("1"))
                                toAdd.add(i);
                        }
                    }
                    if (!toAdd.isEmpty()) {
                        System.out.printf("%s is also entering into side games:\n", cap(name));
                        for (int i : toAdd)
                            System.out.println(sideNames.get(i));
                    }

                    System.out.printf("%s is buying in on bullet %d for $%.2f. Good luck!" + NEFIRMN,
                            cap(name), bullets.get(index) + 1, rebuy);
                    if (!s.nextLine().equals("1")) {
                        System.out.println(CRECN);
                        continue;
                    }
                    amounts.set(index, amounts.get(index) + rebuy);
                    bullets.set(index, bullets.get(index) + 1);
                    entries += 1;
                    bank = roundMoney(bank + rebuy);
                    for (int i : toAdd)
                        sidePlayers.get(i).add(name);
                    writeTournament();
                    writeSideGames();
                    StringBuilder toLog = new StringBuilder(String.format("REBUY %s $%.2f%s",
                            cap(name), rebuy, toAdd.isEmpty() ? "" : " SIDES"));
                    for (int i : toAdd)
                        toLog.append(" ").append(sideNames.get(i));
                    logT(toLog.toString());
                    System.out.printf("%s is now on bullet %d.\n", cap(name), bullets.get(index));
                } else {
                    // add new player
                    System.out.printf("How much is %s buying in for? $", cap(name));
                    double buyin;
                    try {
                        buyin = roundMoney(Double.parseDouble(s.nextLine()));
                    } catch (NumberFormatException e) {
                        System.out.println(NFI);
                        continue;
                    }
                    ArrayList<Integer> toAdd = new ArrayList<>();
                    for (int i = 0; i < sideNames.size(); i++) {
                        System.out.printf("Type 1 to be part of side game %s : ", sideNames.get(i));
                        if (s.nextLine().contains("1"))
                            toAdd.add(i);
                    }
                    System.out.printf("%s is buying in for the first time for $%.2f.\n", cap(name), buyin);
                    if (!toAdd.isEmpty()) {
                        System.out.printf("%s is also playing side games:\n", cap(name));
                        for (int i : toAdd)
                            System.out.println(sideNames.get(i));
                    }
                    System.out.println("Press enter to confirm and type anything to cancel");
                    if (!s.nextLine().isEmpty()) {
                        System.out.println(CRECN);
                        continue;
                    }
                    players.add(name);
                    amounts.add(buyin);
                    bank = roundMoney(bank + buyin);
                    entries += 1;
                    bullets.add(1);
                    for (int i : toAdd)
                        sidePlayers.get(i).add(name);
                    writeTournament();
                    writeSideGames();
                    StringBuilder toLog = new StringBuilder(String.format("BUYIN %s $%.2f%s",
                            cap(name), buyin, toAdd.isEmpty() ? "" : " SIDES"));
                    for (int i : toAdd)
                        toLog.append(" ").append(sideNames.get(i));
                    logT(toLog.toString());
                    System.out.printf("Added %s to the list of players for $%.2f.\n\n", cap(name), buyin);
                }
                // end 1 player entry
            } else if (inputChar == '2') {
                // 2 addons
                System.out.print("---- ADDONS ----\nIMPORTANT: Once the addon process process starts, ");
                System.out.println("it cannot be stopped.\nType 1470 to confirm and anything else to cancel.");
                if (!s.nextLine().equals("1470")) {
                    System.out.println(CRECN);
                    continue;
                }
                System.out.println("Type the amount each player is adding on for.");
                double oldBank = bank;
                double addon = 0d;
                for (int i = 0; i < players.size(); i++) {
                    System.out.printf("%s $", cap(players.get(i)));
                    try {
                        addon = roundMoney(Double.parseDouble(s.nextLine()));
                    } catch (NumberFormatException e) {
                        System.out.println(NFI);
                        i--;
                        continue;
                    }
                    if (addon > 0) {
                        amounts.set(i, amounts.get(i) + addon);
                        bank = roundMoney(bank + addon);
                    }
                }
                logT(String.format("ADDONS $%.2f", addon));
                System.out.printf("Addons phase complete! Added $%.2f onto the bank.\n\n", roundMoney(bank - oldBank));
                // end 2 addons
            } else if (inputChar == '3') {
                // 3 cashout
                if (players.isEmpty()) {
                    System.out.println("There are no players!\n");
                    continue;
                }
                System.out.println("----CASHOUT----");
                if (inputString.length() == 1) {
                    System.out.println("Type 3 for cashout and anything else for bust out.");
                    inputString += s.nextLine();
                }
                System.out.printf("Enter %sout name: ", inputString.contains("33") ? "cash" : "bust");
                String name = s.nextLine();
                if (!nameCheck(name)) {
                    System.out.printf("Name %s not found in database!\n\n", cap(name));
                    continue;
                }
                int index = -1;
                for (int i = 0; i < players.size(); i++) {
                    if (name.equals(players.get(i))) {
                        index = i;
                        break;
                    }
                }
                if (inputString.contains("33")) {
                    // cash
                    if (players.isEmpty()) {
                        System.out.println("There are no players!\n");
                        continue;
                    }
                    System.out.printf("%s is cashing out. How much is %s receiving? $", cap(name), cap(name));
                    double amount;
                    try {
                        amount = roundMoney(Double.parseDouble(s.nextLine()));
                    } catch (NumberFormatException e) {
                        System.out.println(NFI);
                        continue;
                    }
                    System.out.printf("""
                            %s is out of the tournament, $%.2f in the prize fund from them.
                            The bank will pay %s $%.2f. %s
                            \n""", cap(name), amounts.get(index), cap(name), amount, EFIRM);
                    if (!s.nextLine().isEmpty()) {
                        System.out.println(CRECN);
                        continue;
                    }
                    bank = roundMoney(bank - amount);
                    players.remove(index);
                    amounts.remove(index);
                    bullets.remove(index);
                    writeTournament();
                    logT(String.format("CASHOUT %s $%.2f", cap(name), amount));
                    System.out.printf("Thanks for playing, %s! Enjoy your $%.2f.\n\n", cap(name), amount);
                } else {
                    // bustout
                    System.out.printf("%s is out. $%.2f in the prize fund from them. %s\n",
                            cap(name), amounts.get(index), EFIRM);
                    if (!s.nextLine().equals("1")) {
                        System.out.println(CRECN);
                        continue;
                    }
                    players.remove(index);
                    amounts.remove(index);
                    bullets.remove(index);
                    writeTournament();
                    logT(String.format("CASHOUT %s $0.00", cap(name)));
                    System.out.printf("Thanks for playing, %s!\n\n", cap(name));
                }
                // end 3 cashout
            } else if (inputChar == '4') {
                // 4 print names list
                if (players.isEmpty()) {
                    System.out.println("There are no players!\n");
                    continue;
                }
                System.out.println("----LIST OF PLAYERS----");
                System.out.printf("Bank: $%,.2f\nPlayers: %d\nEntries: %d\n\n", bank, players.size(), entries);
                for (int i = 0; i < players.size(); i++) {
                    System.out.printf("%s | $%.2f | %d Entr%s", cap(players.get(i)), amounts.get(i), bullets.get(i),
                            bullets.get(i) > 1 ? "ies" : "y");
                    for (int j = 0; j < sideNames.size(); j++) {
                        if (sidePlayers.get(j).contains(players.get(i))) {
                            System.out.printf(" | %s", sideNames.get(j));
                        }
                    }
                    System.out.println();
                }
                System.out.println(NECON);
                s.nextLine();
                // end 4 print names list
            } else if (inputChar == '5') {
                // 5 side games
                if (inputString.length() == 1) {
                    System.out.println("""
                            ****SIDE GAMES MENU****
                            1: Create New Side Game
                            2: Delete Side Game
                            3: Add Player to Side Game
                            4: Print Side Games List
                            0: Exit""");
                    inputString += s.nextLine();
                }

                if (inputString.equals("51")) {
                    // 51 create sg
                    System.out.print("Enter the name of the new side game: ");
                    String gameName = s.nextLine();
                    if (sideNames.contains(gameName)) {
                        System.out.printf("Side game %s already exists!\n\n", gameName);
                        continue;
                    }
                    System.out.printf("Creating new side game %s. Press enter to confirm and type anything to cancel.",
                            gameName);
                    if (s.nextLine().isEmpty()) {
                        sideNames.add(gameName);
                        sidePlayers.add(new ArrayList<>());
                        writeSideGames();
                        System.out.printf("Created new side game %s.\n\n", gameName);
                        logT("SIDEGAME CREATE %s");
                    } else {
                        System.out.println(CRECN);
                    }
                    // end 51 create sg
                } else if (inputString.equals("52")) {
                    // 52 delete sg
                    if (sideNames.isEmpty()) {
                        System.out.println("There are no side games! Can't delete one.\n");
                        continue;
                    }
                    for (int i = 0; i < sideNames.size(); i++) {
                        System.out.printf("%d: %s\n", i, sideNames.get(i));
                    }
                    System.out.print("\nEnter the index of the side game to delete: ");
                    try {
                        int index = Integer.parseInt(s.nextLine());
                        if (index < 0 || index >= sideNames.size()) {
                            System.out.println("Invalid index provided!\n");
                            continue;
                        }
                        System.out.printf("Deleting side game %s. Players in side game are:\n", sideNames.get(index));
                        for (String person : sidePlayers.get(index))
                            System.out.print(person + " = ");
                        System.out.println("\nPress enter to continue and type anynthing to cancel.");
                        if (s.nextLine().isEmpty()) {
                            System.out.printf("Deleted side game %s.\n\n", sideNames.get(index));
                            logT("SIDEGAME DELETE " + sideNames.get(index));
                            sideNames.remove(index);
                            sidePlayers.remove(index);
                            writeSideGames();
                        } else {
                            System.out.println(CRECN);
                        }
                    } catch (NumberFormatException e) {
                        System.out.println(NFI);
                    }
                    // end 52 delete sg
                } else if (inputString.equals("53")) {
                    // 53 add player to sg
                    System.out.print("Enter player name: ");
                    String name = s.nextLine().toLowerCase();
                    if (!nameCheck(name)) {
                        System.out.println("Name does not exist in system!\n");
                        continue;
                    }
                    for (int i = 0; i < sideNames.size(); i++) {
                        System.out.printf("%d: %s\n", i, sideNames.get(i));
                    }
                    System.out.print("\nEnter the index of the side game to play: ");
                    try {
                        int index = Integer.parseInt(s.nextLine());
                        if (index < 0 || index >= sideNames.size()) {
                            System.out.println("Invalid index provided!\n");
                            continue;
                        }
                        System.out.printf("Adding %s to side game %s.\n", cap(name), sideNames.get(index));
                        System.out.println("Press enter to confirm and anything else to cancel.");
                        if (s.nextLine().isEmpty()) {
                            sidePlayers.get(index).add(name);
                            System.out.printf("Added %s to side game %s.\n\n", cap(name), sideNames.get(index));
                            writeSideGames();
                            logT(String.format("SIDEGAME ADD %s %s", cap(name), sideNames.get(index)));
                        } else {
                            System.out.println(CRECN);
                        }
                    } catch (NumberFormatException e) {
                        System.out.println(NFI);
                    }
                    // end 53 add player to sg
                } else if (inputString.equals("54")) {
                    // 54 print list
                    if (sideNames.isEmpty()) {
                        System.out.println("There are no side games!\n");
                        continue;
                    }
                    for (int i = 0; i < sideNames.size(); i++) {
                        System.out.print(sideNames.get(i));
                        for (String name : sidePlayers.get(i))
                            System.out.printf(" %s %s",
                                    name.equals(sidePlayers.get(i).getFirst()) ? ":" : "=", cap(name));
                        System.out.println();
                    }
                    System.out.println(NECON);
                    s.nextLine();
                    // end 54 print list
                }
                // end 5 side games
            } else if (inputChar == '6') {
                printRules();
                System.out.println(NECON);
                s.nextLine();
            } else if (inputChar == '7') {
                // 7 elimination wave
                System.out.println("---- ELIMINATION WAVE ----");
                System.out.println("Once the process starts, it must be finished. Type 2580 to confirm.");
                if (!s.nextLine().equals("2580")) {
                    System.out.println(CRECN);
                    continue;
                }
                try {
                    System.out.print("Cashout for this elimination wave: $");
                    double amount = roundMoney(Double.parseDouble(s.nextLine()));
                    ArrayList<String> toRemove = new ArrayList<>();
                    double toCash = 0d;
                    System.out.println("Type 1 to eliminate a name and press enter to keep it");
                    StringBuilder ending = new StringBuilder();
                    for (String name : players) {
                        System.out.println(cap(name) + "  ");
                        if (s.nextLine().equals("1")) {
                            ending.append(String.format("%s eliminated%s.\n", cap(name),
                                    amount > 0 ? String.format(" and cashed $%,.2f", amount) : ""));
                            toCash += amount;
                            toRemove.add(name);
                        }
                        if (toCash + amount > bank) {
                            System.out.println("The bank cannot accomodate another elimination! Ending the process.");
                            break;
                        }
                    }
                    if (toCash > 0)
                        ending.append(String.format("Removing $%,.2f from the bank to pay eliminations.\n", toCash));
                    System.out.println(ending);
                    System.out.println(NEFIRMN);
                    if (!s.nextLine().isEmpty()) {
                        bank = roundMoney(bank - toCash);
                        for (int i = players.size() - 1; i >= 0; i--) {
                            if (toRemove.contains(players.get(i))) {
                                players.remove(i);
                                amounts.remove(i);
                                bullets.remove(i);
                            }
                        }
                    }
                    System.out.println("Elimination phase complete.\n");
                } catch (NumberFormatException e) {
                    System.out.println(NFI);
                }
                // end 7 elimination wave
            } else if (inputChar == '9') {
                printLog();
                System.out.println(NECON);
                s.nextLine();
            } else if (inputString.equals("factoryreset")) {
                // factoryreset
                if (SecurityCheck.securityCheck()) {
                    System.out.println("Do you really want to go through with a factory reset?");
                    System.out.println("If yes, type your name in all uppercase");
                    if (!s.nextLine().equals(SecurityCheck.ADMIN_NAME.toUpperCase())) {
                        System.out.println(CRECN);
                        continue;
                    }
                    bank = 0d;
                    entries = 0;
                    players = new ArrayList<>();
                    amounts = new ArrayList<>();
                    bullets = new ArrayList<>();
                    sideNames = new ArrayList<>();
                    sidePlayers = new ArrayList<>();
                    writeTournament();
                    writeSideGames();
                    boolean b = new File(LOG_FILE).delete();
                }
                // end factoryreset
            } else if (inputString.equals("changebank")) {
                // changebank
                if (SecurityCheck.securityCheck()) {
                    System.out.printf("The bank is currently at $%.2f.", bank);
                    System.out.print("What would you like to change the bank value to? $");
                    String line = s.nextLine();
                    try {
                        double change = roundMoney(Double.parseDouble(line));
                        System.out.printf("You are changing the bank from $%.2f to $%.2f.%s\n", bank, change, NECON);
                        if (!s.nextLine().isEmpty()) {
                            System.out.println(CRECN);
                            continue;
                        }
                        logC(String.format("BANK $%.2f -> $%.2f", bank, roundMoney(Double.parseDouble(line))));
                        bank = change;
                        System.out.println("Bank value has been successfully updated.\n");
                    } catch (NumberFormatException e) {
                        System.out.println(NFI);
                    }
                }
                // end changebank
            } else {
                if (inputString.equals("00")) {
                    System.out.println("Goodbye!");
                    System.exit(0);
                }
                if(!Arrays.toString(Thread.currentThread().getStackTrace()).contains("Combo.main"))
                    System.out.println("Goodbye!");
                break;
            }
        } // for
    } // main

    /// Writes the contents of 3 player database ArrayLists to the people file.
    /// (players, amounts, bullets)
    public static void writeTournament() {
        File f = new File(TOURNAMENT_FILE);
        try (PrintWriter pw = new PrintWriter(f)) {
            pw.println(bank);
            pw.println(entries);
            for (int i = 0; i < players.size(); i++) {
                pw.printf("%s;%.2f;%d\n", players.get(i), amounts.get(i), bullets.get(i));
            }
        } catch (IOException e) {
            System.out.println("Error writing to people file.");
        }
    } // write

    /// Reads the info from the people file and updates the 3 player database ArrayLists.
    /// (players, amounts, bullets)
    public static void readTournament() {
        players = new ArrayList<>();
        amounts = new ArrayList<>();
        bullets = new ArrayList<>();
        File f = new File(TOURNAMENT_FILE);
        if (f.exists()) {
            // file already exists
            try (BufferedReader bfr = new BufferedReader(new FileReader(f))) {
                bank = Double.parseDouble(bfr.readLine());
                entries = Integer.parseInt(bfr.readLine());
                String line;
                while ((line = bfr.readLine()) != null) {
                    String[] split = line.split(";");
                    players.add(split[0]);
                    amounts.add(Double.parseDouble(split[1]));
                    bullets.add(Integer.parseInt(split[2]));
                }
            } catch (IOException | NumberFormatException e) {
                System.out.println("Error reading from people file.");
            }
        } else {
            // need to create a new file
            try {
                boolean b = f.createNewFile();
            } catch (IOException e) {
                System.out.println("Error creating new people file. Try renaming the LOCAL_PATH variable!");
            }
            bank = 0d;
            entries = 0;
            writeTournament();
        }
    } // readSimplePeople

    /// Writes the contents of the 2 side games ArrayLists to the people file.
    /// (sideNames, sidePlayers)
    private static void writeSideGames() {
        File f = new File(SIDE_GAMES_FILE);
        try (PrintWriter pw = new PrintWriter(f)) {
            for (int i = 0; i < sideNames.size(); i++) {
                pw.print(sideNames.get(i));
                for (String person : sidePlayers.get(i)) {
                    pw.print(";" + person);
                }
                pw.println();
            }
        } catch (IOException e) {
            System.out.println("Error writing to side games file.");
        }
    }

    /// Reads the info from the side games file and updates the 2 ArrayLists.
    /// (sideNames, sidePlayers)
    private static void readSideGames() {
        File f = new File(SIDE_GAMES_FILE);
        sideNames = new ArrayList<>();
        sidePlayers = new ArrayList<>();
        try (BufferedReader bfr = new BufferedReader(new FileReader(f))) {
            String line;
            while((line = bfr.readLine()) != null) {
                String[] splitter = line.split(";");
                sideNames.add(splitter[0]);
                sidePlayers.add(new ArrayList<>());
                for (int i = 1; i < splitter.length; i++) {
                    sidePlayers.getLast().add(splitter[i]);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading from side games file.");
        }
    } // readSideGames

    /// Logs the string value with a timestamp and tournament watermark to the log file.
    /// @param text The string value to be added to the log.
    public static void logT(String text) {
        File f = new File(LOG_FILE);
        if (!f.exists()) {
            try {
                boolean b = f.createNewFile();
            } catch (IOException e) {
                System.out.println("Error creating new log file. Try renaming the LOCAL_PATH variable!");
                return;
            }
        }
        try (PrintWriter pw = new PrintWriter(f)) {
            pw.println("[" + Instant.now().toString().substring(0, 19).replaceFirst("T", " @ ") + "Z] {T} " + text);
        } catch (IOException e) {
            System.out.println("Error writing to log file.");
        }
    } // log

    /// Prints the rules of the game by reading from tournamentrules.txt
    private static void printRules() {
        File f = new File(TOURNAMENT_RULES_FILE_NAME);
        try (BufferedReader bfr = new BufferedReader(new FileReader(f))) {
            System.out.println("---- TOURNAMENT RULES ----");
            String line;
            while ((line = bfr.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            System.out.println("IO Error reading from rules file.");
        }
    } // printRules

    /// Returns a boolean for whether the name exists in the database or not.
    /// @param name The name to check.
    /// @return Boolean whether the table exists or not.
    public static boolean nameCheck(String name) {
        for (String el : players)
            if (name.equals(el))
                return true;
        return false;
    } // nameCheck

} // class
