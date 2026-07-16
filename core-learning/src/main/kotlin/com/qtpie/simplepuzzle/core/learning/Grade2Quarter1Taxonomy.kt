package com.qtpie.simplepuzzle.core.learning

data class SkillTaxonomy(
    val version: TaxonomyVersion,
    val skills: List<SkillDefinition>,
    val mappings: List<CurriculumMapping>,
)

object TaxonomyValidator {
    fun validate(taxonomy: SkillTaxonomy) {
        val skillsById = taxonomy.skills.associateBy(SkillDefinition::id)
        require(skillsById.size == taxonomy.skills.size) { "Taxonomy contains duplicate skill IDs." }
        require(taxonomy.skills.all { it.taxonomyVersion == taxonomy.version }) {
            "Every skill must use the taxonomy version."
        }
        taxonomy.skills.forEach { skill ->
            require(skill.prerequisites.all(skillsById::containsKey)) {
                "Skill ${skill.id.value} refers to an unknown prerequisite."
            }
            require(skill.replacementId == null || skillsById.containsKey(skill.replacementId)) {
                "Skill ${skill.id.value} refers to an unknown replacement."
            }
        }
        require(taxonomy.mappings.map { mapping ->
            listOf(
                mapping.curriculumId.value,
                mapping.curriculumVersion,
                mapping.mappingVersion.value.toString(),
                mapping.skillId.value,
                mapping.grade.toString(),
                mapping.quarter.toString(),
            ).joinToString("|")
        }.distinct().size == taxonomy.mappings.size) {
            "Taxonomy contains duplicate curriculum mappings."
        }
        require(taxonomy.mappings.all { it.skillId in skillsById }) {
            "Every curriculum mapping must reference a known skill."
        }

        val visiting = mutableSetOf<SkillId>()
        val visited = mutableSetOf<SkillId>()
        fun visit(id: SkillId) {
            if (id in visited) return
            require(visiting.add(id)) { "Taxonomy prerequisite graph contains a cycle at ${id.value}." }
            skillsById.getValue(id).prerequisites.forEach(::visit)
            visiting.remove(id)
            visited.add(id)
        }
        taxonomy.skills.forEach { visit(it.id) }
    }
}

object Grade2Quarter1Skills {
    val REPRESENT_TO_1000 = SkillId("math.whole-number.represent-to-1000")
    val SKIP_COUNT_TO_1000 = SkillId("math.whole-number.skip-count-to-1000")
    val COMPARE_TO_1000 = SkillId("math.whole-number.compare-to-1000")
    val ORDER_TO_1000 = SkillId("math.whole-number.order-to-1000")
    val HUNDREDS_TENS_ONES = SkillId("math.place-value.hundreds-tens-ones")
    val DIGIT_PLACE_AND_VALUE = SkillId("math.place-value.digit-place-and-value")
    val NUMBER_LINE_COUNT_UP = SkillId("math.addition.number-line-count-up")
    val EXPANDED_FORM = SkillId("math.addition.expanded-form")
    val ADD_WITHOUT_REGROUPING = SkillId("math.addition.to-1000-without-regrouping")
    val ADD_WITH_REGROUPING = SkillId("math.addition.to-1000-with-regrouping")
    val ZERO_PROPERTY = SkillId("math.addition.property.zero")
    val COMMUTATIVE_PROPERTY = SkillId("math.addition.property.commutative")
    val ASSOCIATIVE_PROPERTY = SkillId("math.addition.property.associative")
}

object Grade2Quarter1Taxonomy {
    val taxonomyVersion = TaxonomyVersion(1)
    val curriculumId = CurriculumId("ph.deped.matatag-mathematics")
    const val CURRICULUM_VERSION = "revised-k10-matatag"

    private data class Seed(val id: SkillId, val title: String, val description: String)

    private val seeds = listOf(
        Seed(Grade2Quarter1Skills.REPRESENT_TO_1000, "Represent whole numbers to 1,000", "Count, recognize, read, and represent whole numbers to 1,000."),
        Seed(Grade2Quarter1Skills.SKIP_COUNT_TO_1000, "Skip-count within 1,000", "Count by 2s, 5s, 10s, 20s, 50s, and 100s within 1,000."),
        Seed(Grade2Quarter1Skills.COMPARE_TO_1000, "Compare whole numbers to 1,000", "Compare whole-number magnitudes within 1,000."),
        Seed(Grade2Quarter1Skills.ORDER_TO_1000, "Order whole numbers to 1,000", "Order whole numbers within 1,000."),
        Seed(Grade2Quarter1Skills.HUNDREDS_TENS_ONES, "Identify hundreds, tens, and ones", "Identify hundreds, tens, and ones in three-digit numbers."),
        Seed(Grade2Quarter1Skills.DIGIT_PLACE_AND_VALUE, "Determine a digit’s place and value", "Determine the place and value of a digit in a whole number."),
        Seed(Grade2Quarter1Skills.NUMBER_LINE_COUNT_UP, "Add by counting up on a number line", "Illustrate addition through movement and counting up on a number line."),
        Seed(Grade2Quarter1Skills.EXPANDED_FORM, "Add using expanded form", "Represent and solve addition using expanded form."),
        Seed(Grade2Quarter1Skills.ADD_WITHOUT_REGROUPING, "Add to 1,000 without regrouping", "Add whole numbers with sums to 1,000 without regrouping."),
        Seed(Grade2Quarter1Skills.ADD_WITH_REGROUPING, "Add to 1,000 with regrouping", "Add whole numbers with sums to 1,000 with regrouping."),
        Seed(Grade2Quarter1Skills.ZERO_PROPERTY, "Understand the zero property of addition", "Use age-appropriate representations of the zero property of addition."),
        Seed(Grade2Quarter1Skills.COMMUTATIVE_PROPERTY, "Understand the commutative property of addition", "Use age-appropriate representations of the commutative property of addition."),
        Seed(Grade2Quarter1Skills.ASSOCIATIVE_PROPERTY, "Understand the associative property of addition", "Use age-appropriate representations of the associative property of addition."),
    )

    val value: SkillTaxonomy = SkillTaxonomy(
        version = taxonomyVersion,
        skills = seeds.map { seed ->
            SkillDefinition(
                id = seed.id,
                taxonomyVersion = taxonomyVersion,
                title = seed.title,
                description = seed.description,
            )
        },
        mappings = seeds.map { seed ->
            CurriculumMapping(
                curriculumId = curriculumId,
                curriculumVersion = CURRICULUM_VERSION,
                mappingVersion = ContentVersion(1),
                skillId = seed.id,
                grade = 2,
                quarter = 1,
                strand = "Number and Algebra",
                scopeStatement = seed.description,
            )
        },
    ).also(TaxonomyValidator::validate)
}
