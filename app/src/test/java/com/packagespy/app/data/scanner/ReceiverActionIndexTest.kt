package com.packagespy.app.data.scanner

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiverActionIndexTest {

    @Test
    fun `empty hits yields empty index`() {
        val index = buildReceiverActionIndex(emptyMap())
        assertEquals(emptyMap<ReceiverKey, List<String>>(), index)
    }

    @Test
    fun `actions for a key are ordered by WATCHED_RECEIVER_ACTIONS, not by hits order`() {
        val key = ReceiverKey("com.example.app", "com.example.app.Receiver")
        val hits = mapOf(
            WATCHED_RECEIVER_ACTIONS[2] to listOf(key),
            WATCHED_RECEIVER_ACTIONS[0] to listOf(key),
        )
        val index = buildReceiverActionIndex(hits)
        assertEquals(
            listOf(WATCHED_RECEIVER_ACTIONS[0], WATCHED_RECEIVER_ACTIONS[2]),
            index[key],
        )
    }

    @Test
    fun `action not in WATCHED_RECEIVER_ACTIONS is ignored`() {
        val key = ReceiverKey("com.example.app", "com.example.app.Receiver")
        val hits = mapOf(
            "android.intent.action.SOME_OTHER_ACTION" to listOf(key),
        )
        val index = buildReceiverActionIndex(hits)
        assertEquals(emptyMap<ReceiverKey, List<String>>(), index)
    }

    @Test
    fun `duplicate receiver key within one action yields the action once`() {
        val key = ReceiverKey("com.example.app", "com.example.app.Receiver")
        val hits = mapOf(
            WATCHED_RECEIVER_ACTIONS[0] to listOf(key, key),
        )
        val index = buildReceiverActionIndex(hits)
        assertEquals(listOf(WATCHED_RECEIVER_ACTIONS[0]), index[key])
    }

    @Test
    fun `same receiver class name in two packages yields two distinct keys`() {
        val keyA = ReceiverKey("com.example.a", "com.example.Receiver")
        val keyB = ReceiverKey("com.example.b", "com.example.Receiver")
        val hits = mapOf(
            WATCHED_RECEIVER_ACTIONS[0] to listOf(keyA),
            WATCHED_RECEIVER_ACTIONS[1] to listOf(keyB),
        )
        val index = buildReceiverActionIndex(hits)
        assertEquals(listOf(WATCHED_RECEIVER_ACTIONS[0]), index[keyA])
        assertEquals(listOf(WATCHED_RECEIVER_ACTIONS[1]), index[keyB])
    }
}
