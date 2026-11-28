package com.example.ui

import kotlinx.serialization.Serializable

@Serializable
sealed interface KinNestRoute {
    @Serializable
    data object LoginPortal : KinNestRoute

    @Serializable
    data class Workspace(val initialTab: Int = 0) : KinNestRoute

    @Serializable
    data object ActiveCall : KinNestRoute

    @Serializable
    data object IncomingCall : KinNestRoute

    @Serializable
    data object ParentalSettings : KinNestRoute
}

/**
 * 4 fun, 6-year-old-friendly workspace pages matching Holy Moly !
 */
enum class WorkspaceTab(
    val index: Int,
    val label: String,
    val funTitle: String,
    val accessibleTitle: String,
    val malayalamLabel: String
) {
    HOME(0, "Buzz", "Buzz Box", "Buzz Box Walkie Talkie", "ബസ്"),
    CALL(1, "Ring", "Ring Ring", "Ring Ring Video and Voice Calls", "വിളി"),
    CHAT(2, "Chatter", "Chatter Box", "Chatter Box Family Chat", "വർത്തമാനം"),
    PLAY(3, "Play", "Play Zone", "Play Zone XOXO Game", "കളി")
}
