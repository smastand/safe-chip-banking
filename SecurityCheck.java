import java.util.Scanner;

/// Security checking methods for large-scale backend actions,
/// such as resetting to an empty state game and changing bank values.
public class SecurityCheck implements CompleteShorts {
    private static final String SECURITY_QUESTION = "Enter your question here.";
    private static final String SECURITY_ANSWER = "ANSWER";
    public static final String ADMIN_NAME = "Admin";

    /// Performs the actual password check.
    /// @return Boolean for whether the password matches the security answer, case-sensitive.
    public static boolean securityCheck() {
        Scanner s = new Scanner(System.in);
        System.out.printf("[Admin] Hello, %s. %s\n", ADMIN_NAME, SECURITY_QUESTION);
        return s.nextLine().equals(SECURITY_ANSWER);
    } // securityCheck

} // class
