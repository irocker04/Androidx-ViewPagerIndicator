package ru.lexcorp.viewpagerindicator

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.viewpager.widget.ViewPager


/**
 * This widget implements the dynamic action bar tab behavior that can change
 * across different configurations or circumstances.
 */
class TabPageIndicator(context: Context, attrs: AttributeSet? = null) :
    HorizontalScrollView(context, attrs), PageIndicator {

    private var mTabSelector: Runnable? = null

    private val mTabClickListener = OnClickListener { view ->
        val tabView = view as TabView
        val oldSelected = mViewPager!!.currentItem
        val newSelected = tabView.index
        mViewPager!!.currentItem = newSelected
        if (oldSelected == newSelected && mTabReselectedListener != null) {
            mTabReselectedListener!!.onTabReselected(newSelected)
        }
    }

    private val mTabLayout: IcsLinearLayout

    private var mViewPager: ViewPager? = null
    private var mListener: ViewPager.OnPageChangeListener? = null

    private var mMaxTabWidth: Int = 0
    private var mSelectedTabIndex: Int = 0

    private var mTabReselectedListener: OnTabReselectedListener? = null

    /**
     * Interface for a callback when the selected tab has been reselected.
     */
    interface OnTabReselectedListener {
        /**
         * Callback when the selected tab has been reselected.
         *
         * @param position Position of the current center item.
         */
        fun onTabReselected(position: Int)
    }

    init {
        isHorizontalScrollBarEnabled = false

        mTabLayout = IcsLinearLayout(context, R.attr.vpiTabPageIndicatorStyle)
        addView(mTabLayout, ViewGroup.LayoutParams(WRAP_CONTENT, MATCH_PARENT))
    }

    fun setOnTabReselectedListener(listener: OnTabReselectedListener) {
        mTabReselectedListener = listener
    }

    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val lockedExpanded = widthMode == View.MeasureSpec.EXACTLY
        isFillViewport = lockedExpanded

        val childCount = mTabLayout.childCount
        mMaxTabWidth = if (childCount > 1 && (widthMode == View.MeasureSpec.EXACTLY || widthMode == View.MeasureSpec.AT_MOST)) {
            if (childCount > 2) {
                (View.MeasureSpec.getSize(widthMeasureSpec) * 0.4f).toInt()
            } else {
                View.MeasureSpec.getSize(widthMeasureSpec) / 2
            }
        } else {
            -1
        }

        val oldWidth = measuredWidth
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val newWidth = measuredWidth

        if (lockedExpanded && oldWidth != newWidth) {
            // Recenter the tab display if we're at a new (scrollable) size.
            setCurrentItem(mSelectedTabIndex)
        }
    }

    private fun animateToTab(position: Int) {
        val tabView = mTabLayout.getChildAt(position)
        if (mTabSelector != null) {
            removeCallbacks(mTabSelector)
        }
        mTabSelector = Runnable {
            val scrollPos = tabView.left - (width - tabView.width) / 2
            smoothScrollTo(scrollPos, 0)
            mTabSelector = null
        }
        post(mTabSelector)
    }

    public override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (mTabSelector != null) {
            // Re-post the selector we saved
            post(mTabSelector)
        }
    }

    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (mTabSelector != null) {
            removeCallbacks(mTabSelector)
        }
    }

    private fun addTab(index: Int, text: CharSequence, iconResId: Int) {
        val tabView = TabView(context)
        tabView.index = index
        tabView.isFocusable = true
        tabView.setOnClickListener(mTabClickListener)
        tabView.text = text

        if (iconResId != 0) {
            tabView.setCompoundDrawablesWithIntrinsicBounds(iconResId, 0, 0, 0)
        }

        mTabLayout.addView(tabView, LinearLayout.LayoutParams(0, MATCH_PARENT, 1f))
    }

    override fun onPageScrollStateChanged(arg0: Int) {
        if (mListener != null) {
            mListener!!.onPageScrollStateChanged(arg0)
        }
    }

    override fun onPageScrolled(arg0: Int, arg1: Float, arg2: Int) {
        if (mListener != null) {
            mListener!!.onPageScrolled(arg0, arg1, arg2)
        }
    }

    override fun onPageSelected(arg0: Int) {
        setCurrentItem(arg0)
        if (mListener != null) {
            mListener!!.onPageSelected(arg0)
        }
    }

    override fun setViewPager(view: ViewPager) {
        if (mViewPager === view) {
            return
        }
//        if (mViewPager != null) {
//            mViewPager!!.setOnPageChangeListener(null)
//        }

        if (view.adapter == null) {
            throw IllegalStateException("ViewPager does not have adapter instance.")
        }

        mViewPager = view
        view.addOnPageChangeListener(this)
        notifyDataSetChanged()
    }

    override fun notifyDataSetChanged() {
        mTabLayout.removeAllViews()
        val adapter = mViewPager!!.adapter
        var iconAdapter: IconPagerAdapter? = null
        if (adapter is IconPagerAdapter) {
            iconAdapter = adapter
        }
        val count = adapter!!.count
        for (i in 0 until count) {
            var title = adapter.getPageTitle(i)
            if (title == null) {
                title = EMPTY_TITLE
            }
            var iconResId = 0
            if (iconAdapter != null) {
                iconResId = iconAdapter.getIconResId(i)
            }
            addTab(i, title, iconResId)
        }
        if (mSelectedTabIndex > count) {
            mSelectedTabIndex = count - 1
        }
        setCurrentItem(mSelectedTabIndex)
        requestLayout()
    }

    override fun setViewPager(view: ViewPager, initialPosition: Int) {
        setViewPager(view)
        setCurrentItem(initialPosition)
    }

    override fun setCurrentItem(item: Int) {
        if (mViewPager == null) {
            throw IllegalStateException("ViewPager has not been bound.")
        }
        mSelectedTabIndex = item
        mViewPager!!.currentItem = item

        val tabCount = mTabLayout.childCount
        for (i in 0 until tabCount) {
            val child = mTabLayout.getChildAt(i)
            val isSelected = i == item
            child.isSelected = isSelected
            if (isSelected) {
                animateToTab(item)
            }
        }
    }

    override fun setOnPageChangeListener(listener: ViewPager.OnPageChangeListener) {
        mListener = listener
    }

    private inner class TabView(context: Context) : TextView(context, null, R.attr.vpiTabPageIndicatorStyle) {
        var index: Int = 0

        public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)

            // Re-measure if we went beyond our maximum size.
            if ((mMaxTabWidth > 0) && (measuredWidth > mMaxTabWidth)) {
                super.onMeasure(
                    View.MeasureSpec.makeMeasureSpec(mMaxTabWidth, View.MeasureSpec.EXACTLY),
                    heightMeasureSpec
                )
            }
        }
    }

    companion object {
        /** Title text used when no title is provided by the adapter.  */
        private const val EMPTY_TITLE = ""
    }
}
