package com.example.mutlabocsnotes

enum class HomeSection {
    METERS,
    APPLIANCES,
    LIGHTING,
    DOCUMENTS,
    CONTACTS,
    OTHER
}

data class HomeField(
    val key: String = "",
    val value: String = ""
)

data class HomeInfoCard(
    val id: String = "",
    val title: String = "",
    val section: HomeSection = HomeSection.OTHER,
    val fields: List<HomeField> = emptyList(),
    val note: String = "",
    val links: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)