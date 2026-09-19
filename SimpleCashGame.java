import java.io.*;
import java.util.*;
import java.time.Instant;

/// A simplified version of CashGame.
/// It only includes one table for home games without multi-tabling.
public class SimpleCashGame implements CompleteShorts {
    // database variables and lists
    private static double bank;
    private static ArrayList<String> players;
    private static ArrayList<Double> amounts;
    private static ArrayList<String> types;

    private static ArrayList<String> waitingList;
    private static ArrayList<String> infoWaiters;
    private static final Scanner s = new Scanner(System.in);

    public static void main(String[] args) {
        String inputString;
        String queuePlayer = "";
        String queueDescr = "";
        readSimplePeople();
        readWaitList();

        for (;;) {
            // program loop
            if (!queuePlayer.isEmpty()) {
                inputString = "1";
            } else {
                System.out.println("""
                        ----CASH----
                        1: New Player
                        2: Rebuy
                        3: Cashout
                        4: Print Names List
                        5: Add to Waitlist
                        6: Remove from Waitlist
                        8: Payment Type Change
                        9: Print Log
                        0: Quit""");
                inputString = s.nextLine();
            }
            char inputChar = inputString.isEmpty() ? '0' : inputString.charAt(0);

            if (inputChar == '1') {
                // 1 add player

                String name;
                System.out.println("----NEW PLAYER----");

                if (queuePlayer.isEmpty()) {
                    System.out.print("Enter name: ");
                    name = s.nextLine().toLowerCase();
                } else {
                    System.out.printf("Adding from waitlist: %s%s\n",
                            cap(queuePlayer), queueDescr.isEmpty() ? "" : " [" + queueDescr + "]");
                    name = queuePlayer;
                }

                if (name.equals("dealer") || name.equals("changebank")) {
                    System.out.printf("%s is a reserved name in this program. Pick something else!\n\n", name);
                    continue;
                } else if (nameCheck(name)) {
                    System.out.printf("Name %s already exists. Please add a rebuy instead!\n\n", cap(name));
                    continue;
                } else if (name.isEmpty()) {
                    System.out.println("Please enter a name next time!\n");
                    continue;
                }

                System.out.printf("How much is %s buying in for? $", cap(name));
                double buyin;
                try {
                    if ((buyin = Double.parseDouble(s.nextLine())) == 0) {
                        System.out.println(CRECN);
                        queuePlayer = "";
                        queueDescr = "";
                        continue;
                    }
                } catch (NumberFormatException e) {
                    System.out.println(NFI);
                    continue;
                }

                String method = payQuery(name, true, false);
                if (method.equals("0"))
                    continue;

                players.add(name);
                amounts.add(buyin);
                types.add(method);
                bank = roundMoney(bank + buyin);
                System.out.printf("%s bought in for $%.2f using %s.\n\n",
                        cap(name), buyin, payMethods(method));
                queuePlayer = "";
                queueDescr = "";
                writeSimplePeople();
                logS(String.format("BUYIN %s $%.2f %s", cap(name), buyin, payMethods(method)));
                // end 1 add player
            } else if (inputChar == '2') {
                // 2 rebuy
                if (players.isEmpty()) {
                    System.out.println("There are no players, so no rebuys!\n");
                    continue;
                }
                System.out.print("----REBUY----\nEnter name: ");
                String name = s.nextLine().toLowerCase();

                if (name.isEmpty()) {
                    System.out.println("Please enter something next time!\n");
                    continue;
                }

                int index = -1;
                for (int i = 0; i < players.size(); i++) {
                    if (players.get(i).equals(name)) {
                        index = i;
                        break;
                    }
                }
                if (index == -1) {
                    System.out.printf("Name %s not found in playing players database\n\n", name);
                    continue;
                }

                System.out.printf("%s is in for $%.2f via %s.\n", cap(name), amounts.get(index),
                        types.get(index).contains("e") ? "PAY-AT-END" : payMethods(types.get(index)));
                System.out.printf("How much are you rebuying for, %s? $", cap(name));
                double buyin;
                try {
                    if ((buyin = roundMoney(Double.parseDouble(s.nextLine()))) == 0) {
                        System.out.println(CRECN);
                        continue;
                    }
                } catch (NumberFormatException e) {
                    System.out.println(NFI);
                    continue;
                }

                if (types.get(index).contains("e")) {
                    System.out.printf("%s is adding on $%.2f and paying at the end.\n",
                            cap(name), buyin);
                    System.out.print("Press enter to confirm and type anything to cancel. ");
                    if (s.nextLine().isEmpty()) {
                        System.out.println();
                        bank = roundMoney(bank + buyin);
                        amounts.set(index, roundMoney(amounts.get(index) + buyin));
                        writeSimplePeople();
                        logS(String.format("REBUY %s $%.2f", cap(name), buyin));
                    } else
                        System.out.println(CRECN);
                    continue;
                }
                String method = payQuery(name, false, true);
                if (method.equals("0"))
                    continue;

                bank = roundMoney(bank + buyin);
                amounts.set(index, roundMoney(amounts.get(index) + buyin));
                types.set(index, types.get(index) + method);
                System.out.printf("Rebuy confirmed. %s added on $%.2f via %s.\n\n",
                        cap(name), buyin, payMethods(method));
                writeSimplePeople();
                logS(String.format("REBUY %s $%.2f %s now $%.2f",
                        cap(name), buyin, payMethods(method), amounts.get(index)));
                // end 2 rebuy
            } else if (inputChar == '3') {
                // 3 cashout
                String name;
                System.out.println("----CASHOUT----");
                if (players.isEmpty()) {
                    System.out.println("There are no players, so cashout is dealer.");
                    name = "dealer";
                } else {
                    System.out.print("Enter name: ");
                    name = s.nextLine().toLowerCase();
                }
                if (name.equals("dealer")) {
                    System.out.printf("There is currently $%.2f in the bank.\n", bank);
                    System.out.print("How much is the dealer receiving? $");
                    try {
                        double amount = roundMoney(Double.parseDouble(s.nextLine()));
                        if (amount == 0) {
                            System.out.println(CRECN);
                        } else {
                            System.out.printf("Paying $%.2f to dealer.", amount);
                            System.out.println(NEFIRMS);
                            if (s.nextLine().isEmpty()) {
                                bank = roundMoney(bank - amount);
                                writeSimplePeople();
                                logS(String.format("CASHOUT Dealer $%.2f", amount));
                                System.out.printf("Dealer transaction complete for $%.2f.\n\n", amount);
                            } else {
                                System.out.println(CRECN);
                            }
                        }
                    } catch (NumberFormatException e) {
                        System.out.println(NFI);
                    }
                    continue;
                } // dealertip

                int index = -1;
                for (int i = 0; i < players.size(); i++) {
                    if (players.get(i).equals(name)) {
                        index = i;
                        break;
                    }
                }
                if (index == -1) {
                    System.out.printf("Name %s not found in playing players database\n\n", cap(name));
                    continue;
                }

                System.out.printf("There is currently $%.2f in the bank.\n", bank);
                System.out.printf("%s bought in for $%.2f using %s.\n",
                        cap(name), amounts.get(index), payMethods(types.get(index)));
                System.out.printf("Host, how much is %s cashing out for? (0 for bust) $", cap(name));
                try {
                    double cashout = roundMoney(Double.parseDouble(s.nextLine()));

                    if (cashout == 0) {
                        if (types.get(index).equals("e"))
                            System.out.printf("%s still owes $%.2f\n", cap(name), amounts.get(index));
                        System.out.printf("Thanks for playing, %s!", cap(name));
                        System.out.print(NEFIRMS);
                        if (!s.nextLine().isEmpty()) {
                            System.out.println(CRECN);
                            continue;
                        }
                        System.out.println();
                        logS(String.format("CASHOUT %s $0.00 net $%+.2f", cap(name), -amounts.get(index)));
                        cashOut(index, 0);

                    } else if (!types.get(index).contains("e")) {
                        // pay up front
                        System.out.printf("Cashing out %s for $%.2f. They bought in for $%.2f.",
                                cap(name), cashout, amounts.get(index));
                        System.out.print(NEFIRMS);

                        if (!s.nextLine().isEmpty()) {
                            System.out.println(CRECN);
                            continue;
                        }

                        logS(String.format("CASHOUT %s $%.2f net $%+.2f",
                                cap(name), cashout, cashout - amounts.get(index)));
                        cashOut(index, cashout);
                        System.out.printf("Transaction confirmed. Thanks for playing, %s!\n\n", cap(name));
                    } else {
                        // pay at end
                        System.out.printf("Cashing out %s for $%.2f. They still owe $%.2f. Net profit is $%+.2f.",
                                cap(name), cashout, amounts.get(index), cashout - amounts.get(index));
                        for (int i = 0; i < types.get(index).length(); i++) {
                            System.out.print(" " + payMethods(types.get(index).substring(i, i + 1)));
                        }
                        System.out.print("." + NEFIRMS);

                        if (!s.nextLine().isEmpty()) {
                            System.out.println(CRECN);
                            continue;
                        }
                        logS(String.format("CASHOUT %s $%.2f net $%+.2f",
                                cap(name), cashout, cashout - amounts.get(index)));
                        cashOut(index, cashout);
                        System.out.printf("Transaction confirmed. Thanks for playing, %s!\n\n", cap(name));
                    }
                } catch (NumberFormatException e) {
                    System.out.println(NFI);
                }
                // end 3 cashout
            } else if (inputChar == '4') {
                // 4 print names lists
                System.out.println("----LIST OF PLAYERS----");
                System.out.printf("Bank: $%,.2f\n", bank);

                if (players.isEmpty()) {
                    System.out.println("\nNO PLAYERS IN GAME");
                } else {
                    System.out.printf("\n==== CURRENT PLAYERS (%d) ====\n", players.size());
                    for (int i = 0; i < players.size(); i++) {
                        System.out.printf("%s: $%.2f |", cap(players.get(i)), amounts.get(i));
                        for (int j = 0; j < types.get(i).length(); j++)
                            System.out.printf(" %s", payMethods(types.get(i).substring(j, j + 1)));
                        System.out.println();
                    }
                }

                if (!waitingList.isEmpty()) {
                    System.out.printf("\n==== WAITING LIST (%d) ====\n", waitingList.size());
                    for (int i = 0; i < waitingList.size(); i++)
                        System.out.printf("%d - %s%s\n", i + 1, cap(waitingList.get(i)),
                                infoWaiters.get(i).isEmpty() ? "" : " [" + infoWaiters.get(i) + "]");
                }

                System.out.println(NECON);
                s.nextLine();
                // end 4 print lists
            } else if (inputChar == '5') {
                // 5 add to waitlist
                System.out.print("----ADD TO WAITLIST----\nEnter the name of the player to add to the list: ");
                String name = s.nextLine().toLowerCase();

                if (name.contains(";")) {
                    System.out.println("Name cannot contain a semicolon!\n");
                    continue;
                } else if (nameCheck(name)) {
                    System.out.printf("Name %s already exists in the system!\n\n", cap(name));
                    continue;
                }
                if (!name.isEmpty()) {
                    System.out.println("Enter any additional info here. Just press enter for no info.");
                    String addInfo = s.nextLine();
                    String tentative = addInfo.isEmpty() ? "" : " [" + addInfo + "]";
                    System.out.printf("%s%s will be added to the waitlist in position %d. " +
                                    "Press enter to confirm.\n",
                            cap(name), tentative, waitingList.size() + 1);
                    if (!s.nextLine().isEmpty()) {
                        System.out.printf("Waitlist addition for %s cancelled.\n\n", cap(name));
                        continue;
                    }
                    waitingList.add(name);
                    infoWaiters.add(addInfo);
                    writeWaitList();
                    logS("WAITLIST ADD " + cap(name));
                } else {
                    System.out.println("Name is empty. Please enter something!\n");
                }
                // end 5 add to waitlist
            } else if (inputChar == '6') {
                // 6 call from waiting list

                if (waitingList.isEmpty()) {
                    System.out.println("No players in waitlist. Seat remains open.\n");
                    continue;
                }

                System.out.println("""
                        ----CALL FROM WAITLIST----
                        1 to call player
                        2 to move to back of line
                        3 to remove from list""");

                for (int i = 0; i <= waitingList.size(); i++) {
                    if (waitingList.size() == i) {
                        System.out.println("Seat remains open.\n");
                        break;
                    }
                    System.out.print(cap(waitingList.get(i)) + "  ");
                    String input = s.nextLine();
                    if (input.equals("1")) {
                        // call player
                        System.out.printf("Removed %s from waitlist.\n\n", cap(waitingList.get(i)));
                        queuePlayer = waitingList.get(i);
                        queueDescr = infoWaiters.get(i);
                        logS("WAITLIST REMOVE " + cap(waitingList.get(i)));
                        waitingList.remove(i);
                        infoWaiters.remove(i);
                        writeWaitList();
                        break;
                    } else if (input.equals("2")) {
                        // move to back of line
                        System.out.printf("%s moved to back.\n", cap(waitingList.get(i)));
                        String tempName = waitingList.remove(i);
                        String tempDescr = infoWaiters.remove(i);
                        waitingList.add(tempName);
                        infoWaiters.add(tempDescr);
                        writeWaitList();
                        break;
                    } else if (input.equals("3")) {
                        System.out.printf("Removed %s from waitlist.\n\n", cap(waitingList.get(i)));
                        logS("WAITLIST REMOVE " + cap(waitingList.get(i)));
                        waitingList.remove(i);
                        infoWaiters.remove(i);
                        writeWaitList();
                        break;
                    }
                }
                // end 6 call from waiting list
            } else if (inputChar == '8') {
                // 8 change payment method
                System.out.print("----PAYMENT TYPE CHANGE----\nEnter name: ");
                String name = s.nextLine().toLowerCase();
                int index = -1;
                for (int i = 0; i < players.size(); i++) {
                    if (name.equals(players.get(i))) {
                        index = i;
                        break;
                    }
                }
                if (index == -1) {
                    System.out.printf("Name %s not found in any game.\n\n", cap(name));
                    continue;
                }
                System.out.printf("%s is playing via %s.\n", cap(name), types.get(index));
                StringBuilder newTypes = new StringBuilder();
                System.out.println("Enter types now, one by one. (Cash, Venmo, Zelle, End) When done, press enter.");
                String line;
                char c;
                line = s.nextLine().toLowerCase();
                if (line.isEmpty()) {
                    System.out.println("Please enter something next time!\n");
                    continue;
                }
                switch (line.charAt(0)) {
                    case 'e':
                        System.out.printf("%s is now paying at the end. " + EFIRMS, cap(name));
                        if (s.nextLine().isEmpty()) {
                            logS(String.format("TYPE CHANGE %s %s -> %s", cap(name), types.get(index), "e"));
                            types.set(index, "e");
                            writeSimplePeople();
                            System.out.println("Confirmed. Thy has been done.\n");
                        } else { System.out.println(CRECN); }
                        continue;
                    case 'c': case 'v': case 'z': break;
                    default: System.out.println("Invalid character! Returning to main menu.\n"); continue;
                }
                while (!line.isEmpty()) {
                    c = line.charAt(0);
                    switch (c) {
                        case 'c': case 'v': case 'z': newTypes.append(c); break;
                    }
                    System.out.print(newTypes + " + ");
                    line = s.nextLine();
                }

                System.out.printf("The new payment sequence for %s will be: %s",
                        cap(name), payMethods(newTypes.toString()));
                System.out.print(NEFIRMN);
                if (!s.nextLine().isEmpty()) {
                    System.out.println(CRECN);
                    continue;
                }
                logS(String.format("TYPE CHANGE %s %s -> %s", cap(name), types.get(index), newTypes));
                types.set(index, newTypes.toString());
                writeSimplePeople();
                System.out.println("Confirmed. Thy has been done.\n");
                // end 8 change payment method
            } else if (inputChar == '9') {
                printLog();
                System.out.println(NECON);
                s.nextLine();
            } else if (inputString.equals("factoryreset")) {
                // factory reset
                if (SecurityCheck.securityCheck()) {

                    System.out.println("Do you really want to go through with a factory reset?");
                    System.out.println("If yes, type your name in all caps");
                    if (!s.nextLine().equals(SecurityCheck.ADMIN_NAME.toUpperCase())) {
                        System.out.println(CRECN);
                        continue;
                    }

                    bank = 0d;
                    players = new ArrayList<>();
                    amounts = new ArrayList<>();
                    types = new ArrayList<>();
                    waitingList = new ArrayList<>();
                    queuePlayer = "";
                    writeSimplePeople();
                    writeWaitList();
                    boolean b = new File(LOG_FILE).delete();
                    System.out.println("Hello world. Restart complete.\n");
                } else {
                    System.out.println("Security check failed.");
                }
                // end factory reset
            } else if (inputString.equals("changebank")) {
                // change bank
                if (SecurityCheck.securityCheck()) {
                    System.out.printf("Hello, %s. The bank is currently at $%.2f.\n", SecurityCheck.ADMIN_NAME, bank);
                    System.out.println("Type x to cancel.");
                    System.out.print("What would you like to change the bank value to? $");
                    String line = s.nextLine();
                    if (line.equalsIgnoreCase("x")) {
                        System.out.println("Bank value change cancelled.\n");
                    } else {
                        try {
                            logS(String.format("BANK $%.2f -> $%.2f", bank, roundMoney(Double.parseDouble(line))));
                            bank = roundMoney(Double.parseDouble(line));
                            System.out.printf("Bank value changed to $%.2f.\n\n", bank);
                            writeSimplePeople();
                        } catch (NumberFormatException e) {
                            System.out.println(NFI);
                        }
                    }
                }
                // end change bank
            } else {
                // 0 leave
                if (inputString.equals("00")) {
                    System.out.println("Goodbye!");
                    System.exit(0);
                }
                if(!Arrays.toString(Thread.currentThread().getStackTrace()).contains("Combo.main"))
                    System.out.println("Goodbye!");
                break;
                // end 0 leave
            }
        } // for
    } // main

