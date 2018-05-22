/*
    Copyright 2018 Adrian Hall

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
package com.shellmonger.apps.mynotes.ui

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.support.annotation.ColorInt
import android.support.annotation.Px
import android.text.InputFilter
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import com.shellmonger.apps.mynotes.R

enum class ViewType(val type: Int) {
    RECTANGLE(0),
    LINE(1);

    companion object {
        fun valueOf(value: Int): ViewType? = ViewType.values().find { it.type == value }
    }
}

class MFAEditText @JvmOverloads constructor(context: Context, attributes: AttributeSet? = null, defStyleAttr: Int = R.attr.mfaEditTextStyle) : EditText(context, attributes, defStyleAttr) {
    companion object {
        /**
         * The default number of characters in the control.
         */
        private const val DEFAULT_ITEM_COUNT: Int = 6

        /**
         * The periodicity of the cursor blink (in ms)
         */
        private const val BLINK: Long = 500

        /**
         * A list of input filters with no filters
         */
        private val NO_FILTERS = arrayOfNulls<InputFilter>(0)

    }

    // region Private variables
    // Attribute values
    private var mViewType: ViewType
    private var mItemCount: Int
    private var mItemWidth: Int
    private var mItemHeight: Int
    private var mItemRadius: Int
    private var mItemSpacing: Int
    private var mLineWidth: Int
    private var mLineColor: ColorStateList?
    private var mCursorColor: Int
    private var mCursorWidth: Int

    // Internal variables we need
    private val mPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mTextPaint: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val mAnimatorTextPaint: Paint
    private var mCurLineColor = Color.BLACK
    private var mDefaultAddAnimator = ValueAnimator.ofFloat(0.5f, 1f)
    private var mBlink = Blink()
    private var mIsCursorVisible = true
    private var drawCursor = true
    private var isAnimationEnabled = false
    private var mCursorHeight: Float = 0F

    // Drawing components
    private val mItemCenterPoint = PointF()
    private val mItemLineRect = RectF()
    private val mItemBorderRect = RectF()
    private val mPath = Path()
    private val mTextRect = Rect()
    // endregion

    // region Initialization
    init {
        val attrs = context.theme.obtainStyledAttributes(attributes, R.styleable.MFAEditText, defStyleAttr, 0)
        mViewType = ViewType.valueOf(attrs.getInt(R.styleable.MFAEditText_viewType, ViewType.RECTANGLE.ordinal))!!
        mItemCount = attrs.getInt(R.styleable.MFAEditText_itemCount, DEFAULT_ITEM_COUNT)
        mItemHeight = attrs.getDimensionPixelSize(R.styleable.MFAEditText_itemHeight, resources.getDimensionPixelSize(R.dimen.mfa_item_size))
        mItemWidth = attrs.getDimensionPixelSize(R.styleable.MFAEditText_itemWidth, resources.getDimensionPixelSize(R.dimen.mfa_item_size))
        mItemSpacing = attrs.getDimensionPixelSize(R.styleable.MFAEditText_itemSpacing, resources.getDimensionPixelSize(R.dimen.mfa_item_spacing))
        mItemRadius = attrs.getDimensionPixelSize(R.styleable.MFAEditText_itemRadius, resources.getDimensionPixelSize(R.dimen.mfa_item_radius))
        mLineWidth = attrs.getDimensionPixelSize(R.styleable.MFAEditText_lineWidth, resources.getDimensionPixelSize(R.dimen.mfa_line_width))
        mLineColor = attrs.getColorStateList(R.styleable.MFAEditText_lineColor)
        isCursorVisible = attrs.getBoolean(R.styleable.MFAEditText_android_cursorVisible, true)
        mCursorColor = attrs.getColor(R.styleable.MFAEditText_cursorColor, currentTextColor)
        mCursorWidth = attrs.getDimensionPixelSize(R.styleable.MFAEditText_cursorWidth, resources.getDimensionPixelSize(R.dimen.mfa_cursor_width))
        attrs.recycle()

        mCurLineColor = mLineColor?.defaultColor ?: Color.BLACK
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeWidth = mLineWidth.toFloat()
        mTextPaint.density = resources.displayMetrics.density
        mTextPaint.style = Paint.Style.FILL
        mTextPaint.textSize = textSize
        mAnimatorTextPaint = TextPaint(mTextPaint)

        updateCursorHeight()
        checkItemRadius()
        setMaxLength(mItemCount)

        // Set up default animator
        mDefaultAddAnimator.duration = 150
        mDefaultAddAnimator.interpolator = DecelerateInterpolator()
        mDefaultAddAnimator.addUpdateListener { animation: ValueAnimator ->
            val scale = animation.animatedValue as Float
            mAnimatorTextPaint.textSize = textSize * scale
            mAnimatorTextPaint.alpha = (255 * scale).toInt()
            postInvalidate()
        }

        super.setCursorVisible(false)
        setTextIsSelectable(false)
    }
    // endregion

    // region Public API
    /**
     * Sets the width (in pixels) of the cursor
     *
     * @attr ref R.styleable#MFAEditText_cursorWidth
     * @see #getCursorWidth()
     */
    fun setCursorWidth(@Px width: Int) {
        if (mCursorWidth != width) {
            mCursorWidth = width
            if (isCursorVisible) invalidateCursor(true)
        }
    }

    /**
     * @return the width of the cursor (in pixels)
     * @see #setCursorWidth(int)
     */
    @Px fun getCursorWidth() = mCursorWidth

    /**
     * Sets the cursor color
     *
     * @param color A color value in the form 0xAARRGGBB - do not pass a resource ID
     * @attr ref R.styleable#MFAEditText_cursorColor
     * @see #getCursorColor()
     */
    fun setCursorColor(@ColorInt color: Int) {
        if (mCursorColor != color) {
            mCursorColor = color
            if (isCursorVisible) invalidateCursor(true)
        }
    }

    /**
     * @return the current cursor color
     * @see #setCursorColor(int)
     */
    @ColorInt fun getCursorColor() = mCursorColor

    /**
     * Sets the cursor visibility
     *
     * @param visible true if the cursor is visible
     * @attr ref R.styleable#MFAEditText_android_cursorVisible
     * @see #isCursorVisible()
     */
    override fun setCursorVisible(visible: Boolean) {
        if (mIsCursorVisible != visible) {
            mIsCursorVisible = visible
            invalidateCursor(isCursorVisible)
            makeCursorBlink()
        }
    }

    /**
     * @return if the cursor is visible
     * @see #setCursorVisible(boolean)
     */
    override fun isCursorVisible() = mIsCursorVisible

    /**
     * Sets the line color for all states (normal, selected, focused) to be this color
     * @param color A color value in the form of 0xAARRGGBB - do not use a resource ID
     * @attr ref R.styleable#MFAEditText_lineColor
     * @see #setLineColor(ColorStateList)
     * @see #getLineColors()
     */
    fun setLineColor(@ColorInt color: Int) {
        mLineColor = ColorStateList.valueOf(color)
        updateColors()
    }

    /**
     * Sets the line color for all states
     *
     * @attr ref R.styleable#MFAEditText_lineColor
     * @see #setLineColor(int)
     * @see #getLineColors()
     */
    fun setLineColor(colors: ColorStateList) {
        mLineColor = colors
        updateColors()
    }

    /**
     * @return the line colors for the different states
     * @attr ref R.styleable#MFAEditText_lineColor
     * @see #setLineColor(ColorStateList)
     * @see #setLineColor(int)
     */
    fun getLineColors() = mLineColor

    /**
     * @return the current color selected for a normal line
     */
    @ColorInt fun getCurrentLineColor(): Int = mCurLineColor


    /**
     * @return the width of the item's line
     * @see #setLineWidth(int)
     */
    @Px fun getLineWidth(): Int = mLineWidth

    /**
     * Sets the number of digits in the MFA request
     * @attr ref R.styleable#MFAEditText_itemCount
     * @see #getItemCount()
     */
    fun setItemCount(count: Int) {
        mItemCount = count
        setMaxLength(count)
        requestLayout()
    }

    /**
     * @return the number of digits in the MFA request
     * @see #setItemCount()
     */
    fun getItemCount(): Int = mItemCount

    /**
     * Sets the radius of the square
     * @attr ref R.styleable#MFAEditText_itemRadius
     * @see #getItemRadius()
     */
    fun setItemRadius(@Px itemRadius: Int) {
        mItemRadius = itemRadius
        checkItemRadius()
        requestLayout()
    }

    /**
     * @return the radius of the square
     * @see #setItemRadius(int)
     */
    @Px fun getItemRadius(): Int = mItemRadius

    /**
     * Sets the spacing between the digits
     * @attr ref R.styleable#MFAEditText_itemSpacing
     * @see #getItemSpacing()
     */
    fun setItemSpacing(@Px itemSpacing: Int) {
        mItemSpacing = itemSpacing
        requestLayout()
    }

    /**
     * @return the spacing between the digits
     * @see #setItemSpacing(int)
     */
    @Px fun getItemSpacing() = mItemSpacing

    /**
     * Sets the height of the digits, to the closest pixel
     * @attr ref R.styleable#MFAEditText_itemHeight
     * @see #getItemHeight()
     */
    fun setItemHeight(itemHeight: Float) {
        mItemHeight = itemHeight.toInt()
        updateCursorHeight()
        requestLayout()
    }

    /**
     * @return the height of the digits
     * @see #setItemHeight(float)
     */
    fun getItemHeight(): Float = mItemHeight.toFloat()

    /**
     * sets the width of a single digit
     * @attr ref R.styleable#MFAEditText_itemWidth
     * @see #getItemWidth()
     */
    fun setItemWidth(itemWidth: Float) {
        mItemWidth = itemWidth.toInt()
        checkItemRadius()
        requestLayout()
    }

    /**
     * @return the width of a single digit
     * @see #setItemWidth(float)
     */
    fun getItemWidth(): Float = mItemWidth.toFloat()


    /**
     * Specifies whether the text animation should be enabled
     * @param enable true to start animation when adding text
     */
    fun setAnimationEnabled(enable: Boolean) {
        isAnimationEnabled = enable
    }

    override fun setTextSize(size: Float) {
        super.setTextSize(size)
        updateCursorHeight()
    }

    override fun setTextSize(unit: Int, size: Float) {
        super.setTextSize(unit, size)
        updateCursorHeight()
    }
    /**
     * This method is called whenever the state of the view changes in such a way that it impacts
     * the state of drawables being shown.
     */
    override fun drawableStateChanged() {
        super.drawableStateChanged()
        if (mLineColor == null || mLineColor!!.isStateful) updateColors()
    }

    /**
     * This is called when the view is attached to a window.  At this point, it has a surface
     * and will start drawing.  Note that this function is guaranteed to be called before onDraw
     * @see #onDetachedFromWindow()
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mBlink.uncancel()
        makeCursorBlink()
    }

    /**
     * This is called when the view is detached from the window.  At this point, it no longer has
     * a surface to draw upon.
     * @see #onAttachedToWindow()
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mBlink.cancel()
        invalidateCursor(false)
    }

    /**
     * Draws the control
     */
    override fun onDraw(canvas: Canvas?) {
        canvas?.let {
            it.save()

            with (mPaint) {
                color = mCurLineColor
                style = Paint.Style.STROKE
                strokeWidth = mLineWidth.toFloat()
            }
            mTextPaint.color = currentTextColor

            for (i in 0 until mItemCount) {
                updateItemRectF(i)
                mItemCenterPoint.set(mItemBorderRect.left + Math.abs(mItemBorderRect.width()) / 2, mItemBorderRect.top + Math.abs(mItemBorderRect.height()) / 2)
                drawDigitElement(it, i)

                if (text.length > i) {
                    if (isPasswordInputType()) {
                        drawCircle(it, i)
                    } else {
                        drawTextAtBox(it, getPaintByIndex(i), text, i)
                    }
                } else if (!hint.isEmpty() && hint.length == mItemCount) {
                    drawTextAtBox(it, getPaintByIndex(i, currentHintTextColor), hint, i)
                }
            }

            // Highlight the next item
            if (isFocused && text.length != mItemCount) {
                updateItemRectF(text.length)
                mItemCenterPoint.set(mItemBorderRect.left + Math.abs(mItemBorderRect.width()) / 2, mItemBorderRect.top + Math.abs(mItemBorderRect.height()) / 2)
                mPaint.color = mLineColor?.getColorForState(intArrayOf(android.R.attr.state_selected), mCurLineColor) ?: mCurLineColor
                drawCursor(canvas)
                drawDigitElement(it, text.length)
            }

            it.restore()
        }
    }

    /**
     * Measure the view and its content to determine the measured width and height
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height=
                if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY)
                    MeasureSpec.getSize(heightMeasureSpec)
                else
                    Math.round(mItemHeight.toFloat() + paddingTop + paddingBottom)

        var width: Int
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
            width = MeasureSpec.getSize(widthMeasureSpec)
        } else  {
            width = Math.round((((mItemCount - 1) * mItemSpacing) + (mItemCount * mItemWidth)).toFloat() + paddingEnd + paddingStart)
            if (mItemSpacing == 0) width -= (mItemCount - 1) * mLineWidth
        }

        setMeasuredDimension(width, height)
    }

    /**
     * This method is called whenever the state of the screen this view is attached to changes.
     * A state change will usually occur when the screen turns on or off.  We use it to start and
     * stop the blink capability of the cursor.
     */
    override fun onScreenStateChanged(screenState: Int) {
        super.onScreenStateChanged(screenState)
        when (screenState) {
            View.SCREEN_STATE_ON -> {
                mBlink.uncancel()
                makeCursorBlink()
            }
            View.SCREEN_STATE_OFF -> {
                mBlink.cancel()
                invalidateCursor(false)
            }
        }
    }

    /**
     * This method is called when the text is changed.  Within text, the lengthAfter characters
     * beginning at start have just replaced old text that had length lengthBefore.  It is an
     * error to make changes to text from this callback.
     */
    override fun onTextChanged(newText: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        if (start != newText?.length) setSelection(text.length)
        makeCursorBlink()
        if (isAnimationEnabled) {
            mDefaultAddAnimator.end()
            mDefaultAddAnimator.start()
        }
    }

    /**
     * This method is called by the view system when the focus state of this view changes
     */
    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (focused) {
            setSelection(text.length)
            makeCursorBlink()
        }
    }

    /**
     * This method is called when the selection has changed.
     */
    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (selEnd != text.length) setSelection(text.length)
    }
    // endregion

    // region Private Methods
    /**
     * Sets the input filter according to the length of the text field
     */
    private fun setMaxLength(maxLength: Int) {
        filters = if (maxLength >= 0) arrayOf(InputFilter.LengthFilter(maxLength)) else NO_FILTERS
    }

    /**
     * Determines if the EditText has a password input type
     */
    private fun isPasswordInputType(): Boolean {
        val variation = inputType and (EditorInfo.TYPE_MASK_CLASS or EditorInfo.TYPE_MASK_VARIATION)
        return variation == (EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD)
                || variation == (EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD)
                || variation == (EditorInfo.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD)
    }

    /**
     * Ensures that the itemRadius field is appropriately sized.
     */
    private fun checkItemRadius() {
        if (mViewType == ViewType.LINE && mItemRadius > (mLineWidth / 2)) {
            throw RuntimeException("The itemRadius cannot be greater than lineWidth when viewType is LINE")
        }
        if (mItemRadius > (mItemWidth / 2)) {
            throw RuntimeException("The itemRadius cannot be greater than itemWidth")
        }
    }

    /**
     * Convert display-points to pixel measurements based on pixel density
     */
    private fun dpToPx(dp: Float): Int = (dp * resources.displayMetrics.density + 0.5f).toInt()

    /**
     * Updates the cursor height
     */
    private fun updateCursorHeight() {
        val delta = 2 * dpToPx(2f)
        mCursorHeight = if (mItemHeight - textSize > delta) textSize + delta else textSize
    }

    /**
     * Gets a paint object for the drawing operation
     */
    private fun getPaintByIndex(i: Int, color: Int? = null): Paint {
        var painter: Paint = mTextPaint
        if (isAnimationEnabled && i == text.length - 1) {
            mAnimatorTextPaint.color = mTextPaint.color
            painter = mAnimatorTextPaint
        }
        if (color != null) painter.color = color
        return painter
    }

    /**
     * Draws some text onto the canvas
     */
    private fun drawTextAtBox(canvas: Canvas, paint: Paint, text: CharSequence, charAt: Int) {
        paint.getTextBounds(text.toString(), charAt, charAt + 1, mTextRect)
        val x = mItemCenterPoint.x - Math.abs(mTextRect.width()) / 2 - mTextRect.left
        val y = mItemCenterPoint.y + Math.abs(mTextRect.height()) / 2 - mTextRect.bottom
        canvas.drawText(text, charAt, charAt + 1, x, y, paint)
    }

    /**
     * Draws a circle onto the canvas - used for password-type prompts
     */
    private fun drawCircle(canvas: Canvas, i: Int) {
        val paint = getPaintByIndex(i)
        canvas.drawCircle(mItemCenterPoint.x, mItemCenterPoint.y, paint.textSize / 2, paint)
    }

    /**
     * Draw a digit element (line or box)
     */
    private fun drawDigitElement(canvas: Canvas, i: Int) {
        var drawRightCorner = mViewType == ViewType.LINE
        var drawLeftCorner = mViewType == ViewType.LINE

        when (mViewType) {
            ViewType.RECTANGLE -> {
                if (mItemSpacing != 0) {
                    drawRightCorner = true
                    drawLeftCorner = true
                } else {
                    if (i == 0 && i != mItemCount - 1) drawLeftCorner = true
                    if (i == mItemCount - 1 && i != 0) drawRightCorner = true
                }
                updateRoundRectPath(mItemBorderRect, mItemRadius.toFloat(), mItemRadius.toFloat(), drawLeftCorner, drawRightCorner)
            }

            ViewType.LINE -> {
                if (mItemSpacing == 0 && mItemCount > 1) {
                    when (i) {
                        0 -> drawRightCorner = false
                        (mItemCount - 1) -> drawLeftCorner = false
                        else -> {
                            drawLeftCorner = false
                            drawRightCorner = false
                        }
                    }
                }
                mPaint.style = Paint.Style.FILL
                mPaint.strokeWidth = mLineWidth.toFloat() / 10
                val halfLineWidth = mLineWidth.toFloat() / 2
                mItemLineRect.set(mItemBorderRect.left, mItemBorderRect.bottom - halfLineWidth, mItemBorderRect.right, mItemBorderRect.bottom + halfLineWidth)
                updateRoundRectPath(mItemLineRect, mItemRadius.toFloat(), mItemRadius.toFloat(), drawLeftCorner, drawRightCorner)
            }
        }

        canvas.drawPath(mPath, mPaint)
    }

    /**
     * Set up a path for drawing a bounding box
     */
    private fun updateRoundRectPath(rectF: RectF, rx: Float, ry: Float, tl: Boolean, tr: Boolean, br: Boolean = tr, bl: Boolean = tl) {
        val l = rectF.left
        val t = rectF.top
        val r = rectF.right
        val b = rectF.bottom
        val w = r - l
        val h = b - t
        val lw = w - 2 * rx
        val lh = h - 2 * ry

        mPath.reset()
        mPath.moveTo(l, t + ry)
        if (tl) {
            mPath.rQuadTo(0F, -ry, rx, -ry)
        } else {
            mPath.rLineTo(0F, -ry)
            mPath.rLineTo(rx, 0F)
        }
        mPath.rLineTo(lw, 0F)
        if (tr) {
            mPath.rQuadTo(rx, 0F, rx, ry)
        } else {
            mPath.rLineTo(rx, 0F)
            mPath.rLineTo(0F, ry)
        }
        mPath.rLineTo(0F, lh)
        if (br) {
            mPath.rQuadTo(0F, ry, -rx, ry)
        } else {
            mPath.rLineTo(0F, ry)
            mPath.rLineTo(-rx, 0F)
        }
        mPath.rLineTo(-lw, 0F)
        if (bl) {
            mPath.rQuadTo(-rx, 0F, -rx, -ry)
        } else {
            mPath.rLineTo(-rx, 0F)
            mPath.rLineTo(0F, -ry)
        }
        mPath.rLineTo(0F, -lh)
        mPath.close()
    }

    /**
     * Draws a cursor
     */
    private fun drawCursor(canvas: Canvas) {
        if (!drawCursor) return

        val x = mItemCenterPoint.x
        val y = mItemCenterPoint.y - mCursorHeight / 2

        // Save these for restoration after the operation
        val color = mPaint.color
        val strokeWidth = mPaint.strokeWidth

        mPaint.color = mCursorColor
        mPaint.strokeWidth = mCursorWidth.toFloat()
        canvas.drawLine(x, y, x, y + mCursorHeight, mPaint)

        // Restore mPaint settings after the operation
        mPaint.color = color
        mPaint.strokeWidth = strokeWidth
    }

    /**
     * Update the colors being used for drawables within this view
     */
    private fun updateColors() {
        val color = mLineColor?.getColorForState(drawableState, 0) ?: currentTextColor
        if (color != mCurLineColor) {
            mCurLineColor = color
            invalidate()
        }
    }

    /**
     * Updates the mItemBorderRect position and size
     */
    private fun updateItemRectF(i: Int) {
        val halfLineWidth: Float = mLineWidth.toFloat() / 2
        var left = scrollX + paddingStart + i * (mItemSpacing + mItemWidth) + halfLineWidth
        if (mItemSpacing == 0 && i > 0) left -= mLineWidth * i
        val top = scrollY + paddingTop + halfLineWidth
        val right = left + mItemWidth.toFloat() - mLineWidth.toFloat()
        val bottom = top + mItemHeight.toFloat() - mLineWidth.toFloat()
        mItemBorderRect.set(left, top, right, bottom)
    }

    /**
     * Makes the cursor blink
     */
    private fun makeCursorBlink() {
        removeCallbacks(mBlink)
        if (isCursorVisible && isFocused) {
            drawCursor = false
            postDelayed(mBlink, BLINK)
        }
    }

    /**
     * Invalidates the cursor so that it can be redrawn
     */
    private fun invalidateCursor(showCursor: Boolean = false) {
        if (drawCursor != showCursor) {
            drawCursor = showCursor
            invalidate()
        }
    }


    /**
     * Runnable that deals with the blinking of the cursor
     */
    private inner class Blink : Runnable {
        private var mCancelled : Boolean = false

        override fun run() {
            if (mCancelled)
                return
            removeCallbacks(this)
            if (isCursorVisible && isFocused) {
                invalidateCursor(!drawCursor)
                postDelayed(this, BLINK)
            }
        }

        fun cancel() {
            if (!mCancelled) {
                removeCallbacks(this)
                mCancelled = true
            }
        }

        fun uncancel() { mCancelled = false }
    }
    // endregion
}
