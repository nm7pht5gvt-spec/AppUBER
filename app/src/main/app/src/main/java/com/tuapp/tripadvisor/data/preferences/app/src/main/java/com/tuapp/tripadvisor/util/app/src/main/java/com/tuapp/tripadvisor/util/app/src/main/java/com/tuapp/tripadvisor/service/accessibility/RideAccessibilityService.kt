package com.tuapp.tripadvisor.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.tuapp.tripadvisor.data.parser.ScreenTextParser
import com.tuapp.tripadvisor.service.overlay.OverlayService

class RideAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "RideAccessibilityService"
        private const val PACKAGE_UBER_DRIVER = "com.ubercab.driver"
        private const val PACKAGE_DIDI_DRIVER = "com.didiglobal.driver"

        private val TARGET_PACKAGES = setOf(PACKAGE_UBER_DRIVER, PACKAGE_DIDI_DRIVER)
    }

    private var lastProcessedText: String = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Servicio de accesibilidad conectado")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        val packageName = event.packageName?.toString() ?: return
        if (packageName !in TARGET_PACKAGES) return

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            return
        }

        val rootNode = rootInActiveWindow ?: return
        val screenText = extractAllText(rootNode)
        rootNode.recycle()

        if (screenText.isBlank() || screenText == lastProcessedText) return
        lastProcessedText = screenText

        val offer = ScreenTextParser.parse(screenText)

        if (offer != null) {
            sendOfferToOverlay(offer)
        } else {
            notifyNoOffer()
        }
    }

    private fun extractAllText(node: AccessibilityNodeInfo): String {
        val builder = StringBuilder()
        collectText(node, builder)
        return builder.toString()
    }

    private fun collectText(node: AccessibilityNodeInfo?, builder: StringBuilder) {
        node ?: return

        node.text?.let {
            builder.append(it).append(" ")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectText(child, builder)
            child.recycle()
        }
    }

    private fun sendOfferToOverlay(offer: com.tuapp.tripadvisor.domain.model.TripOffer) {
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_OFFER
            putExtra(OverlayService.EXTRA_DISTANCE_KM, offer.distanceKm)
            putExtra(OverlayService.EXTRA_TIME_MINUTES, offer.estimatedTimeMinutes)
            putExtra(OverlayService.EXTRA_PRICE, offer.offeredPrice)
        }
        startService(intent)
    }

    private fun notifyNoOffer() {
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_HIDE_OFFER
        }
        startService(intent)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Servicio de accesibilidad interrumpido")
    }
}
