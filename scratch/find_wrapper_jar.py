import urllib.request
import zipfile
import io
import os

url = "https://services.gradle.org/distributions/gradle-8.5-bin.zip"

print("Downloading Gradle 8.5...")
headers = {'User-Agent': 'Mozilla/5.0'}
req = urllib.request.Request(url, headers=headers)
with urllib.request.urlopen(req) as resp:
    zip_bytes = resp.read()

print("Searching inside zip for GradleWrapperMain...")
with zipfile.ZipFile(io.BytesIO(zip_bytes)) as z:
    for name in z.namelist():
        if name.endswith(".jar"):
            try:
                data = z.read(name)
                with zipfile.ZipFile(io.BytesIO(data)) as j:
                    if 'META-INF/MANIFEST.MF' in j.namelist():
                        manifest = j.read('META-INF/MANIFEST.MF').decode('utf-8', errors='ignore')
                        if 'GradleWrapperMain' in manifest:
                            print(f"FOUND MATCH: {name}\nManifest:\n{manifest}\n---")
                            os.makedirs("gradle/wrapper", exist_ok=True)
                            with open("gradle/wrapper/gradle-wrapper.jar", "wb") as f:
                                f.write(data)
                            print(f"SAVED REAL WRAPPER JAR ({len(data)} bytes) to gradle/wrapper/gradle-wrapper.jar!")
                            break
            except Exception as e:
                pass
