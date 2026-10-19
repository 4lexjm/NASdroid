package com.nasdroid.reporting.logic.graph

/**
 * Encapsulates all possible timeframes for which graph data can be retrieved.
 */
enum class GraphTimeframe {
    /**
     * Past hour timeframe.
     */
    Hour,

    /**
     * Past day timeframe.
     */
    Day,

    /**
     * Past week timeframe.
     */
    Week,

    /**
     * Past month timeframe.
     */
    Month,

    /**
     * Past year timeframe.
     */
    Year
}
