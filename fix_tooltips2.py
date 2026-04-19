import re
with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "r") as f:
    content = f.read()

content = content.replace("mapView.post { circle.showInfoWindow() }", "circle.showInfoWindow()")
content = content.replace("mapView.post { marker.showInfoWindow() }", "marker.showInfoWindow()")

with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "w") as f:
    f.write(content)
