package com.qtpie.simplepuzzle.core.learning

import kotlinx.serialization.Serializable

private val NAMESPACED_ID = Regex("[a-z][a-z0-9]*(?:[.-][a-z0-9]+)+")
private val OPAQUE_ID = Regex("[A-Za-z0-9][A-Za-z0-9._:-]{2,159}")

private fun requireNamespaced(value: String, type: String) {
    require(value.length <= 160 && NAMESPACED_ID.matches(value)) {
        "$type must be a lowercase namespaced identifier."
    }
}

private fun requireOpaque(value: String, type: String) {
    require(OPAQUE_ID.matches(value)) {
        "$type must be an opaque identifier containing 3 to 160 safe characters."
    }
}

@Serializable
@JvmInline
value class SkillId(val value: String) {
    init {
        requireNamespaced(value, "SkillId")
    }
}

@Serializable
@JvmInline
value class CurriculumId(val value: String) {
    init {
        requireNamespaced(value, "CurriculumId")
    }
}

@Serializable
@JvmInline
value class ActivityId(val value: String) {
    init {
        requireNamespaced(value, "ActivityId")
    }
}

@Serializable
@JvmInline
value class LearningItemId(val value: String) {
    init {
        requireNamespaced(value, "LearningItemId")
    }
}

@Serializable
@JvmInline
value class AttemptId(val value: String) {
    init {
        requireOpaque(value, "AttemptId")
    }
}

@Serializable
@JvmInline
value class SessionId(val value: String) {
    init {
        requireOpaque(value, "SessionId")
    }
}

@Serializable
@JvmInline
value class ContentVersion(val value: Int) {
    init {
        require(value > 0) { "ContentVersion must be positive." }
    }
}

@Serializable
@JvmInline
value class ActivityVersion(val value: Int) {
    init {
        require(value > 0) { "ActivityVersion must be positive." }
    }
}

@Serializable
@JvmInline
value class EvidencePolicyVersion(val value: Int) {
    init {
        require(value > 0) { "EvidencePolicyVersion must be positive." }
    }
}

@Serializable
@JvmInline
value class TaxonomyVersion(val value: Int) {
    init {
        require(value > 0) { "TaxonomyVersion must be positive." }
    }
}
