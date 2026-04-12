package com.example.androidsignalswifi

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Marker
import android.graphics.Color

@Composable
fun MapScreen(aps: List<ScannedAp>) {
    val context = LocalContext.current
    Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(18.0)
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { mapView ->
            mapView.overlays.clear()

            var centerPoint: GeoPoint? = null

            for (ap in aps) {
                val point = GeoPoint(ap.estLat, ap.estLon)
                if (centerPoint == null) centerPoint = point

                val securityText = if (ap.isSecured) "Secured" else "Open"

                val hash = ap.bssid.hashCode()
                val r = (hash and 0xFF0000) shr 16
                val g = (hash and 0x00FF00) shr 8
                val b = (hash and 0x0000FF)

                val radiusMeters = kotlin.math.max(10.0, 2000.0 / kotlin.math.max(10.0, ap.totalWeight))

                val circle = Polygon(mapView).apply {
                    points = Polygon.pointsAsCircle(point, radiusMeters)
                    fillPaint.color = Color.argb(100, r, g, b)
                    outlinePaint.color = Color.argb(255, r, g, b)
                    outlinePaint.strokeWidth = 3.0f
                    title = "SSID: ${if(ap.ssid.isNotEmpty()) ap.ssid else "[Hidden]"} ($securityText)"
                    snippet = "BSSID: ${ap.bssid}\nRSSI: ${ap.rssi} dBm\nWeight: ${ap.totalWeight.toInt()}"
                }
                mapView.overlays.add(circle)

                if (ap.totalWeight > 200.0) {
                    val marker = Marker(mapView).apply {
                        position = point
                        title = circle.title
                        snippet = circle.snippet
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = ContextCompat.getDrawable(context, R.drawable.ic_wifi_pin)
                    }
                    mapView.overlays.add(marker)
                }
            }

            if (centerPoint != null && mapView.overlays.isNotEmpty()) {
                mapView.controller.setCenter(centerPoint)
            }

            mapView.invalidate()
        }
    )
}
