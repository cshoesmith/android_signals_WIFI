import re
with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "r") as f:
    content = f.read()

# Change getBitmapDrawable to return Bitmap
old_func = """fun getBitmapDrawable(context: android.content.Context, id: Int): BitmapDrawable? {
    val drawable = ContextCompat.getDrawable(context, id) ?: return null
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return BitmapDrawable(context.resources, bitmap)
}"""

new_func = """fun getBitmap(context: android.content.Context, id: Int): Bitmap? {
    val drawable = ContextCompat.getDrawable(context, id) ?: return null
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}"""
content = content.replace(old_func, new_func)

# Change setup variables
old_vars = """            val wifiIconBlue = getBitmapDrawable(context, R.drawable.ic_cloud_wifi_blue)
            val wifiIconLightGreen = getBitmapDrawable(context, R.drawable.ic_cloud_wifi_light_green)
            val wifiIconDarkGreen = getBitmapDrawable(context, R.drawable.ic_cloud_wifi_dark_green)
            val bleIcon = getBitmapDrawable(context, R.drawable.ic_ble_device)
            val cellIcon = getBitmapDrawable(context, R.drawable.ic_cell_tower)"""
new_vars = """            val wifiBlueBmp = getBitmap(context, R.drawable.ic_cloud_wifi_blue)
            val wifiLightGreenBmp = getBitmap(context, R.drawable.ic_cloud_wifi_light_green)
            val wifiDarkGreenBmp = getBitmap(context, R.drawable.ic_cloud_wifi_dark_green)
            val bleBmp = getBitmap(context, R.drawable.ic_ble_device)
            val cellBmp = getBitmap(context, R.drawable.ic_cell_tower)"""
content = content.replace(old_vars, new_vars)

# Change wifi marker
old_wifi_marker = """                        val selectedIcon = if (!isSecured) {
                            wifiIconLightGreen
                        } else if (isWep) {
                            wifiIconDarkGreen
                        } else {
                            wifiIconBlue
                        }

                        if (selectedIcon != null) {
                            icon = selectedIcon
                        }"""
new_wifi_marker = """                        val selectedBmp = if (!isSecured) {
                            wifiLightGreenBmp
                        } else if (isWep) {
                            wifiDarkGreenBmp
                        } else {
                            wifiBlueBmp
                        }

                        if (selectedBmp != null) {
                            icon = BitmapDrawable(context.resources, selectedBmp)
                        }"""
content = content.replace(old_wifi_marker, new_wifi_marker)

# Change ble marker
old_ble_marker = """                        if (bleIcon != null) {
                            icon = bleIcon
                        }"""
new_ble_marker = """                        if (bleBmp != null) {
                            icon = BitmapDrawable(context.resources, bleBmp)
                        }"""
content = content.replace(old_ble_marker, new_ble_marker)

# Change cell marker
old_cell_marker = """                    if (cellIcon != null) {
                        icon = cellIcon
                    }"""
new_cell_marker = """                    if (cellBmp != null) {
                        icon = BitmapDrawable(context.resources, cellBmp)
                    }"""
content = content.replace(old_cell_marker, new_cell_marker)

with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "w") as f:
    f.write(content)
