package ru.lexcorp.viewpagerindicator

interface IconPagerAdapter {

    // From PagerAdapter
    fun getCount(): Int //val count: Int

    /**
     * Get icon representing the page at `index` in the adapter.
     */
    fun getIconResId(index: Int): Int
}