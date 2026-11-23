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
    data object LiveDrawingBoard : KinNestRoute

    @Serializable
    data object StoryTogether : KinNestRoute

    @Serializable
    data object AdventureMission : KinNestRoute

    @Serializable
    data object PermissionsRecovery : KinNestRoute

    @Serializable
    data object ParentalSettings : KinNestRoute
}

enum class WorkspaceTab(
    val index: Int,
    val label: String,
    val malayalamLabel: String
) {
    HOME(0, "Home", "വീട്"),
    CALL(1, "Call", "വിളി"),
    CHAT(2, "Chat", "സന്ദേശം"),
    PLAY(3, "Play", "കളി"),
    US(4, "Us", "നമ്മൾ")
}
