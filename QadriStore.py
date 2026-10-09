import subprocess
import re
import requests
import os
import sys
import time
from datetime import datetime

# ==========================================
# CONFIGURATION
# ==========================================
FIREBASE_URL = "https://qadri-759f0-default-rtdb.firebaseio.com/website_url.json"
LOCAL_PORT = "8080"
SCRIPT_DIR = r"D:\Excel"  # Hardcoded path!

# ==========================================
# FORCE WORKING DIRECTORY (Most Important!)
# ==========================================
try:
    os.chdir(SCRIPT_DIR)
except Exception as e:
    print(f"Cannot change to {SCRIPT_DIR}: {e}")
    # Fallback: current file's directory
    SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
    os.chdir(SCRIPT_DIR)

# ==========================================
# LOGGING SETUP
# ==========================================
LOG_FILE = os.path.join(SCRIPT_DIR, "debug_log.txt")

def log_message(msg):
    """Log file aur console dono mein likho"""
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    log_line = f"[{timestamp}] {msg}"
    
    print(log_line)
    
    try:
        with open(LOG_FILE, 'a', encoding='utf-8') as f:
            f.write(log_line + '\n')
    except:
        pass

# ==========================================
# MAIN PROGRAM
# ==========================================
def main():
    log_message("="*60)
    log_message("🚀 CLOUDFLARE TUNNEL STARTING")
    log_message("="*60)
    log_message(f"Script Location: {os.path.abspath(__file__)}")
    log_message(f"Working Directory: {os.getcwd()}")
    log_message(f"Python Executable: {sys.executable}")
    log_message(f"Local Port: {LOCAL_PORT}")
    
    # Check if files exist
    log_message("\n📁 Checking files...")
    log_message(f"Script dir exists: {os.path.exists(SCRIPT_DIR)}")
    log_message(f"Log file will be: {LOG_FILE}")
    
    def update_firebase(url):
        try:
            log_message(f"\n📡 Updating Firebase with URL: {url}")
            
            headers = {'Content-Type': 'application/json'}
            response = requests.put(
                FIREBASE_URL, 
                data=f'"{url}"', 
                headers=headers,
                timeout=15
            )
            
            if response.status_code == 200:
                log_message("✅ SUCCESS! Firebase updated!")
                log_message(f"🔗 Live Link: {url}")
                log_message("⚙️ Tunnel is running...")
                
                # Success log in separate file
                success_file = os.path.join(SCRIPT_DIR, "last_success.txt")
                with open(success_file, 'w') as f:
                    f.write(f"URL: {url}\nTime: {datetime.now()}\n")
            else:
                log_message(f"❌ Firebase Error! Status: {response.status_code}")
                log_message(f"Response: {response.text}")
                
        except requests.exceptions.ConnectionError as e:
            log_message(f"❌ Network Error: No internet connection?")
            log_message(f"Details: {e}")
        except requests.exceptions.Timeout:
            log_message("❌ Timeout Error: Server not responding")
        except Exception as e:
            log_message(f"❌ Firebase Update Failed: {e}")
            import traceback
            log_message(traceback.format_exc())

    try:
        # Find cloudflared
        log_message("\n🔍 Looking for cloudflared...")
        
        cloudflared_name = "cloudflared.exe" if sys.platform == 'win32' else "cloudflared"
        
        # Possible locations
        possible_paths = [
            os.path.join(SCRIPT_DIR, cloudflared_name),
            r"C:\Program Files\cloudflared\cloudflared.exe",
            r"C:\Program Files (x86)\cloudflared\cloudflared.exe",
            cloudflared_name  # Try PATH
        ]
        
        cloudflared_path = None
        for path in possible_paths:
            log_message(f"   Checking: {path}")
            if os.path.exists(path):
                cloudflared_path = path
                break
        
        if cloudflared_path is None:
            # Check if it's in PATH
            log_message("   Checking system PATH...")
            result = subprocess.run(['where', cloudflared_name], 
                                  capture_output=True, text=True)
            if result.returncode == 0:
                cloudflared_path = cloudflared_name
                log_message(f"   Found in PATH: {result.stdout.strip()}")
        
        if cloudflared_path is None:
            log_message("❌ ERROR: cloudflared.exe NOT FOUND!")
            log_message("Please install cloudflared or place it in D:\\Excel\\")
            input("Press Enter to exit...")
            return
        
        log_message(f"✅ Using: {cloudflared_path}")
        
        # Build command
        command = [cloudflared_path, "tunnel", "--url", f"http://localhost:{LOCAL_PORT}"]
        log_message(f"\n📋 Command: {' '.join(command)}")
        
        # Start process
        log_message("\n⏳ Starting Cloudflare process...")
        
        # Windows-specific flags
        startupinfo = subprocess.STARTUPINFO()
        startupinfo.dwFlags |= subprocess.STARTF_USESHOWWINDOW
        
        process = subprocess.Popen(
            command,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            cwd=SCRIPT_DIR,  # CRITICAL!
            startupinfo=startupinfo
        )
        
        log_message(f"✅ Process Started! PID: {process.pid}")
        log_message("Waiting for tunnel URL...\n")
        
        # Pattern to find URL
        pattern = re.compile(r'https://[a-z0-9-]+\.trycloudflare\.com')
        link_found = False
        line_count = 0
        max_lines = 1000  # Safety limit
        
        for line in process.stdout:
            line_count += 1
            line_clean = line.strip()
            
            if line_clean:
                log_message(f"[CF] {line_clean}")
            
            # Look for URL
            if not link_found and line_count < max_lines:
                match = pattern.search(line)
                if match:
                    new_link = match.group(0)
                    log_message("\n" + "="*60)
                    log_message("🎉 TUNNEL URL FOUND!")
                    log_message("="*60)
                    update_firebase(new_link)
                    link_found = True
            
            # Safety: if too many lines and no URL
            if line_count >= max_lines and not link_found:
                log_message("\n⚠️ Warning: Read 1000 lines but no URL found")
                log_message("Tunnel might have failed to start")
        
        # Process ended
        return_code = process.wait()
        log_message(f"\n{'='*60}")
        log_message(f"Process ended. Return code: {return_code}")
        log_message(f"End time: {datetime.now()}")
        log_message('='*60)
        
        if return_code != 0:
            log_message("❌ Process exited with error!")
        
    except FileNotFoundError:
        log_message("❌ ERROR: Cannot execute cloudflared!")
        log_message(f"File not found: {cloudflared_path}")
        input("\nPress Enter to exit...")
        
    except PermissionError as e:
        log_message(f"❌ PERMISSION DENIED: {e}")
        log_message("Try running as Administrator")
        input("\nPress Enter to exit...")
        
    except Exception as e:
        log_message(f"❌ UNEXPECTED ERROR: {e}")
        import traceback
        log_message(traceback.format_exc())
        input("\nPress Enter to exit...")

# Run main function
if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        log_message("\n\n⛔ User interrupted (Ctrl+C)")
    except Exception as e:
        log_message(f"FATAL ERROR: {e}")
        import traceback
        log_message(traceback.format_exc())
        input("Press Enter to exit...")