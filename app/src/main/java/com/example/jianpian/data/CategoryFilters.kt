package com.example.jianpian.data

data class CategoryFilters(
    val types: List<FilterItem> = emptyList(),
    val regions: List<FilterItem> = emptyList(),
    val years: List<FilterItem> = emptyList(),
    val languages: List<FilterItem> = emptyList(),
    val letters: List<FilterItem> = emptyList()
)

data class FilterItem(
    val name: String,
    val url: String
) 