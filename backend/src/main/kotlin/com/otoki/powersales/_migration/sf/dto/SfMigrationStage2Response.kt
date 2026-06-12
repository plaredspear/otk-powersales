package com.otoki.powersales._migration.sf.dto

data class SubstepResult(
    val label: String,
    val rowsAffected: Int,
)

data class SfMigrationStage2Response(
    val substep: String,
    val results: List<SubstepResult>,
    val totalRowsAffected: Int,
)