    /// Writes the contents of the player database ArrayLists to the people file.
    /// (players, amounts, types, locations)
    public static void writeSimplePeople() {
        File f = new File(SIMPLE_PEOPLE_FILE);
        try (PrintWriter pw = new PrintWriter(f)) {
            pw.println(bank);
            for (int i = 0; i < players.size(); i++) {
                pw.println(String.format("%s;%.2f;%s",
                        players.get(i), amounts.get(i), types.get(i)));
            }
        } catch (IOException e) {
            System.out.println("Error writing to people file.");
        }
    } // write

    /// Reads the info from the people file and updates the 5 ArrayLists.
    /// (database, players, amounts, types, locations)
    public static void readSimplePeople() {
        players = new ArrayList<>();
        amounts = new ArrayList<>();
        types = new ArrayList<>();
        File f = new File(SIMPLE_PEOPLE_FILE);
        if (f.exists()) {
            // file already exists
            try (BufferedReader bfr = new BufferedReader(new FileReader(f))) {
                bank = Double.parseDouble(bfr.readLine());
                String line;
                while ((line = bfr.readLine()) != null) {
                    String[] split = line.split(";");
                    players.add(split[0]);
                    amounts.add(Double.parseDouble(split[1]));
                    types.add(split[2]);
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
            writeSimplePeople();
        }
    } // readSimplePeople

    /// Writes the contents of the 2 waiting list ArrayLists to the waiting list file.
    /// (waitingList, infoWaiters)
    public static void writeWaitList() {
        File f = new File(WAITING_LIST_FILE);
        try (PrintWriter pw = new PrintWriter(f)) {
            for (int i = 0; i < waitingList.size(); i++)
                pw.printf("%s;%s\n", waitingList.get(i), infoWaiters.get(i).isEmpty() ? "." : infoWaiters.get(i));
        } catch (IOException e) {
            System.out.println("Error writing to waiting list file.");
        }
    } // writeWaitList

    /// Reads the info from the tables file and updates the 2 ArrayLists.
    /// (waitingList, infoWaiters)
    public static void readWaitList() {
        waitingList = new ArrayList<>();
        infoWaiters = new ArrayList<>();
        File f = new File(WAITING_LIST_FILE);
        if (f.exists()) {
            // file already exists
            try (BufferedReader bfr = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = bfr.readLine()) != null) {
                    String[] splitter = line.split(";");
                    waitingList.add(splitter[0]);
                    infoWaiters.add(splitter[1].equals(".") ? "" : splitter[1]);
                }
            } catch (IOException | NumberFormatException e) {
                System.out.println("Error reading from waiting list file.");
            }
        } else {
            // need to create a new file
            try {
                boolean b = f.createNewFile();
            } catch (IOException e) {
                System.out.println("Error creating new waiting list file. Try renaming the LOCAL_PATH variable!");
            }
        }
    } // readWaitList

