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

enum class WorkspaceTab(
    val index: Int,
    val label: String,
    val accessibleTitle: String,
    val malayalamLabel: String
) {
    HOME(0, "A12 E", "A12 E Home", "വീട്"),
    CALL(1, "C4L L", "Family Video Call", "വിളി"),
    CHAT(2, "T4L K", "Family Chat", "സന്ദേശം"),
    PLAY(3, "P1A Y", "XOXO Game", "കളി")
}
