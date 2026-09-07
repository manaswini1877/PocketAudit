import urllib.request
import zipfile
import io
import os
import sys

url = "https://services.gradle.org/distributions/gradle-8.5-bin.zip"
print("Downloading Gradle 8.5 distribution zip...")

headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}
req = urllib.request.Request(url, headers=headers)

try:
    with urllib.request.urlopen(req, timeout=60) as resp:
        zip_bytes = resp.read()
    print(f"Downloaded {len(zip_bytes)} bytes of Gradle distribution.")
    
    with zipfile.ZipFile(io.BytesIO(zip_bytes)) as z:
        for name in z.namelist():
            if name.endswith("gradle-wrapper-8.5.jar"):
                data = z.read(name)
                try:
                    with zipfile.ZipFile(io.BytesIO(data)) as j:
                        if 'META-INF/MANIFEST.MF' in j.namelist():
                            manifest = j.read('META-INF/MANIFEST.MF').decode('utf-8', errors='ignore')
                            if 'Main-Class' in manifest:
                                os.makedirs("gradle/wrapper", exist_ok=True)
                                with open("gradle/wrapper/gradle-wrapper.jar", "wb") as f:
                                    f.write(data)
                                print(f"Successfully extracted valid gradle-wrapper.jar from {name} ({len(data)} bytes)!")
                                sys.exit(0)
                except Exception as e:
                    pass
    print("Could not find Main-Class gradle-wrapper.jar in zip!")
except Exception as e:
    print("Error:", e)
    sys.exit(1)
