package com.example.androidsignalswifi

import android.location.Location
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilessource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(aps: List<ScannedAp>) {
    val context = LocalContext.current
    Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))

    AndroidView(
        factory = { ctx ->
            MapView(context).apply {
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
                
                val marker = Marker(mapView).apply {
                    position = point
                    title = "SSID:  ()"
                    snippet = "BSSID: \nRSSI:  dBm\nType: "
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(marker)
            }
            
            if (centerPoint != null && mapView.overlays.isNotEmpty()) {
                mapView.controller.setCenter(centerPoint)
            }
            
            mapView.invalidate()
        }
    )
}
