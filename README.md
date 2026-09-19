# Safe Chip Banking
Safe Chip Banking is a CLI program that allows a host to bank a poker game.

## Getting Started
### Requirements
- Java compiler (javac is acceptable; any IDE is recommended for file editing)
- File editor (optional) - for creating txt rules files and
changing which rules file to display

### Installing
Make sure all files are in one directory. The program will automatically check
for path to that directory, and it will not work without all files being together.

### Execution
**IMPORTANT** Change the values of SECURITY_QUESTION, SECURITY_ANSWER, and ADMIN_NAME in SecurityCheck.java.
The program comes with default values; any non-empty string value can replace those.
- CashGame.java and SimpleCashGame.java do not require any extra instructions to run on their own.
- Before running Tournament.java, ensure the tournament rules file in EditMeForFiles.java is the desired rules file.
Also, you can remove the "/src" tag or change it to fit your specific directory location.
- Also ensure that the tournament rules file is in the src folder and not the bin/out folder.
- I recommend always running Combo.java just to be safe. It's easy to switch between modes that way.
- **NOTE** Entering 0 is not the only way to exit from a program. If a non-number is entered,
the program will terminate automatically. (unless given factoryreset or changebank)

### Data
All activity is automatically saved to the specified txt files in the folder.

*DO NOT DELETE THE FOLLOWING FILES:*
  - listwait.txt : waitlist (or reservations) for CashGame and SimpleCashGame
  - log.txt : all actions performed since last reset for all types
  - people.txt : player information for CashGame
  - sidegames.txt : side game information for Tournament
  - simplepeople.txt : player information for SimpleCashGame
  - tables.txt : table information in CashGame
  - tournament.txt : player information in Tournament

## Help
- If something isn't adding up, first check the log (9 to print log on menus).

**HOW TO READ THE LOG**

\[DATE Y-M-D @ TIME IN ZULU/UTC] {X} \<ACTION> \<description>

X is replaced with a letter based on which division logged an item:
C for CashGame, S for SimpleCashGame, T for Tournament.

- Manual value changes can be done in the actual files, and a limited number of edits can be done in command line.
- To completely wipe all data saved across all documents, use the following:
```
factoryreset
<SECURITY_ANSWER>
<ADMIN_NAME_IN_ALL_CAPS>
```
- To edit the value of the main bank, use the following:
```
changebank
<SECURITY_ANSWER>
<VALUE>
```

## Author
Shawn Mastandrea

## Version History
- v1 : September 2026
  - Initial GitHub release
- v0 : April 2026
  - Initial testing and beta usage

## License
This project is licensed under an MIT license - see the LICENSE.md file for details
