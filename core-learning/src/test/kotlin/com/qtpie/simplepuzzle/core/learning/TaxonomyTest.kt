package com.qtpie.simplepuzzle.core.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class TaxonomyTest {
    @Test
    fun stableIdentifiersRejectBlankOrUnnamespacedValues() {
        assertThrows(IllegalArgumentException::class.java) { SkillId("") }
        assertThrows(IllegalArgumentException::class.java) { SkillId("addition") }
        assertThrows(IllegalArgumentException::class.java) { AttemptId("a") }
        assertEquals("math.addition.to-1000-with-regrouping", Grade2Quarter1Skills.ADD_WITH_REGROUPING.value)
    }

    @Test
    fun approvedTaxonomyHasUniqueMappedSkillsAndNoImplicitPrerequisites() {
        val taxonomy = Grade2Quarter1Taxonomy.value

        TaxonomyValidator.validate(taxonomy)

        assertEquals(13, taxonomy.skills.size)
        assertEquals(taxonomy.skills.size, taxonomy.skills.map { it.id }.distinct().size)
        assertEquals(taxonomy.skills.map { it.id }.toSet(), taxonomy.mappings.map { it.skillId }.toSet())
        assertTrue(taxonomy.skills.all { it.prerequisites.isEmpty() })
        assertTrue(taxonomy.mappings.all { it.mappingVersion == ContentVersion(1) })
    }

    @Test
    fun duplicateIdsAreRejected() {
        val skill = skill(SkillId("math.test.one"))
        val taxonomy = SkillTaxonomy(TaxonomyVersion(1), listOf(skill, skill.copy(title = "Duplicate")), emptyList())

        assertThrows(IllegalArgumentException::class.java) { TaxonomyValidator.validate(taxonomy) }
    }

    @Test
    fun prerequisiteCyclesAreRejected() {
        val one = SkillId("math.test.one")
        val two = SkillId("math.test.two")
        val taxonomy = SkillTaxonomy(
            TaxonomyVersion(1),
            listOf(skill(one, setOf(two)), skill(two, setOf(one))),
            emptyList(),
        )

        assertThrows(IllegalArgumentException::class.java) { TaxonomyValidator.validate(taxonomy) }
    }

    @Test
    fun mappingsRequireKnownSkillsAndExplicitVersion() {
        assertThrows(IllegalArgumentException::class.java) { ContentVersion(0) }
        val mapping = CurriculumMapping(
            curriculumId = CurriculumId("ph.test.math"),
            curriculumVersion = "v1",
            mappingVersion = ContentVersion(1),
            skillId = SkillId("math.test.unknown"),
            grade = 2,
            quarter = 1,
            strand = "Number and Algebra",
            scopeStatement = "Test scope",
        )

        assertThrows(IllegalArgumentException::class.java) {
            TaxonomyValidator.validate(SkillTaxonomy(TaxonomyVersion(1), listOf(skill(SkillId("math.test.known"))), listOf(mapping)))
        }
    }

    private fun skill(id: SkillId, prerequisites: Set<SkillId> = emptySet()) = SkillDefinition(
        id = id,
        taxonomyVersion = TaxonomyVersion(1),
        title = id.value,
        description = "Test skill",
        prerequisites = prerequisites,
    )
}
