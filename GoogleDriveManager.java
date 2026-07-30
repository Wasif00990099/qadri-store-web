import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GoogleDriveManager {
    
    // Yahan apni Google API Key daalein
    private static final String API_KEY = "AIzaSyBhTPG7iTSokeMhFTv3a4iW81DsP78vpBQ";
    
    // Yahan apni Google Drive ki File ID daalein (Pehli baar jab aap drive par file upload karenge toh uski ID nikal lein)
    // File ID link ke aakhir mein hoti hai: drive.google.com/file/d/FILE_ID_YAHAN_HOGI/view
    private static final String FILE_ID = "https://drive.google.com/file/d/1JqyVjNB7EGRPXkWySekXBow_fxiPHO3b/view?usp=sharing"; 
    
    private static final String LOCAL_FILE = "QadriData.csv";

    // --- UPLOAD FUNCTION (Main PC se data Google Drive par bhejne ke liye) ---
    public static void uploadDataFile() {
        try {
            File file = new File(LOCAL_FILE);
            if (!file.exists()) return;

            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            URL url = new URL("https://www.googleapis.com/upload/drive/v3/files/" + FILE_ID + "?uploadType=multipart&key=" + API_KEY);
            
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PATCH"); // PATCH isliye use kiya taake purani file overwrite ho, nayi file na bane
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/related; boundary=" + boundary);

            try (OutputStream os = conn.getOutputStream()) {
                // Metadata
                os.write(("--" + boundary + "\r\n").getBytes());
                os.write("Content-Type: application/json; charset=UTF-8\r\n\r\n".getBytes());
                os.write("{\"name\": \"QadriData.csv\"}\r\n".getBytes());
                
                // File Data
                os.write(("--" + boundary + "\r\n").getBytes());
                os.write("Content-Type: text/csv\r\n\r\n".getBytes());
                Files.copy(file.toPath(), os);
                
                os.write(("\r\n--" + boundary + "--\r\n").getBytes());
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                System.out.println("Data Google Drive par upload ho gaya!");
            } else {
                System.out.println("Upload fail hua. Code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- DOWNLOAD FUNCTION (Dusre PC se data uthane ke liye) ---
    public static File downloadDataFile() {
        try {
            URL url = new URL("https://www.googleapis.com/drive/v3/files/" + FILE_ID + "?alt=media&key=" + API_KEY);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                File localFile = new File(LOCAL_FILE);
                try (InputStream is = conn.getInputStream(); FileOutputStream fos = new FileOutputStream(localFile)) {
                    is.transferTo(fos);
                }
                System.out.println("Data Google Drive se download ho gaya!");
                return localFile;
            } else {
                System.out.println("Download fail hua. Code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}