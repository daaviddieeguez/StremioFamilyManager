package com.example.stremiofamily

data class AddonDifference(val additions: Int, val removals: Int)

/** Counts changes using Stremio's addon endpoint URL identity, matching its client library. */
fun addonDifference(source: List<AddonDescriptor>, target: List<AddonDescriptor>): AddonDifference {
    val sourceUrls = source.map { it.transportUrl }.toSet()
    val targetUrls = target.map { it.transportUrl }.toSet()
    return AddonDifference((sourceUrls - targetUrls).size, (targetUrls - sourceUrls).size)
}