    /// Logs the string value with a timestamp and simple cash game watermark to the log file.
    /// @param text The string value to be added to the log.
    public static void logS(String text) {
        File f = new File(LOG_FILE);
        if (!f.exists()) {
            try {
                boolean b = f.createNewFile();
            } catch (IOException e) {
                System.out.println("Error creating new log file. Try renaming the LOCAL_PATH variable!");
                return;
            }
        }
        try (PrintWriter pw = new PrintWriter(new FileWriter(f, true))) {
            pw.println("[" + Instant.now().toString().substring(0, 19).replaceFirst("T", " @ ") + "Z] {S} " + text);
        } catch (IOException e) {
            System.out.println("Error writing to log file.");
        }
    } // logS

    /// Prints specified values of the log, as prompted in the method.
    public static void printLog() {
        File f = new File(LOG_FILE);
        if (!f.exists()) {
            System.out.println("There is no log file!");
            return;
        }
        int matches = 0;
        int countL = 0;
        System.out.println("----PRINT LOG----\nEnter a search keyword if you like: ");
        String keyword = s.nextLine().toLowerCase();
        try (BufferedReader bfr = new BufferedReader(new FileReader(f))) {
            String line = bfr.readLine();
            while (line != null) {
                countL++;
                if (line.toLowerCase().contains(keyword)) {
                    System.out.println(line);
                    matches++;
                }
                line = bfr.readLine();
            }
            if (!keyword.isEmpty())
                System.out.print("\n" + (matches == 0 ? "No" : matches) + " matches found out of ");
            System.out.println(countL + " lines");
        } catch (FileNotFoundException e) {
            System.out.println("There is no log file!");
        } catch (IOException e) {
            System.out.println("Error reading from log file!");
        }
    }

