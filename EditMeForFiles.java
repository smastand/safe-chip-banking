import java.io.File;

/// Interface used to locate files in a specified directory.
/// Change the values of TOURNAMENT_FILE_NAME
/// and the lookup of LOCAL_PATH_TO_FILE before starting the program.
public interface EditMeForFiles {
    // Change this value to edit the tournament rules file name
    String TOURNAMENT_FILE_NAME = "demo_rules.txt";

    File fff = new File(TOURNAMENT_FILE_NAME);

    // Change this value to
    String LOCAL_PATH_TO_FILE =
            fff.getAbsolutePath().substring(0, fff.getAbsolutePath().length() - TOURNAMENT_FILE_NAME.length()) + "src/";
} // intf
