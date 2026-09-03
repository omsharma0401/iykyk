package com.omsharma.iykyk.data.model.enums

enum class ProcessingStage(val label: String) {
    INITIALIZING("Preheating…"),
    DETECTING_FACES("Cooking… spotting faces"),
    MATCHING_PEOPLE("Simmering… matching people"),
    PICKING_BEST_SHOTS("Plating… picking best shots")
}