    /// Rounds any double value to 2 decimal places, as would be money.
    /// @param money The monetary value to round.
    /// @return Double rounded to the penny (2 decimal places).
    public static double roundMoney(double money) {
        return (double) Math.round(money * 100) / 100;
    } // roundMoney

    /// Capitalizes the first letter of every name and makes the rest lowercase.
    /// @param name The name to capitalize.
    /// @return The capitalized name as a String.
    public static String cap(String name) {
        StringBuilder fullName = new StringBuilder();
        String[] all = name.split(" ");
        for (int i = 0; i < all.length; i++) {
            String init = all[i];
            if (!init.isEmpty()) {
                fullName.append(init.substring(0, 1).toUpperCase());
                if (init.length() > 1)
                    fullName.append(init.substring(1));
                if (i != all.length - 1)
                    fullName.append(" ");
            }
        }
        return fullName.toString();
    } // cap

    /// Returns a boolean for whether the name exists in the database or not.
    /// @param name The name to check.
    /// @return Returns a boolean whether the table exists or not.
    private static boolean nameCheck(String name) {
        for (String el : players) {
            if (name.equals(el))
                return true;
        }
        for (String el : waitingList) {
            el = el.split(" ")[0];
            if (name.equals(el))
                return true;
        }
        return false;
    } // nameCheck

