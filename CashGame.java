import java.io.*;
import java.util.*;
import java.time.Instant;

/// A full-form Cash Game handler, designed for a full poker room.
/// Multiple tables and individual money on each table are included.
public class CashGame implements CompleteShorts {
    // database variables and lists
    private static double bank;
    private static ArrayList<String> players;
    private static ArrayList<Double> amounts;
    private static ArrayList<String> types;
    private static ArrayList<Integer> locations;

    private static ArrayList<String> tables;
    private static ArrayList<String> descriptions;
    private static ArrayList<Integer> tablePlayerCounts;
    private static ArrayList<Double> indiBanks;

    private static ArrayList<String> waitingList;
    private static ArrayList<String> infoWaiters;
    private static final Scanner s = new Scanner(System.in);

    public static void main(String[] args) {
        String inputString;
        String queuePlayer = "";
        String queueDescr = "";
        readPeople();
        readTables();
        readWaitList();

        for (;;) {
            // program loop
            if (!queuePlayer.isEmpty()) {
                inputString = "1";
            } else {
                System.out.printf("""
                        ----CASH----
                        1: New Player
                        2: Rebuy
                        3: Cashout
                        4: Print Names List
                        5: Add to %s
                        6: %s
                        7: Tables Menu*
                        8: Payment Type Change
                        9: Print Log
                        0: Quit
                        """, tables.isEmpty() ? "Reservations" : "Waitlist",
                        tables.isEmpty() ? "Remove from Reservations" : "Call from Waitlist");
                inputString = s.nextLine();
            }
            char inputChar = inputString.isEmpty() ? '0' : inputString.charAt(0);

            if (inputChar == '1') {
                // 1 add player

                if (tables.isEmpty()) {
                    System.out.println("There are no tables! Create one to add players!\n");
                    continue;
                }

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

                if (name.equals("dealertip") || name.equals("changebank")) {
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

                int loc = 0;
                if (tables.size() > 1) {
                    printTablesList();
                    System.out.printf("What table are you going to, %s? ", cap(name));
                    try {
                        loc = Integer.parseInt(s.nextLine());
                    } catch (NumberFormatException e) {
                        System.out.println(NFI);
                        continue;
                    }
                }
                if (loc >= tables.size() || loc < 0) {
                    System.out.println("Please enter a valid table number!");
                }
                players.add(name);
                amounts.add(buyin);
                types.add(method);
                locations.add(loc);
                bank = roundMoney(bank + buyin);
                tablePlayerCounts.set(loc, tablePlayerCounts.get(loc) + 1);
                indiBanks.set(loc, indiBanks.get(loc) + buyin);
                System.out.printf("%s bought in for $%.2f using %s at %s.\n\n",
                        cap(name), buyin, payMethods(method), tables.get(loc));
                queuePlayer = "";
                queueDescr = "";
                writePeople();
                writeTables();
                logC(String.format("BUYIN %s $%.2f %s %s", cap(name), buyin, payMethods(method), tables.get(loc)));
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

                int loc = locations.get(index);
                System.out.printf("%s is in for $%.2f at %s via %s.\n", cap(name), amounts.get(index), tables.get(loc),
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
                    System.out.printf("%s is adding on $%.2f at %s and paying at the end.\n",
                            cap(name), buyin, tables.get(loc));
                    System.out.print("Press enter to confirm and type anything to cancel. ");
                    if (s.nextLine().isEmpty()) {
                        System.out.println();
                        bank = roundMoney(bank + buyin);
                        amounts.set(index, roundMoney(amounts.get(index) + buyin));
                        indiBanks.set(loc, roundMoney(indiBanks.get(loc) + buyin));
                        writePeople();
                        writeTables();
                        logC(String.format("REBUY %s $%.2f %s", cap(name), buyin, tables.get(loc)));
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
                indiBanks.set(loc, roundMoney(indiBanks.get(loc) + buyin));
                System.out.printf("Rebuy confirmed. %s added on $%.2f via %s at %s.\n\n",
                        cap(name), buyin, payMethods(method), tables.get(locations.get(loc)));
                writePeople();
                writeTables();
                logC(String.format("REBUY %s $%.2f %s %s now $%.2f",
                        cap(name), buyin, payMethods(method), tables.get(loc), amounts.get(index)));
                // end 2 rebuy
            } else if (inputChar == '3') {
                // 3 cashout
                String name;
                System.out.println("----CASHOUT----");
                if (players.isEmpty()) {
                    System.out.println("There are no players, so cashout is dealertip.");
                    name = "dealertip";
                } else {
                    System.out.print("Enter name: ");
                    name = s.nextLine().toLowerCase();
                }
                if (name.equals("dealertip")) {
                    System.out.printf("There is currently $%.2f in the bank.\n", bank);
                    System.out.print("How much is the dealer receiving? $");
                    try {
                        double amount = roundMoney(Double.parseDouble(s.nextLine()));
                        if (amount == 0) {
                            System.out.println(CRECN);
                        } else {
                            int loc = 0;
                            if (tables.size() > 1) {
                                printTablesList();
                                System.out.print("Which table is the dealer coming from? ");
                                loc = Integer.parseInt(s.nextLine());
                            }

                            if (loc < 0 || loc >= tables.size()) {
                                System.out.println("Please enter a valid table number next time.\n");
                                continue;
                            }

                            System.out.printf("Paying $%.2f to dealer." + NEFIRMN, amount);
                            if (s.nextLine().isEmpty()) {
                                bank = roundMoney(bank - amount);
                                indiBanks.set(loc, roundMoney(indiBanks.get(loc) - amount));
                                writePeople();
                                writeTables();
                                logC(String.format("CASHOUT Dealer $%.2f %s", amount, tables.get(loc)));
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
                int loc = locations.get(index);

                System.out.printf("There is currently $%.2f in the bank and $%.2f in the %s bank.\n",
                        bank, indiBanks.get(loc), tables.get(loc));
                System.out.printf("%s bought in for $%.2f using %s at %s.\n",
                        cap(name), amounts.get(index), payMethods(types.get(index)), tables.get(locations.get(index)));
                System.out.printf("Host, how much is %s cashing out for? (0 for bust) $", cap(name));
                try {
                    double cashout = roundMoney(Double.parseDouble(s.nextLine()));

                    if (cashout == 0) {
                        if (types.get(index).equals("e")) {
                            System.out.printf("%s still owes $%.2f\n", cap(name), amounts.get(index));
                        }
                        System.out.printf("Thanks for playing, %s!" + NEFIRMS, cap(name));
                        if (!s.nextLine().isEmpty()) {
                            System.out.println(CRECN);
                            continue;
                        }
                        System.out.println();
                        tablePlayerCounts.set(loc, tablePlayerCounts.get(loc) - 1);
                        logC(String.format("CASHOUT %s $0.00 %s net $%.2f",
                                cap(name), tables.get(loc), -amounts.get(index)));
                        cashOut(index, 0);
                    } else if (!types.get(index).contains("e")) {
                        // pay up front
                        System.out.printf("Cashing out %s for $%.2f from %s. They bought in for $%.2f.",
                                cap(name), cashout, tables.get(locations.get(index)), amounts.get(index));
                        System.out.print(NEFIRMS);

                        if (!s.nextLine().isEmpty()) {
                            System.out.println(CRECN);
                            continue;
                        }

                        tablePlayerCounts.set(loc, tablePlayerCounts.get(loc) - 1);
                        indiBanks.set(loc, roundMoney(indiBanks.get(loc)) - cashout);
                        logC(String.format("CASHOUT %s $%.2f %s net $%.2f",
                                cap(name), cashout, tables.get(loc), cashout - amounts.get(index)));
                        cashOut(index, cashout);

                        System.out.printf("Transaction confirmed. Thanks for playing, %s!\n\n", cap(name));
                    } else {
                        // pay at end
                        System.out.printf("Cashing out %s for $%.2f. They still owe $%.2f. Net profit is $%+.2f.",
                                cap(name), cashout, amounts.get(index), roundMoney(cashout - amounts.get(index)));
                        for (int i = 0; i < types.get(index).length(); i++) {
                            System.out.print(" " + payMethods(types.get(index).substring(i, i + 1)));
                        }
                        System.out.print("." + NEFIRMS);

                        if (!s.nextLine().isEmpty()) {
                            System.out.println(CRECN);
                            continue;
                        }
                        tablePlayerCounts.set(loc, tablePlayerCounts.get(loc) - 1);
                        indiBanks.set(loc, roundMoney(indiBanks.get(loc)) - cashout);
                        logC(String.format("CASHOUT %s $%.2f %s net $%.2f",
                                cap(name), cashout, tables.get(loc), cashout - amounts.get(index)));
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
                        System.out.printf(" | %s\n", tables.get(locations.get(i)));
                    }
                }

                if (!waitingList.isEmpty()) {
                    System.out.printf("\n==== %s (%d) ====\n",
                            tables.isEmpty() ? "RESERVATIONS" : "WAITING LIST", waitingList.size());
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
                    logC("WAITLIST ADD " + cap(name));
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

                System.out.printf("---- CALL FROM %s ----\n1 to %s\n2 to move to back of line%s\n",
                        tables.isEmpty() ? "RESERVATIONS" : "WAITLIST",
                        tables.isEmpty() ? "remove from list" : "call player",
                        tables.isEmpty() ? "" : "\n3 to remove from list");

                for (int i = 0; i <= waitingList.size(); i++) {
                    if (waitingList.size() == i) {
                        System.out.println("Seat remains open.\n");
                        break;
                    }
                    System.out.print(cap(waitingList.get(i)) + "  ");
                    String input = s.nextLine();
                    if (input.equals("1")) {
                        // call player
                        System.out.printf("Removed %s from %slist.\n\n", cap(waitingList.get(i)),
                                tables.isEmpty() ? "reservations " : "wait");
                        queuePlayer = tables.isEmpty() ? "" : waitingList.get(i);
                        queueDescr = tables.isEmpty() ? "" : infoWaiters.get(i);
                        logC("WAITLIST REMOVE " + cap(waitingList.get(i)));
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
                    } else if (!tables.isEmpty() && input.equals("3")) {
                        System.out.printf("Removed %s from waitlist.\n\n", cap(waitingList.get(i)));
                        waitingList.remove(i);
                        infoWaiters.remove(i);
                        writeWaitList();
                        break;
                    }
                }
                // end 6 call from waiting list
            } else if (inputChar == '7') {
                // 7 tables menu

                boolean confirm = false;
                char secondChar = 0;
                if (inputString.length() > 1) {
                    secondChar = inputString.charAt(1);
                    if (secondChar >= '1' && secondChar <= '6')
                        confirm = true;
                }
                if (!confirm) {
                    System.out.println("""
                            ****TABLES MENU****
                            1: Create New Table
                            2: Delete Empty Table
                            3: Change Table Information
                            4: Print Tables List
                            5: Check Table Contents
                            6: Player Table Change
                            0: Exit""");
                    try {
                        secondChar = s.nextLine().charAt(0);
                    } catch (StringIndexOutOfBoundsException e) {
                        System.out.println("Please enter something!\n");
                        continue;
                    }
                }

                if (secondChar == '1') {
                    // 71 create new table
                    System.out.print("---- CREATE NEW TABLE ----\nEnter new Table name: ");
                    String name = s.nextLine();
                    if (name.contains(";")) {
                        System.out.println("Table name cannot contain a semicolon!\n");
                        continue;
                    } else if (tableCheck(name)) {
                        System.out.println("Table name already exists!\n");
                        continue;
                    }

                    System.out.print("Enter table description: ");
                    String description = s.nextLine();
                    description = description.isEmpty() ? name : description;

                    System.out.printf("Creating table %s (%s). Press enter to confirm and type anything to cancel. ",
                            name, description);
                    if (s.nextLine().isEmpty()) {
                        tables.add(name);
                        descriptions.add(description);
                        tablePlayerCounts.add(0);
                        indiBanks.add(0d);
                        System.out.printf("New Table created! Table %s is index %d.\n\n", name, tables.size() - 1);
                        writeTables();
                        logC("TABLE CREATE " + name);
                    } else {
                        System.out.println(CRECN);
                    }
                    // end 71 create new table
                } else if (secondChar == '2') {
                    // 72 delete empty table
                    if (tables.isEmpty() || !tablePlayerCounts.contains(0)) {
                        System.out.println("There are no empty tables to delete!\n");
                        continue;
                    }
                    System.out.println("---- DELETE TABLE ----");
                    for (int i = 0; i < tables.size(); i++) {
                        if (tablePlayerCounts.get(i) == 0)
                            System.out.printf("%d: %s (%s)\n", i, tables.get(i), descriptions.get(i));
                    }
                    try {
                        System.out.print("Enter the index of the Table to delete: ");
                        int index = Integer.parseInt(s.nextLine());

                        if (index < 0 || index >= tables.size() || tablePlayerCounts.get(index) != 0) {
                            System.out.println("Invalid index provided!\n");
                            continue;
                        }

                        System.out.printf("Deleting table %s (%s). Press enter to confirm and type anything to cancel.",
                                tables.get(index), descriptions.get(index));

                        if (s.nextLine().isEmpty()) {
                            System.out.printf("Deleted table %s (%s).\n\n", tables.get(index), descriptions.get(index));
                            logC("TABLE DELETE " + tables.get(index));
                            tables.remove(index);
                            descriptions.remove(index);
                            tablePlayerCounts.remove(index);
                            indiBanks.remove(index);
                            for (int i = 0; i < locations.size(); i++) {
                                if (locations.get(i) > index)
                                    locations.set(i, locations.get(i) - 1);
                            }
                            writePeople();
                            writeTables();
                        } else {
                            System.out.println(CRECN);
                        }
                    } catch (NumberFormatException e) {
                        System.out.println(NFI);
                    }
                    // end 72 delete empty table
                } else if (secondChar == '3') {
                    // 73 change table information
                    int change = 0;
                    if (tables.isEmpty()) {
                        System.out.println("There are no tables!\n");
                        continue;
                    } else if (tables.size() > 1) {
                        printTablesList();
                        System.out.print("Which table information would you like to change? ");
                        try {
                            change = Integer.parseInt(s.nextLine());
                            if (change < 0 || change >= tables.size()) {
                                System.out.println("Please enter a valid table number next time.\n");
                                continue;
                            }
                        } catch (NumberFormatException e) {
                            System.out.println(NFI);
                            continue;
                        }
                    }
                    System.out.printf("%d: %s\nDescription: %s\nWhat would you like to change the name to?\n",
                            change, tables.get(change), descriptions.get(change));
                    String newName = s.nextLine();
                    System.out.println("What would you like to change the description to?");
                    String newDesc = s.nextLine();

                    writeTables();
                    System.out.printf("%s [Table %d] now is described as: %s%s\n",
                            tables.get(change), change, descriptions.get(change), NECON);
                    if (s.nextLine().isEmpty()) {
                        logC(String.format("TABLE DESCRIPTION %s (%s) -> %s (%s)", tables.get(change),
                                descriptions.get(change), newName.isEmpty() ? tables.get(change) : newName,
                                newDesc.isEmpty() ? descriptions.get(change) : newDesc));
                        if (!newName.isEmpty())
                            tables.set(change, newName);
                        if (!newDesc.isEmpty())
                            descriptions.set(change, newDesc);
                        System.out.println("Table information change complete.\n");
                    } else {
                        System.out.println(CRECN);
                    }
                    // end 73 change table information
                } else if (secondChar == '4') {
                    // 74 print tables list
                    if (tables.isEmpty()) {
                        System.out.println("There are no tables!\n");
                        continue;
                    }
                    System.out.println("==== LIST OF TABLES ====");
                    for (int i = 0; i < tables.size(); i++) {
                        System.out.printf("%d: %s [%dP] %s [$%.2f]\n", i, tables.get(i), tablePlayerCounts.get(i),
                                descriptions.get(i), indiBanks.get(i));
                    }
                    System.out.println();
                    // end 74 print tables list
                } else if (secondChar == '5') {
                    // 75 check table contents
                    int check = 0;
                    if (tables.isEmpty()) {
                        System.out.println("There are no tables!\n");
                        continue;
                    } else if (tables.size() > 1) {
                        printTablesList();
                        System.out.print("Which table would you like to check the contents of? ");
                        try {
                            check = Integer.parseInt(s.nextLine());
                            if (check < 0 || check >= tables.size()) {
                                System.out.println("Please enter a valid table number next time!\n\n");
                                continue;
                            }
                        } catch (NumberFormatException e) {
                            System.out.println(NFI);
                            continue;
                        }
                    }
                    if (tablePlayerCounts.get(check) == 0) {
                        System.out.printf("\n==== TABLE %d IS EMPTY! ====\nName: %s\nDesc: %s\n\n",
                                check, tables.get(check), descriptions.get(check));
                        continue;
                    }
                    System.out.printf("""
                                    \n==== TABLE %d ====
                                    %s - %s
                                    %d Players
                                    Table Bank: $%.2f
                                    Average Stack: $%.2f
                                    
                                    """, check, tables.get(check), descriptions.get(check),
                            tablePlayerCounts.get(check), indiBanks.get(check),
                            indiBanks.get(check) / tablePlayerCounts.get(check));
                    for (int i = 0; i < players.size(); i++) {
                        if (locations.get(i) == check)
                            System.out.printf("%s $%.2f\n", cap(players.get(i)), roundMoney(amounts.get(i)));
                    }
                    System.out.println(NECON);
                    s.nextLine();
                    // end 75 check table contents
                } else if (secondChar == '6') {
                    // 76 change tables
                    System.out.print("Enter name: ");
                    String name = s.nextLine().toLowerCase();

                    if (!nameCheck(name) || waitingList.contains(name)) {
                        System.out.printf("Name %s not found in database!\n\n", cap(name));
                        continue;
                    }

                    int index = players.indexOf(name);
                    int oldLoc = locations.get(index);

                    printTablesList();
                    System.out.printf("%s is currently at %s. Which table are they moving to?\n",
                            cap(name), tables.get(oldLoc));
                    int newLoc;
                    try {
                        newLoc = Integer.parseInt(s.nextLine());
                    } catch (NumberFormatException e) {
                        System.out.println(NFI);
                        continue;
                    }

                    if (newLoc < 0 || newLoc >= tables.size()) {
                        System.out.println("Cannot change to a table that doesn't exist!\n");
                        continue;
                    } else if (newLoc == oldLoc) {
                        System.out.println("Please actually change tables!\n");
                        continue;
                    }
                    double oldStack, newStack;
                    try {
                        System.out.printf("How much does %s have in their stack right now? $", cap(name));
                        oldStack = roundMoney(Double.parseDouble(s.nextLine()));
                        System.out.printf("How much will %s have at %s? $", cap(name), tables.get(newLoc));
                        newStack = roundMoney(Double.parseDouble(s.nextLine()));
                    } catch (NumberFormatException e) {
                        System.out.println(NFI);
                        continue;
                    }
                    double diff = roundMoney(oldStack - newStack);
                    String method = "";

                    if (diff > 0) {
                        // forced cashout
                        System.out.printf("%s, the bank must cash $%.2f out of your stack.\n", cap(name), diff);
                        double newBuyin = roundMoney(amounts.get(index) - diff);
                        if (types.get(index).equals("e")) {
                            System.out.printf("%s is PAY-AT-END. Changing their buyin value from $%.2f to $%.2f.",
                                    cap(name), amounts.get(index), newBuyin);
                            System.out.print(NEFIRMS);
                            if (s.nextLine().isEmpty()) {
                                amounts.set(index, newBuyin);
                            } else {
                                System.out.println(CRECN);
                                continue;
                            }
                        } else {
                            System.out.printf("%s bought in via %s. " + EFIRMS,
                                    cap(name), payMethods(types.get(index)));
                            if (s.nextLine().isEmpty())
                                amounts.set(index, amounts.get(index) - diff);
                            else {
                                System.out.println(CRECN);
                                continue;
                            }
                        }
                        bank = roundMoney(bank - diff);
                    } else if (diff < 0) {
                        // must add on
                        double addOn = -diff;
                        System.out.printf("%s, the bank must add $%.2f onto your stack.\n", cap(name), addOn);
                        double newBuyin = roundMoney(amounts.get(index) + addOn);

                        if (!types.get(index).equals("e")) {
                            // not pay at end
                            method = payQuery(name, false, true);
                            if (method.equals("0"))
                                continue;
                            System.out.printf("%s is adding $%.2f onto their stack that will be $%.2f via %s.",
                                    cap(name), addOn, newStack, payMethods(method));
                            System.out.print(NEFIRMS);
                            if (s.nextLine().isEmpty()) {
                                amounts.set(index, newBuyin);
                                types.set(index, types.get(index) + method);
                            } else {
                                System.out.println(CRECN);
                                continue;
                            }
                        } else {
                            // pay at end
                            System.out.printf("%s is PAY-AT-END. Changing their buyin value from $%.2f to $%.2f.",
                                    cap(name), amounts.get(index), newBuyin);
                            System.out.print(NEFIRMS);
                            if (s.nextLine().isEmpty()) {
                                amounts.set(index, newBuyin);
                            } else {
                                System.out.println(CRECN);
                                continue;
                            }
                        }
                        bank = roundMoney(bank + addOn);
                    } else {
                        // same stack
                        System.out.printf("Moving %s from %s to %s. " + EFIRMS,
                                cap(name), tables.get(oldLoc), tables.get(newLoc));
                        if (!s.nextLine().isEmpty()) {
                            System.out.println(CRECN);
                            continue;
                        }
                    }

                    logC(String.format("TABLE CHANGE %s $%.2f %s -%s> $%.2f %s",
                            cap(name), oldStack, tables.get(oldLoc), diff < 0 && !method.isEmpty() ?
                                    "--" + method + "--" : "", newStack, tables.get(newLoc)));
                    locations.set(index, newLoc);
                    tablePlayerCounts.set(oldLoc, tablePlayerCounts.get(oldLoc) - 1);
                    tablePlayerCounts.set(newLoc, tablePlayerCounts.get(newLoc) + 1);
                    indiBanks.set(oldLoc, roundMoney(indiBanks.get(oldLoc) - oldStack));
                    indiBanks.set(newLoc, roundMoney(indiBanks.get(newLoc) + newStack));
                    writePeople();
                    writeTables();
                    System.out.printf("Change confirmed. %s moved from %s to %s and their stack %s by $%.2f.\n\n",
                            cap(name), tables.get(oldLoc), tables.get(newLoc),
                            diff < 0 ? "increased" : "decreased", Math.abs(diff));
                    // 76 end change tables
                }
                // end 7 tables menu
            } else if (inputChar == '8') {
                // 8 payment type change
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
                System.out.printf("%s is playing at %s via %s.\n", cap(name), tables.get(index), types.get(index));
                StringBuilder newTypes = new StringBuilder();
                System.out.print("Enter types now, one by one. (Cash, Venmo, Zelle, End) When done, press enter. ");
                String line;
                char c;
                line = s.nextLine().toLowerCase();
                if (line.isEmpty()) {
                    System.out.println("Please enter something next time!\n");
                    continue;
                }
                switch (line.charAt(0)) {
                    case 'e':
                        System.out.printf("%s is now paying at the end." + EFIRMS, cap(name));
                        if (s.nextLine().isEmpty()) {
                            logC(String.format("TYPE CHANGE %s %s -> %s", cap(name), types.get(index), newTypes));
                            types.set(index, "e");
                            writePeople();
                            System.out.println("Confirmed. Thy has been done.");
                        } else { System.out.println(CRECN); continue; }
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
                logC(String.format("TYPE CHANGE %s %s -> %s", cap(name), types.get(index), newTypes));
                types.set(index, newTypes.toString());
                writePeople();
                System.out.println("Confirmed. Thy has been done.\n");
                // end 8 payment type change
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
                    tables = new ArrayList<>();
                    descriptions = new ArrayList<>();
                    tablePlayerCounts = new ArrayList<>();
                    indiBanks = new ArrayList<>();
                    waitingList = new ArrayList<>();
                    queuePlayer = "";
                    writePeople();
                    writeWaitList();
                    writeTables();
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
                            logC(String.format("BANK $%.2f -> $%.2f", bank, roundMoney(Double.parseDouble(line))));
                            bank = roundMoney(Double.parseDouble(line));
                            System.out.printf("Bank value changed to $%.2f.\n\n", bank);
                            writePeople();
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
    public static void writePeople() {
        File f = new File(PEOPLE_FILE);
        try (PrintWriter pw = new PrintWriter(f)) {
            pw.println(bank);
            for (int i = 0; i < players.size(); i++) {
                pw.println(String.format("%s;%.2f;%s;%d",
                        players.get(i), amounts.get(i), types.get(i), locations.get(i)));
            }
        } catch (IOException e) {
            System.out.println("Error writing to people file.");
        }
    } // write

    /// Reads the info from the people file and updates the 5 ArrayLists.
    /// (database, players, amounts, types, locations)
    public static void readPeople() {
        players = new ArrayList<>();
        amounts = new ArrayList<>();
        types = new ArrayList<>();
        locations = new ArrayList<>();
        File f = new File(PEOPLE_FILE);
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
                    locations.add(Integer.parseInt(split[3]));
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
            writePeople();
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

    /// Writes the contents of the 4 table ArrayLists to the tables file.
    /// (tables, descriptions, tablePlayerCounts, indiBanks)
    public static void writeTables() {
        File f = new File(TABLES_LIST_FILE);
        try (PrintWriter pw = new PrintWriter(f)) {
            for (int i = 0; i < tables.size(); i++) {

                pw.println(String.format("%s;%s;%d;%.2f", tables.get(i), descriptions.get(i),
                        tablePlayerCounts.get(i), indiBanks.get(i)));
            }
        } catch (IOException e) {
            System.out.println("Error writing to tables file.");
        }
    } // writeTables

    /// Reads the info from the tables file and updates the 4 ArrayLists.
    /// (tables, descriptions, tablePlayerCounts, indiBanks)
    public static void readTables() {
        tables = new ArrayList<>();
        descriptions = new ArrayList<>();
        tablePlayerCounts = new ArrayList<>();
        indiBanks = new ArrayList<>();
        File f = new File(TABLES_LIST_FILE);
        if (f.exists()) {
            // file already exists
            try (BufferedReader bfr = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = bfr.readLine()) != null) {
                    String[] split = line.split(";");
                    tables.add(split[0]);
                    descriptions.add(split[1]);
                    tablePlayerCounts.add(Integer.parseInt(split[2]));
                    indiBanks.add(roundMoney(Double.parseDouble(split[3])));
                }
            } catch (IOException | NumberFormatException e) {
                System.out.println("Error reading from tables file.");
            }
        } else {
            // need to create a new file
            try {
                boolean b = f.createNewFile();
            } catch (IOException e) {
                System.out.println("Error creating new tables file. Try renaming the LOCAL_PATH variable!");
            }
        }
    } // readTables

    /// Prints a list of the tables in play.
    public static void printTablesList() {
        System.out.printf("==== LIST OF TABLES (%d) ====\n", tables.size());
        for (int i = 0; i < tables.size(); i++) {
            System.out.printf("%d: %s - %s\n", i, tables.get(i), descriptions.get(i));
        }
    } // printTablesList

    /// Logs the string value with a timestamp and cash game watermark to the log file.
    /// @param text The string value to be added to the log.
    public static void logC(String text) {
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
            pw.println("[" + Instant.now().toString().substring(0, 19).replaceFirst("T", " @ ") + "Z] {C} " + text);
        } catch (IOException e) {
            System.out.println("Error writing to log file.");
        }
    } // logC

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

    /// Returns a boolean for whether the table exists in the tables list or not.
    /// @param tableName The name of the table to check.
    /// @return Boolean whether the table exists or not.
    private static boolean tableCheck(String tableName) {
        for (String el : tables) {
            if (tableName.equalsIgnoreCase(el))
                return true;
        }
        return false;
    } // tableCheck

    /// Cashes the player out and removes them from the database.
    /// @param playerIndex The index of the player in the database.
    /// @param cashoutAmount The amount in chips the player is leaving with.
    private static void cashOut(int playerIndex, double cashoutAmount) {
        bank = roundMoney(bank - cashoutAmount);
        players.remove(playerIndex);
        amounts.remove(playerIndex);
        types.remove(playerIndex);
        locations.remove(playerIndex);
        writePeople();
        writeTables();
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
