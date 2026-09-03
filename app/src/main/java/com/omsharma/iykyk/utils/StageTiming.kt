package com.omsharma.iykyk.utils

// Where processing time went, for the summary log
class StageTiming {
    var detectNs = 0L
    var embedNs = 0L
    var pickNs = 0L

    override fun toString() =
        "detectMs=${detectNs / 1_000_000} embedMs=${embedNs / 1_000_000} pickMs=${pickNs / 1_000_000}"
}
