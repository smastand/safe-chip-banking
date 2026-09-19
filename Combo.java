import java.util.Scanner;

/// A main menu that combines all games' abilities into one.
/// All three versions can be run simultaneously through this class.
public class Combo implements CompleteShorts {
    public static void main(String[] args) {
        Scanner s = new Scanner(System.in);
        for(;;) {
            System.out.println("""
                    ----MAIN MENU----
                    1: Cash Game
                    2: Tournament
                    3: Simple Cash Game
                    0: Exit""");
            String input = s.nextLine();
            if (input.contains("1")) {
                CashGame.main(new String[]{});
            } else if (input.contains("2")) {
                Tournament.main(new String[]{});
            } else if (input.contains("3")) {
                SimpleCashGame.main(new String[]{});
            } else {
                System.out.println("Goodbye!");
                break;
            }
        } // for
    } // main
} // class
