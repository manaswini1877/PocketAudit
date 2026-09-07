import urllib.request
import zipfile
import io
import os

url = "https://services.gradle.org/distributions/gradle-8.5-bin.zip"
zip_path = "gradle-8.5-bin.zip"

print("Downloading Gradle 8.5 from services.gradle.org...")
headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}
req = urllib.request.Request(url, headers=headers)

try:
    with urllib.request.urlopen(req, timeout=120) as resp, open(zip_path, 'wb') as out_file:
        chunk_size = 1024 * 1024
        total = 0
        while True:
            chunk = resp.read(chunk_size)
            if not chunk:
                break
            out_file.write(chunk)
            total += len(chunk)
            if int(total / (1024*1024)) % 10 == 0:
                print(f"Downloaded {total / (1024*1024):.1f} MB...")

    print("Download complete. Extracting gradle-wrapper.jar...")
    with zipfile.ZipFile(zip_path) as z:
        found = False
        for name in z.namelist():
            if name.endswith("gradle-wrapper.jar") or (name.endswith(".jar") and "wrapper" in name):
                data = z.read(name)
                # check if jar has Main-Class in manifest
                try:
                    with zipfile.ZipFile(io.BytesIO(data)) as j:
                        if 'META-INF/MANIFEST.MF' in j.namelist():
                            manifest = j.read('META-INF/MANIFEST.MF').decode('utf-8', errors='ignore')
                            if 'Main-Class' in manifest:
                                os.makedirs("gradle/wrapper", exist_ok=True)
                                with open("gradle/wrapper/gradle-wrapper.jar", "wb") as f:
                                    f.write(data)
                                print(f"EXTRACTED VALID gradle-wrapper.jar ({len(data)} bytes) from {name}")
                                found = True
                                break
                except Exception as e:
                    pass
        if not found:
            print("No valid gradle-wrapper.jar found in zip!")
except Exception as e:
    print("Error during download/extraction:", e)
finally:
    if os.path.exists(zip_path):
        os.remove(zip_path)