    /// Cashes the player out and removes them from the database.
    /// @param playerIndex The index of the player in the database.
    /// @param cashoutAmount The amount in chips the player is leaving with.
    private static void cashOut(int playerIndex, double cashoutAmount) {
        bank = roundMoney(bank - cashoutAmount);
        players.remove(playerIndex);
        amounts.remove(playerIndex);
        types.remove(playerIndex);
        writeSimplePeople();
    } // cashOut

    /// Returns the String format of all the payments so far for the player.
    /// @param payTypes Individial payment methods String where each character is a different payment.
    /// @return An expanded list of all the payment types, separated by spaces.
    public static String payMethods(String payTypes) {
        if (payTypes.contains("e"))
            return "PAY-AT-END";
        if (payTypes.isEmpty())
            return "None";
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < payTypes.length(); i++) {
            char c = payTypes.charAt(i);
            if (c == 'c')
                ret.append("Cash");
            else if (c == 'z')
                ret.append("Zelle");
            else if (c == 'v')
                ret.append("Venmo");
            else
                ret.append("UNKNOWN");
            if (i + 1 < payTypes.length())
                ret.append(" ");
        }
        return ret.toString();
    } // payBy

    /// Queries for the type of payment method for a buyin, addon, or rebuy.
    /// @param name The name of the player
    /// @param endable Whether or not 'e' (PAY-AT-END) can be entered
    /// @param noneable Whether or not 'n' (None) can be entered
    /// @return String value of the payment method to append to types string.
    /// If the method is invalid, returns 0. If it's none, it returns empty.
    public static String payQuery(String name, boolean endable, boolean noneable) {
        System.out.printf("%s, how are you paying? (Cash, Venmo, Zelle%s%s) ",
                cap(name), endable ? ", End" : "", noneable ? ", None" : "");
        try {
            char c = s.nextLine().toLowerCase().charAt(0);
            if (c == 'c' || c == 'v' || c == 'z' || (endable && c == 'e'))
                return String.valueOf(c);
            else if (noneable && c == 'n')
                return "";
            else
                System.out.println("Invalid payment method!\n");
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Please enter something next time!\n");
        }
        return "0";
    } // paymentMethod

} // class
