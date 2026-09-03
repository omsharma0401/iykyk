package com.omsharma.iykyk.data.model

class Person(val id: Int, val appearances: List<Appearance>) {
    val appearanceCount: Int get() = appearances.size
    val observations: List<FaceObservation> get() = appearances.flatMap { it.observations }
}
