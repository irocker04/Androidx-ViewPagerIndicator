package ru.lexcorp.viewpagerindicator

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.ImageView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener

import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewGroup.LayoutParams.MATCH_PARENT

/**
 * This widget implements the dynamic action bar tab behavior that can change
 * across different configurations or circumstances.
 */
class IconPageIndicator(context: Context, attrs: AttributeSet) : HorizontalScrollView(context, attrs), PageIndicator {

    private val mIconsLayout: IcsLinearLayout

    private var mViewPager: ViewPager? = null
    private var mListener: OnPageChangeListener? = null
    private var mIconSelector: Runnable? = null
    private var mSelectedIndex: Int = 0

    init {
        isHorizontalScrollBarEnabled = false

        mIconsLayout = IcsLinearLayout(context, R.attr.vpiIconPageIndicatorStyle)
        addView(mIconsLayout, LayoutParams(WRAP_CONTENT, MATCH_PARENT, Gravity.CENTER))
    }

    private fun animateToIcon(position: Int) {
        val iconView = mIconsLayout.getChildAt(position)
        if (mIconSelector != null) {
            removeCallbacks(mIconSelector)
        }
        mIconSelector = Runnable {
            val scrollPos = iconView.left - (width - iconView.width) / 2
            smoothScrollTo(scrollPos, 0)
            mIconSelector = null
        }
        post(mIconSelector)
    }

    public override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (mIconSelector != null) {
            // Re-post the selector we saved
            post(mIconSelector)
        }
    }

    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (mIconSelector != null) {
            removeCallbacks(mIconSelector)
        }
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
//            mViewPager!!.addOnPageChangeListener(null)
//        }

        if (view.adapter == null) {
            throw IllegalStateException("ViewPager does not have adapter instance.")
        }
        mViewPager = view
        view.addOnPageChangeListener(this)
        notifyDataSetChanged()
    }

    override fun notifyDataSetChanged() {
        mIconsLayout.removeAllViews()
        val iconAdapter = mViewPager!!.adapter as IconPagerAdapter
        val count = iconAdapter.getCount()
        for (i in 0 until count) {
            val view = ImageView(context, null, R.attr.vpiIconPageIndicatorStyle)
            view.setImageResource(iconAdapter.getIconResId(i))
            mIconsLayout.addView(view)
        }
        if (mSelectedIndex > count) {
            mSelectedIndex = count - 1
        }
        setCurrentItem(mSelectedIndex)
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
        mSelectedIndex = item
        mViewPager!!.currentItem = item

        val tabCount = mIconsLayout.childCount
        for (i in 0 until tabCount) {
            val child = mIconsLayout.getChildAt(i)
            val isSelected = i == item
            child.isSelected = isSelected
            if (isSelected) {
                animateToIcon(item)
            }
        }
    }

    override fun setOnPageChangeListener(listener: ViewPager.OnPageChangeListener) {
        mListener = listener
    }
}
