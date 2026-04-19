import re
with open("app/src/main/java/com/example/androidsignalswifi/MainActivity.kt", "r", encoding="utf-8") as f:
    content = f.read()

content = content.replace("List APs", "List Devices")

with open("app/src/main/java/com/example/androidsignalswifi/MainActivity.kt", "w", encoding="utf-8") as f:
    f.write(content)
