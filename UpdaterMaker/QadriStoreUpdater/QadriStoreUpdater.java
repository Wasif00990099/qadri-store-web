import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class QadriStoreUpdater {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Invalid arguments!");
            return;
        }

        String currentExePath = args[0]; // Jo EXE abhi chal rahi thi
        String newExePath = args[1];     // Jo download hui hai

        File currentExe = new File(currentExePath);
        File newExe = new File(newExePath);

        try {
            System.out.println("Updater started. Waiting for main app to close...");
            // 2 second wait karna taake main software poori tarah band ho jaye
            Thread.sleep(2000); 
            
            // Purani EXE ko delete karna
            boolean deleted = false;
            for (int i = 0; i < 10; i++) { // 10 baar try karega
                if (currentExe.exists()) {
                    deleted = currentExe.delete();
                    if (deleted) break;
                    Thread.sleep(1000); 
                } else {
                    deleted = true;
                    break;
                }
            }

            if (!deleted) {
                System.out.println("Failed to delete old EXE!");
                return;
            }

            // Nayi EXE ko uski jagah move karna (Replace)
            Files.move(newExe.toPath(), currentExe.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Update replaced successfully!");

            // Nayi EXE ko run karna
            ProcessBuilder pb = new ProcessBuilder(currentExe.getAbsolutePath());
            pb.start();
            System.out.println("New version launched!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}