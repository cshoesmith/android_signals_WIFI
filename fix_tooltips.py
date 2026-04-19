import re
with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "r") as f:
    content = f.read()

# WiFi Tooltip Fix
old_wifi_tt = """                mapView.overlays.add(circle)
                if (openTooltipId == identifier) mapView.post { circle.showInfoWindow() }

                if (primaryAp.totalWeight > 200.0) {"""
new_wifi_tt = """                mapView.overlays.add(circle)
                if (openTooltipId == identifier && primaryAp.totalWeight <= 200.0) mapView.post { circle.showInfoWindow() }

                if (primaryAp.totalWeight > 200.0) {"""
content = content.replace(old_wifi_tt, new_wifi_tt)

# BLE Tooltip Fix
old_ble_tt = """                mapView.overlays.add(circle)
                if (openTooltipId == identifier) mapView.post { circle.showInfoWindow() }

                if (ble.totalWeight > 20.0) {"""
new_ble_tt = """                mapView.overlays.add(circle)
                if (openTooltipId == identifier && ble.totalWeight <= 20.0) mapView.post { circle.showInfoWindow() }

                if (ble.totalWeight > 20.0) {"""
content = content.replace(old_ble_tt, new_ble_tt)

# Cell Tooltip Fix
old_cell_tt = """                mapView.overlays.add(circle)
                if (openTooltipId == identifier) mapView.post { circle.showInfoWindow() }

                // Always draw cell tower markers, they are rare and spread out"""
new_cell_tt = """                mapView.overlays.add(circle)

                // Always draw cell tower markers, they are rare and spread out"""
content = content.replace(old_cell_tt, new_cell_tt)

with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "w") as f:
    f.write(content)
