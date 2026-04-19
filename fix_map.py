import re
with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "r") as f:
    content = f.read()

# Add last interaction tracker
s1_old = """    var mapViewState by remember { mutableStateOf<MapView?>(null) }
    var initialCenterSet by remember { mutableStateOf(false) }
    var openTooltipId by remember { mutableStateOf<String?>(null) }"""
s1_new = """    var mapViewState by remember { mutableStateOf<MapView?>(null) }
    var initialCenterSet by remember { mutableStateOf(false) }
    var openTooltipId by remember { mutableStateOf<String?>(null) }
    val lastManualMoveTime = remember { longArrayOf(0L) }"""
content = content.replace(s1_old, s1_new)

# Reset interaction time when centertrigger fires
s2_old = """    LaunchedEffect(centerTrigger) {
        if (centerTrigger > 0) {
            val mv = mapViewState"""
s2_new = """    LaunchedEffect(centerTrigger) {
        if (centerTrigger > 0) {
            lastManualMoveTime[0] = 0L
            val mv = mapViewState"""
content = content.replace(s2_old, s2_new)

# Add listener
s3_old = """        factory = { ctx ->
            MapView(ctx).apply {
                mapViewState = this
                setBuiltInZoomControls(false)
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(18.0)
            }
        },"""
s3_new = """        factory = { ctx ->
            MapView(ctx).apply {
                mapViewState = this
                setBuiltInZoomControls(false)
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(18.0)
                setOnTouchListener { _, _ ->
                    lastManualMoveTime[0] = System.currentTimeMillis()
                    false // Do not consume the event
                }
            }
        },"""
content = content.replace(s3_old, s3_new)

# Block updates for 10s
s4_old = """        modifier = Modifier.fillMaxSize(),
        update = { mapView ->
            InfoWindow.closeAllInfoWindowsOn(mapView)
            mapView.overlays.clear()

            var centerPoint: GeoPoint? = null"""
s4_new = """        modifier = Modifier.fillMaxSize(),
        update = { mapView ->
            if (System.currentTimeMillis() - lastManualMoveTime[0] < 10000L) {
                return@AndroidView
            }

            InfoWindow.closeAllInfoWindowsOn(mapView)
            mapView.overlays.clear()

            var centerPoint: GeoPoint? = null"""
content = content.replace(s4_old, s4_new)

with open("app/src/main/java/com/example/androidsignalswifi/MapScreen.kt", "w") as f:
    f.write(content)
