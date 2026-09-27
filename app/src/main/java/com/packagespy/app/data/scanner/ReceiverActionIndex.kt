package com.packagespy.app.data.scanner

import com.packagespy.app.domain.usecase.RiskScorer

/**
 * The fixed set of "package lifecycle" broadcast actions we care about when
 * inspecting a manifest receiver. Order matters: it determines the order of
 * actions in [buildReceiverActionIndex]'s output.
 */
val WATCHED_RECEIVER_ACTIONS: List<String> = listOf(
    RiskScorer.ACTION_PACKAGE_ADDED,
    RiskScorer.ACTION_PACKAGE_REMOVED,
    RiskScorer.ACTION_BOOT_COMPLETED,
    "android.intent.action.PACKAGE_REPLACED",
    "android.intent.action.PACKAGE_CHANGED",
    "android.intent.action.PACKAGE_FULLY_REMOVED",
)

/** Identifies a manifest receiver by its owning package and class name. */
data class ReceiverKey(val packageName: String, val receiverName: String)

/**
 * Builds a lookup from [ReceiverKey] to the list of [WATCHED_RECEIVER_ACTIONS]
 * that receiver resolves, given the raw per-action resolution results.
 *
 * @param hits action -> receivers that resolve it (as returned by one
 *   `queryBroadcastReceivers` call per action).
 */
fun buildReceiverActionIndex(hits: Map<String, List<ReceiverKey>>): Map<ReceiverKey, List<String>> {
    val index = LinkedHashMap<ReceiverKey, MutableList<String>>()
    for (action in WATCHED_RECEIVER_ACTIONS) {
        val receivers = hits[action] ?: continue
        for (key in receivers) {
            val actions = index.getOrPut(key) { mutableListOf() }
            if (action !in actions) {
                actions += action
            }
        }
    }
    return index
}
